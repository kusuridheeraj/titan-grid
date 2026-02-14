# Titan Grid - Frontend Implementation Walkthrough

I have successfully implemented the **Titan Grid Console**, a modern, responsive React application that serves as the unified interface for Aegis, Cryptex, and Nexus.

## 1. Features Implemented

### 🛡️ Aegis Dashboard
- **Real-time Traffic Monitor**: Visualization of allowed vs. blocked requests.
- **Rate Limit Rules**: Configuration table for managing API limits dynamically.

### 🔒 Cryptex Vault
- **Secure Upload**: Drag-and-drop zone with simulated AES-256-GCM encryption.
- **File Management**: List of encrypted files with download/delete actions.

### 🧠 Nexus Command Center
- **AI Chat Interface**: Direct communication channel with the Nexus AI operator.
- **Human-in-the-Loop**: Approval cards for critical system actions (e.g., banning IPs).

### 🖥️ Application Shell
- **Modern UI**: Dark mode aesthetic using Tailwind CSS and Shadcn/UI.
- **Responsive Layout**: Sidebar navigation and mobile-friendly design.
- **Dockerized**: Fully containerized with Nginx for production-ready deployment.

## 2. Technical Stack

| Component | Technology |
|-----------|------------|
| **Framework** | React 18 + Vite |
| **Styling** | Tailwind CSS + Shadcn/UI |
| **Charts** | Recharts |
| **Icons** | Lucide React |
| **Server** | Nginx (Alpine) |
| **Container** | Docker |

## 3. How to Run

### Option 1: Development Mode
```bash
cd titan-console
npm install
npm run dev
```

### Option 2: Docker (Full System)
I have updated the main `docker-compose.yml` to include the console.
```bash
cd infra
docker-compose up --build -d
```
The console will be available at `http://localhost:80`.

## 4. Architecture Updates
- **Reverse Proxy**: Nginx is configured to proxy API requests:
    - `/api/aegis` -> `titan-aegis:8080`
    - `/api/cryptex` -> `titan-cryptex:8081`
    - `/api/nexus` -> `titan-nexus:8082`

## 5. Verification
- [x] **UI Components**: All requested dashboards are implemented.
- [x] **Navigation**: Sidebar correctly routes between modules.
- [x] **Docker Config**: `Dockerfile` and `nginx.conf` are created and integrated into `docker-compose.yml`.
