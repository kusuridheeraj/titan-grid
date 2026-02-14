# Frontend Implementation Plan - Titan Grid Console

## Goal
Build a unified, industry-standard "Mission Control" dashboard that visually demonstrates the power of Titan Grid's three core services (Aegis, Cryptex, Nexus). The UI must be responsive, modern ("Dark Mode" aesthetic), and intuitive.

## Tech Stack
*   **Framework**: [React](https://react.dev/) v18+ with [Vite](https://vitejs.dev/) (Fast, lightweight, standard).
*   **Language**: TypeScript (Type safety is industry standard).
*   **Styling**: [Tailwind CSS](https://tailwindcss.com/) (Rapid, utility-first styling).
*   **Components**: [shadcn/ui](https://ui.shadcn.com/) (Premium, accessible, copy-paste components based on Radix UI).
*   **Charts**: [Recharts](https://recharts.org/) (For Aegis traffic visualization).
*   **State Management**: [Zustand](https://github.com/pmndrs/zustand) (Simpler and lighter than Redux).
*   **Icons**: [Lucide React](https://lucide.dev/).

## User Experience (UX) Philosophy
*   **"Show, Don't Just Tell"**: The UI should visualize the *invisible* backend processes.
    *   *Aegis*: Show packets traveling and getting blocked in real-time.
    *   *Cryptex*: Show the "locking" animation when a file is encrypted.
    *   *Nexus*: A chat interface that feels like talking to a digital operator.

## Architecture & Modules

### 1. The Shell (Layout)
*   **Sidebar Navigation**: Context switching between services.
*   **Global Status Bar**: Health indicators for Redis, Postgres, Vault, and Ollama.

### 2. Aegis Module (Rate Limit Guard)
*   **Live Traffic Monitor**: Updates every second showing Requests/sec.
*   **Color Coded**: Green (Allowed) vs Red (Blocked).
*   **Rule Configurator**: A table to view and edit Rate Limit Rules (connecting to the API we analyzed).

### 3. Cryptex Module (Secure Vault)
*   **Drop Zone**: Drag & Drop file upload area.
*   **Security Context**: Visual indicator showing "Encrypted with AES-256-GCM" and "Key managed by Vault".
*   **File List**: Browser for stored encrypted files with "Decrypt & Download" action.

### 4. Nexus Module (AI Command)
*   **Terminal Interface**: A chat-like UI `>` where user types instructions.
*   **Approval Cards**: When Nexus requests approval (e.g., "Ban IP 1.2.3.4?"), a stylized card appears with "Approve" / "Deny" buttons.

## Implementation Steps

### Phase 1: Setup & Foundation
1.  Initialize `titan-console` project using Vite.
2.  Install Tailwind & Shadcn/UI (Glassmorphism theme).
3.  Set up the Dashboard Layout.
4.  **Implement Simple Login Screen** (Concept for future SSO).

### Phase 2: Aegis UI & Data Layer
1.  Create `TrafficChart` component (Recharts, real-time).
2.  **Implement Data Adapter Pattern**: Switch between `MockAdapter` and `RealApiAdapter` via `.env` flag.
3.  Connect to `/actuator/prometheus` for real metrics.
4.  Implement Rule Management Table.

### Phase 3: Nexus UI
1.  Build the Chat Interface.
2.  Connect to Nexus MCP / FastAPI endpoints.
3.  Implement the "Human-in-the-Loop" approval card.

### Phase 4: Cryptex UI
1.  Build File Upload component.
2.  Handle Stream responses for download.

## Folder Structure
```
titan-console/
├── src/
│   ├── components/
│   │   ├── ui/ (shadcn)
│   │   ├── layout/
│   │   ├── aegis/
│   │   ├── cryptex/
│   │   └── nexus/
│   ├── lib/ (utils, api clients)
│   ├── hooks/
│   └── stores/ (zustand state)
```

## Dockerization Strategy
To ensure the UI is deployment-ready as requested:
1.  **Dockerfile**: Multi-stage build (Node.js build -> Nginx alpine image).
2.  **Docker Compose**: Add `titan-console` service to the main `infra/docker-compose.yml`.
3.  **Networking**: Configure Nginx reverse proxy to route `/api` requests to backend services (Aegis/Cryptex/Nexus) to avoid CORS issues.
