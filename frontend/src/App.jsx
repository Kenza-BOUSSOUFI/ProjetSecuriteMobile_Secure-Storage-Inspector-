import { useEffect, useMemo, useState } from "react";

const DEFAULT_API_BASE = getDefaultApiBase();
const HISTORY_KEY = "ssi.analysisHistory";

const SAMPLE_TEXT = `const api_key = "sk_live_1234567890abcdef";
val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature"
Log.d("Auth", "password=superSecret123")
prefs.edit().putString("refresh_token", "refresh_1234567890abcdef").apply()
support@example.com`;

const RISK_WEIGHT = { HIGH: 0, MEDIUM: 1, LOW: 2 };

function normalizeApiBase(value = "") {
  return value.trim().replace(/\/+$/, "");
}

function getDefaultApiBase() {
  const configuredBase = normalizeApiBase(import.meta.env.VITE_API_BASE_URL || "");
  if (configuredBase) return configuredBase;
  return window.location.protocol === "file:" ? "http://localhost:8080" : "";
}

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY)) || [];
  } catch {
    return [];
  }
}

function saveHistory(items) {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, 8)));
  } catch {
    // Local storage can be disabled in strict/private browser contexts.
  }
}

function getSavedApiBase() {
  try {
    const saved = localStorage.getItem("ssi.apiBase");
    if (saved === null) return DEFAULT_API_BASE;

    const clean = normalizeApiBase(saved);
    if (clean === "http://localhost:8080" && window.location.port && window.location.port !== "8080") {
      localStorage.removeItem("ssi.apiBase");
      return DEFAULT_API_BASE;
    }

    return clean;
  } catch {
    return DEFAULT_API_BASE;
  }
}

function saveApiBase(value) {
  try {
    if (value) {
      localStorage.setItem("ssi.apiBase", value);
    } else {
      localStorage.removeItem("ssi.apiBase");
    }
  } catch {
    // The typed API base still works for the current session.
  }
}

function getCounts(issues = []) {
  return issues.reduce(
    (counts, issue) => {
      counts[issue.risk] = (counts[issue.risk] || 0) + 1;
      counts.total += 1;
      return counts;
    },
    { HIGH: 0, MEDIUM: 0, LOW: 0, total: 0 }
  );
}

function getScoreTone(score) {
  if (score >= 80) return "good";
  if (score >= 50) return "warn";
  return "bad";
}

function formatDate(value) {
  if (!value) return "Just now";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function maskValue(value = "") {
  if (value.length <= 10) return value;
  return `${value.slice(0, 5)}...${value.slice(-4)}`;
}

function normalizeResult(response, fallbackId, inputLabel) {
  const analysisId = response.analysisId || fallbackId || "raw_text";
  return {
    ...response,
    analysisId,
    analyzedAt: response.analyzedAt || new Date().toISOString(),
    inputLabel,
    issues: response.issues || [],
  };
}

function Metric({ label, value, tone = "neutral" }) {
  return (
    <div className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export default function App() {
  const [apiBase, setApiBase] = useState(getSavedApiBase);
  const [text, setText] = useState("");
  const [file, setFile] = useState(null);
  const [activeResult, setActiveResult] = useState(null);
  const [history, setHistory] = useState(loadHistory);
  const [riskFilter, setRiskFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [showSecrets, setShowSecrets] = useState(false);
  const [aiTab, setAiTab] = useState("explanation");
  const [isScanning, setIsScanning] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState("");
  const [backendStatus, setBackendStatus] = useState("checking");
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 5;

  const counts = useMemo(() => getCounts(activeResult?.issues), [activeResult]);
  const apiTargetLabel = apiBase || "same-origin /api";

  function apiUrl(path) {
    return apiBase ? `${apiBase}${path}` : path;
  }

  useEffect(() => {
    const controller = new AbortController();

    async function checkBackend() {
      setBackendStatus("checking");
      try {
        const response = await fetch(apiUrl("/api/health"), {
          signal: controller.signal,
        });
        const data = response.ok ? await response.json() : null;
        setBackendStatus(data?.status === "ok" ? "online" : "offline");
      } catch {
        if (!controller.signal.aborted) {
          setBackendStatus("offline");
        }
      }
    }

    checkBackend();
    return () => controller.abort();
  }, [apiBase]);

  const filteredIssues = useMemo(() => {
    const search = query.trim().toLowerCase();
    return (activeResult?.issues || [])
      .filter((issue) => riskFilter === "ALL" || issue.risk === riskFilter)
      .filter((issue) => {
        if (!search) return true;
        return [issue.type, issue.source, issue.owaspId, issue.owaspTitle, issue.key, issue.value]
          .filter(Boolean)
          .some((part) => String(part).toLowerCase().includes(search));
      })
      .sort((a, b) => (RISK_WEIGHT[a.risk] ?? 9) - (RISK_WEIGHT[b.risk] ?? 9));
  }, [activeResult, query, riskFilter]);

  const paginatedIssues = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredIssues.slice(start, start + PAGE_SIZE);
  }, [filteredIssues, currentPage]);

  const totalPages = Math.ceil(filteredIssues.length / PAGE_SIZE);

  useEffect(() => {
    setCurrentPage(1);
  }, [riskFilter, query, activeResult]);

  function rememberResult(result) {
    setActiveResult(result);
    const nextHistory = [
      result,
      ...history.filter((item) => item.analysisId !== result.analysisId || item.analyzedAt !== result.analyzedAt),
    ].slice(0, 8);
    setHistory(nextHistory);
    saveHistory(nextHistory);
  }

  function updateApiBase(value) {
    const clean = normalizeApiBase(value);
    setApiBase(clean);
    saveApiBase(clean);
  }

  async function submitAnalysis(formData, fallbackId, inputLabel) {
    setError("");
    setIsScanning(true);

    try {
      const response = await fetch(apiUrl("/api/analyze"), {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Analysis failed with HTTP ${response.status}`);
      }

      const data = await response.json();
      rememberResult(normalizeResult(data, fallbackId, inputLabel));
      setRiskFilter("ALL");
      setQuery("");
    } catch (err) {
      setBackendStatus("offline");
      setError(
        err instanceof TypeError
          ? `Cannot reach the backend through ${apiTargetLabel}. Start the Spring Boot server on port 8080, then try again.`
          : err.message || "Analysis failed"
      );
    } finally {
      setIsScanning(false);
    }
  }

  function analyzeText(event) {
    event.preventDefault();
    const cleanText = text.trim();
    if (!cleanText) {
      setError("Paste code, JSON, XML, properties, or logs before scanning text.");
      return;
    }

    const formData = new FormData();
    formData.append("text", cleanText);
    submitAnalysis(formData, "raw_text", "Raw text");
  }

  function analyzeFile(event) {
    event.preventDefault();
    if (!file) {
      setError("Choose a TXT, XML, JSON, properties, smali, or APK file first.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    submitAnalysis(formData, file.name, file.name);
  }

  async function downloadReport(result = activeResult) {
    if (!result?.analysisId) {
      setError("Run an analysis before downloading a PDF report.");
      return;
    }

    setError("");
    setIsDownloading(true);

    try {
      const response = await fetch(apiUrl(`/api/report/${encodeURIComponent(result.analysisId)}`));

      if (!response.ok) {
        throw new Error(`Report download failed with HTTP ${response.status}`);
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `SecurityReport_${result.analysisId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setBackendStatus("offline");
      setError(
        err instanceof TypeError
          ? `Cannot reach the backend through ${apiTargetLabel}. Start the Spring Boot server before downloading a report.`
          : err.message || "Report download failed"
      );
    } finally {
      setIsDownloading(false);
    }
  }

  function exportJson() {
    if (!activeResult) return;
    const blob = new Blob([JSON.stringify(activeResult, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${activeResult.analysisId || "analysis"}-security-analysis.json`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  const ai = activeResult?.aiAnalysis || {};
  const aiContent = {
    explanation: ai.explanation,
    remediation: ai.remediation,
    code: ai.codeExample,
    tests: ai.tests,
  };

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Static mobile security analysis</p>
          <h1>Secure Storage Inspector</h1>
        </div>
        <label className="api-field">
          <span>API base</span>
          <input
            value={apiBase}
            onChange={(event) => updateApiBase(event.target.value)}
            placeholder="Same origin, or http://localhost:8080"
          />
        </label>
        <div className={`backend-status ${backendStatus}`}>
          <span>{backendStatus === "online" ? "Backend online" : backendStatus === "offline" ? "Backend offline" : "Checking backend"}</span>
        </div>
      </header>

      <section className="workspace">
        <div className="scan-column">
          <form className="panel input-panel" onSubmit={analyzeText}>
            <div className="panel-heading">
              <div>
                <h2>Text scan</h2>
                <p>Use this for snippets, configs, logs, JSON, XML, or pasted source.</p>
              </div>
              <button type="button" className="secondary" onClick={() => setText(SAMPLE_TEXT)}>
                Load sample
              </button>
            </div>
            <textarea
              value={text}
              onChange={(event) => setText(event.target.value)}
              placeholder="Paste Android code, config files, tokens, logs, or XML here..."
            />
            <div className="actions">
              <button type="submit" disabled={isScanning}>
                {isScanning ? "Scanning..." : "Analyze text"}
              </button>
            </div>
          </form>

          <form className="panel file-panel" onSubmit={analyzeFile}>
            <div className="panel-heading">
              <div>
                <h2>File or APK scan</h2>
                <p>Upload source-like files for direct scanning, or an APK for decompile analysis.</p>
              </div>
            </div>
            <label className="drop-zone">
              <input
                type="file"
                accept=".txt,.xml,.json,.properties,.smali,.apk"
                onChange={(event) => setFile(event.target.files?.[0] || null)}
              />
              <span>{file ? file.name : "Choose TXT, XML, JSON, properties, smali, or APK"}</span>
            </label>
            <div className="actions">
              <button type="submit" disabled={isScanning}>
                {isScanning ? "Scanning..." : "Analyze file"}
              </button>
            </div>
          </form>

          <section className="panel history-panel">
            <div className="panel-heading compact">
              <h2>Recent scans</h2>
              {history.length > 0 && (
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    setHistory([]);
                    saveHistory([]);
                  }}
                >
                  Clear
                </button>
              )}
            </div>
            <div className="history-list">
              {history.length === 0 ? (
                <p className="muted" style={{ textAlign: "center", padding: "10px" }}>
                  No recent scans yet. Your analysis history will appear here.
                </p>
              ) : (
                history.map((item) => (
                  <button
                    type="button"
                    className={`history-item ${activeResult?.analyzedAt === item.analyzedAt ? "active" : ""}`}
                    key={`${item.analysisId}-${item.analyzedAt}`}
                    onClick={() => setActiveResult(item)}
                  >
                    <span>{item.inputLabel || item.analysisId}</span>
                    <strong>{item.score}/100</strong>
                  </button>
                ))
              )}
            </div>
          </section>
        </div>

        <section className="results-column">
          {error && <div className="alert">{error}</div>}

          {!activeResult ? (
            <div className="empty-state">
              <p className="eyebrow">Ready</p>
              <h2>Run a scan to see score, findings, OWASP mapping, AI guidance, and PDF export.</h2>
            </div>
          ) : (
            <>
              <section className="summary-band">
                <div className={`score-dial ${getScoreTone(activeResult.score)}`}>
                  <span>{activeResult.score}</span>
                  <small>/100</small>
                </div>
                <div className="summary-copy">
                  <p className="eyebrow">{formatDate(activeResult.analyzedAt)}</p>
                  <h2>{activeResult.inputLabel || activeResult.analysisId}</h2>
                  <p>
                    {counts.total} finding{counts.total === 1 ? "" : "s"} detected across regex, heuristic, manifest,
                    and APK scanning paths.
                  </p>
                </div>
                <div className="summary-actions">
                  <button type="button" onClick={() => downloadReport()} disabled={isDownloading}>
                    {isDownloading ? "Preparing..." : "PDF report"}
                  </button>
                  <button type="button" className="secondary" onClick={exportJson}>
                    JSON
                  </button>
                </div>
              </section>

              <section className="metric-grid">
                <Metric label="High" value={counts.HIGH} tone="high" />
                <Metric label="Medium" value={counts.MEDIUM} tone="medium" />
                <Metric label="Low" value={counts.LOW} tone="low" />
                <Metric label="OWASP mapped" value={(activeResult.issues || []).filter((issue) => issue.owaspId).length} />
              </section>

              <section className="panel findings-panel">
                <div className="panel-heading">
                  <div>
                    <h2>Findings</h2>
                    <p>Table view with 20 results per page. Use filters to narrow down the list.</p>
                  </div>
                  <label className="toggle">
                    <input
                      type="checkbox"
                      checked={showSecrets}
                      onChange={(event) => setShowSecrets(event.target.checked)}
                    />
                    <span>Show values</span>
                  </label>
                </div>

                <div className="filters">
                  <div className="segmented">
                    {["ALL", "HIGH", "MEDIUM", "LOW"].map((risk) => (
                      <button
                        type="button"
                        key={risk}
                        className={riskFilter === risk ? "selected" : ""}
                        onClick={() => setRiskFilter(risk)}
                      >
                        {risk}
                      </button>
                    ))}
                  </div>
                  <input
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Search findings..."
                  />
                </div>

                <div className="issues-list compact-table">
                  {filteredIssues.length === 0 ? (
                    <p className="muted">No findings match the current filters.</p>
                  ) : (
                    <>
                      <table>
                        <thead>
                          <tr>
                            <th>Risk</th>
                            <th>Type</th>
                            <th>Value</th>
                            <th>Source</th>
                            <th>OWASP</th>
                          </tr>
                        </thead>
                        <tbody>
                          {paginatedIssues.map((issue, index) => (
                            <tr key={`${issue.type}-${index}`}>
                              <td><span className={`risk-tag ${issue.risk?.toLowerCase()}`}>{issue.risk}</span></td>
                              <td><strong>{issue.type}</strong></td>
                              <td className="mono">{showSecrets ? issue.value : maskValue(issue.value)}</td>
                              <td className="mono-small">{issue.source}</td>
                              <td>{issue.owaspId}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>

                      {totalPages > 1 && (
                        <div className="pagination">
                          <button 
                            disabled={currentPage === 1} 
                            onClick={() => setCurrentPage(p => p - 1)}
                            className="ghost"
                          >
                            Previous
                          </button>
                          <span className="page-info">
                            Page <strong>{currentPage}</strong> of {totalPages}
                          </span>
                          <button 
                            disabled={currentPage === totalPages} 
                            onClick={() => setCurrentPage(p => p + 1)}
                            className="ghost"
                          >
                            Next
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </section>

              <section className="panel ai-panel">
                <div className="panel-heading">
                  <div>
                    <h2>AI guidance</h2>
                    <p>Generated by the backend Ollama/LLaMA service when it is available.</p>
                  </div>
                </div>
                <div className="tabs">
                  {[
                    ["explanation", "Explanation"],
                    ["remediation", "Remediation"],
                    ["code", "Code"],
                    ["tests", "Tests"],
                  ].map(([id, label]) => (
                    <button type="button" key={id} className={aiTab === id ? "selected" : ""} onClick={() => setAiTab(id)}>
                      {label}
                    </button>
                  ))}
                </div>
                <div className="ai-content">
                  <pre>{aiContent[aiTab] || "No AI content returned for this section."}</pre>
                </div>
              </section>
            </>
          )}
        </section>
      </section>
    </main>
  );
}
