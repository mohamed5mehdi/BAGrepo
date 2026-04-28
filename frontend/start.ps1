# 🚀 Script d'installation et démarrage du frontend React
# Exécuter depuis le dossier: GestionsAchat\frontend\

Write-Host "⏳ Installation des dépendances..." -ForegroundColor Cyan
npm install

Write-Host "✅ Démarrage du serveur de développement..." -ForegroundColor Green
Write-Host "   Frontend → http://localhost:3000" -ForegroundColor Yellow
Write-Host "   Backend  → http://localhost:8080 (doit être démarré)" -ForegroundColor Yellow
npm run dev
