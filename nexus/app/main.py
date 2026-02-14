from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator
import logging
import json
import httpx
from app.core.config import settings
from app.services.health_service import HealthService
from app.services.security_service import SecurityService
from app.services.analyze_service import AnalyzeService

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("nexus")

# Initialize Services
health_service = HealthService()
security_service = SecurityService()
analyze_service = AnalyzeService()

app = FastAPI(title=settings.APP_NAME, version=settings.VERSION)

# Initialize Prometheus Instrumentator
Instrumentator().instrument(app).expose(app)

# Define tools as standard API endpoints for now to unblock
# We can add real MCP transport later if the library is giving trouble

@app.get("/health")
async def health_check():
    return {"status": "UP", "component": "nexus"}

@app.get("/")
async def root():
    return {"message": "Nexus AI Operator is running. Tools available via API."}

@app.get("/tools/health")
async def tool_check_health():
    """Tool: Checks the health of all Titan Grid services."""
    return await health_service.get_system_health()

@app.get("/tools/security")
async def tool_get_security_events(limit: int = 10):
    """Tool: Retrieves recent suspicious activity logs."""
    return await security_service.get_recent_suspicious_activity(limit=limit)

@app.post("/tools/analyze")
async def tool_analyze_events(limit: int = 10):
    """Tool: AI analysis of attack patterns."""
    events = await security_service.get_recent_suspicious_activity(limit=limit)
    if not events:
        return {"analysis": "No events found."}
    analysis = await analyze_service.analyze_security_events(events)
    return {"analysis": analysis}

@app.post("/tools/ban")
async def tool_ban_ip(ip: str, hours: int = 1):
    """Tool: Bans an IP address."""
    return {"result": await security_service.ban_ip(ip, hours * 3600)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)
