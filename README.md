# Secure Storage Inspector (SSI) 🛡️
AI-Powered Static Analysis Platform for Android Secret Detection and Security Auditing

Secure Storage Inspector (SSI) is an advanced Android static analysis platform designed to detect insecure storage practices, hardcoded secrets, exposed credentials, and risky configurations within Android applications and APK packages.

The platform combines:
* Static APK analysis
* Heuristic and Regex-based detection
* OWASP Mobile Top 10 mapping
* AI-powered remediation assistance
* Automated security scoring
* Professional reporting

SSI aims to bridge the gap between vulnerability detection and developer remediation by transforming raw findings into actionable security guidance.

## ✨ Key Features

### 🔍 Multi-format Static Analysis
SSI supports the analysis of multiple Android-related file formats:
* APK packages
* AndroidManifest.xml
* Smali files
* XML resources
* JSON configuration files
* Properties files
* Raw text snippets

APK files are automatically decompiled using APKTool before analysis.

### 🧠 Intelligent Secret Detection
The detection engine combines:
* Regular Expressions (Regex)
* Heuristic analysis
* Pattern matching
* Android configuration inspection

SSI can detect:
* API keys
* JWT tokens
* Passwords and credentials
* Emails
* Authentication tokens
* Suspicious high-entropy strings
* Insecure Android configurations

Example detections:
* `android:allowBackup="true"`
* `android:exported="true"`

### 🛡️ OWASP Mobile Top 10 Mapping
All detected vulnerabilities are automatically mapped to relevant:
* OWASP Mobile Top 10 categories
* Mobile security risks
* Android security misconfigurations

| Vulnerability | OWASP Mapping |
| :--- | :--- |
| Hardcoded API Key | M2 - Insecure Data Storage |
| Insecure Logging | M2 - Insecure Data Storage |
| Exported Component | M1 - Improper Platform Usage |

### 🤖 AI-Powered Remediation Guidance
SSI integrates Llama 3 locally using Ollama to provide:
* Vulnerability explanations
* Secure coding recommendations
* Remediation plans
* Security testing procedures
* Secure implementation examples

> [!TIP]
> **Example recommendation:**
> Use `EncryptedSharedPreferences` instead of plaintext `SharedPreferences` for storing authentication tokens.

All AI processing is performed locally to preserve source code privacy.

### 📈 Security Scoring Engine
SSI computes a global security score ranging from 0 to 100. The scoring system applies weighted penalties based on vulnerability severity:

| Severity | Penalty |
| :--- | :--- |
| HIGH | -20 |
| MEDIUM | -10 |
| LOW | -5 |

Additional mechanisms include:
* Vulnerability deduplication
* Severity balancing
* Risk capping
* Global posture estimation

### 📄 Professional Reporting
SSI supports exportation of:
* Professional PDF audit reports
* Structured JSON reports

Reports include:
* Security score
* Vulnerability summary
* OWASP mappings
* AI remediation guidance
* Technical recommendations

## 🏗️ System Architecture
SSI follows a modular client-server architecture composed of:

### Frontend Layer (React + Vite)
Provides:
* Interactive dashboard
* APK upload interface
* Findings visualization
* Risk filtering
* Security score display
* PDF/JSON export

### Backend Layer (Spring Boot)
Responsible for:
* APK decompilation
* File processing
* Static analysis orchestration
* AI communication
* Report generation

### AI & Analysis Engine
Handles:
* Regex scanning
* Heuristic analysis
* OWASP classification
* AI remediation generation

## ⚙️ Installation Guide

### 1. Prerequisites
Install the following dependencies:
* Java 17+
* Maven 3.8+
* Node.js 18+
* npm
* APKTool
* Ollama

### 2. Install Ollama
Download from: [https://ollama.com/](https://ollama.com/)
Then pull the Llama 3 model:
```bash
ollama pull llama3
```

## 🚀 Running the Project

### Backend (Spring Boot)
```bash
cd projet_mobile
mvn spring-boot:run
```
Backend server: `http://localhost:8080`

### Frontend (React + Vite)
```bash
cd projet_mobile/frontend
npm install
npm run dev
```
Frontend interface: `http://localhost:5173`

## 🧪 Analysis Workflow
The SSI analysis pipeline follows these stages:
1. APK upload or source ingestion
2. APK decompilation using APKTool
3. Resource extraction
4. Static analysis execution
5. Secret detection
6. OWASP mapping
7. Security score computation
8. AI remediation generation
9. PDF/JSON report export

## 🔬 Technologies Used

### Backend
* Java
* Spring Boot
* Maven
* APKTool
* OpenPDF

### Frontend
* React.js
* Vite
* CSS

### AI Integration
* Ollama
* Llama 3

### Analysis Technologies
* Static Analysis
* Regex Detection
* Heuristic Detection

## 📚 Research & Educational Value
SSI can be used for:
* Android security education
* Secure development training
* Mobile security research
* APK dataset analysis
* Vulnerability remediation learning

## ⚠️ Current Limitations
* Potential false positives
* Android-only support
* Static-analysis limitations
* Dependency on APKTool compatibility
* Limited semantic understanding

**Future work may include:**
* iOS support
* Dynamic analysis integration
* CI/CD plugins
* Advanced AI semantic analysis

## 👨‍💻 Authors
**Kenza BOUSSOUFI**
**Saad MAKOUCH**

EMSI - Moroccan School of Engineering Sciences
4th Year Engineering Program in Networks and Cybersecurity
Marrakech, Morocco

## 📜 License
Apache License 2.0

## 🙏 Acknowledgements
Special thanks to:
* Mr. Mohamed LACHGAR
* EMSI Marrakech
