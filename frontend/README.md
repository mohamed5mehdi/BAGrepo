# README Frontend — GestionsAchat React App

## Stack technique
- **React 18** + **TypeScript**
- **Vite 5** (bundler ultra-rapide)
- **Tailwind CSS 3** (utility-first styling)
- **React Router 6** (navigation SPA)
- **TanStack Query v5** (data fetching + cache)
- **Axios** (appels API REST)
- **React Hot Toast** (notifications)

## Structure du projet

```
frontend/
├── src/
│   ├── api/
│   │   ├── axios.ts         # Instance Axios (proxy /api → localhost:8080)
│   │   └── services.ts      # Tous les appels REST (DA, workflow, famille, etc.)
│   ├── components/
│   │   ├── DaModal.tsx      # Modal réutilisable avec détails DA + stepper
│   │   ├── DaTable.tsx      # Tableau DA avec filtre et action
│   │   ├── DashboardLayout.tsx  # Layout sidebar + topbar
│   │   ├── KpiCard.tsx      # Carte KPI avec gradient
│   │   ├── Sidebar.tsx      # Sidebar avec navigation par rôle
│   │   ├── StatusBadge.tsx  # Badge coloré par statut DA
│   │   ├── Topbar.tsx       # En-tête avec notifications
│   │   └── WorkflowStepper.tsx  # Stepper visuel du workflow
│   ├── context/
│   │   └── AuthContext.tsx  # Auth (user, login, logout) avec localStorage
│   ├── pages/
│   │   ├── LoginPage.tsx    # Page de connexion glassmorphism
│   │   ├── DemandeurPage.tsx  # Dashboard demandeur (view + create DA)
│   │   ├── ValidatorPage.tsx  # Page générique validateur (N1, Tech, AMG)
│   │   ├── AcheteurPage.tsx   # Dashboard acheteur complet
│   │   ├── DafPage.tsx        # Dashboard DAF (transfert sous-famille)
│   │   ├── DgPage.tsx         # Dashboard DG (arbitrage final + injection)
│   │   └── RolePages.tsx      # Wrappers N1Page, TechPage, AmgPage + ProtectedRoute
│   ├── types/
│   │   └── index.ts         # Tous les types TypeScript (entités + API)
│   ├── utils/
│   │   └── constants.ts     # Labels, couleurs, formatters, workflow steps
│   ├── App.tsx              # Routes + providers
│   ├── index.css            # Tailwind + animations custom
│   └── main.tsx             # Entry point React
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts           # Proxy /api → http://localhost:8080
```

## Prérequis

1. **Node.js 18+** — Télécharger sur https://nodejs.org/
2. **Spring Boot** doit tourner sur le port 8080

## Démarrage

```powershell
# Dans le dossier frontend/
cd C:\Users\user\IdeaProjects\GestionsAchat\frontend

npm install      # Installer les dépendances
npm run dev      # Démarrer le serveur de dev (port 3000)
```

Ouvrir → http://localhost:3000

## Comptes de test

| Rôle       | Email                    | Mot de passe |
|------------|--------------------------|--------------|
| Demandeur  | demandeur@test.com       | password     |
| N+1        | n1@test.com              | password     |
| Technicien | tech@test.com            | password     |
| Acheteur   | acheteur@test.com        | password     |
| AMG        | amg@test.com             | password     |
| DAF        | daf@test.com             | password     |
| DG         | dg@test.com              | password     |

## CORS Spring Boot

Le proxy Vite transmet toutes les requêtes `/api/*` vers `http://localhost:8080`.
En production, configurer `@CrossOrigin(origins = "http://votre-domaine.com")`.
