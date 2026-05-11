# Secure Storage Inspector (SSI) 🛡️
**AI-Powered Static Analysis Platform for Android Secret Detection and Security Auditing**

Secure Storage Inspector (SSI) est une plateforme avancée d'analyse statique conçue pour identifier les vulnérabilités de stockage, les fuites de secrets (clés API, tokens, emails) et les mauvaises configurations dans les applications Android (APK) et le code source.

## 🚀 Fonctionnalités Clés
- **Analyse Multiforme :** Analyse de fichiers texte bruts, code source (Smali, XML, Java) et décompilation complète d'APK via `Apktool`.
- **Détection Intelligente :** Utilisation de RegEx et d'heuristiques pour détecter les secrets et les vulnérabilités du Manifest.
- **Mapping OWASP :** Corrélation automatique des failles avec le **OWASP Mobile Top 10**.
- **Remédiation par IA :** Intégration de **Llama 3 (via Ollama)** pour fournir des explications contextuelles et des correctifs de code sécurisés.
- **Scoring de Sécurité :** Algorithme de notation (0-100) avec déduplication et plafonnement par niveau de risque.
- **Reporting :** Exportation des résultats en formats PDF professionnel et JSON.

---

## 🛠️ Installation et Lancement

### 1. Prérequis
- **Java 17** ou supérieur.
- **Node.js** (v16+) et **npm**.
- **Ollama** (pour l'analyse IA) : [Télécharger Ollama](https://ollama.com/).
- **Modèle Llama 3 :** Exécutez `ollama pull llama3` dans votre terminal.

### 2. Lancement du Backend (Spring Boot)
Le serveur backend gère la décompilation, le scan et la communication avec l'IA.
```bash
cd projet_mobile
mvn spring-boot:run
```
*Le serveur sera disponible sur `http://localhost:8080`.*

### 3. Lancement du Frontend (React + Vite)
L'interface utilisateur moderne pour visualiser les rapports et le score.
```bash
cd projet_mobile/frontend
npm install
npm run dev
```
*Accédez à l'application via `http://localhost:5173`.*

---

## 📊 Méthodologie de Calcul du Score
Le score SSI est calculé selon une formule de déduction plafonnée :
- **High Risk :** -20 pts (Plafonné à 60 pts)
- **Medium Risk :** -10 pts (Plafonné à 30 pts)
- **Low Risk :** -5 pts (Plafonné à 10 pts)
*Le système déduplique les vulnérabilités par type pour une évaluation juste de la posture de sécurité.*

---

## 🧪 Technologies Utilisées
- **Backend :** Java, Spring Boot, Maven, Apktool.
- **Frontend :** React.js, Vite, Vanilla CSS (Premium Dark Mode).
- **IA :** Llama 3, Ollama API.
- **Analyse :** Regex, Heuristics, Static Analysis.

---
© 2026 Elsevier Ltd - Développé dans le cadre de la recherche sur la sécurité mobile.