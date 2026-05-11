import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./styles.css";

const rootElement = document.getElementById("root");

try {
  createRoot(rootElement).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
} catch (error) {
  rootElement.innerHTML = `
    <div style="font-family: system-ui, sans-serif; padding: 32px; color: #20231f">
      <h1>Secure Storage Inspector could not start</h1>
      <p>${error instanceof Error ? error.message : "Unknown startup error"}</p>
    </div>
  `;
}
