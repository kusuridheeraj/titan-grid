import httpx
import logging
from app.core.config import settings

logger = logging.getLogger("nexus.services.health")

class HealthService:
    async def check_service_health(self, service_url: str) -> dict:
        """Pings a service's health endpoint and returns the status."""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{service_url}/actuator/health")
                if response.status_code == 200:
                    return response.json()
                return {"status": "DOWN", "error": f"HTTP {response.status_code}"}
        except httpx.RequestError as e:
            logger.error(f"Health check failed for {service_url}: {e}")
            return {"status": "DOWN", "error": str(e)}

    async def get_system_health(self) -> dict:
        """Aggregates health status of all core services."""
        aegis_health = await self.check_service_health(settings.AEGIS_URL)
        cryptex_health = await self.check_service_health(settings.CRYPTEX_URL)
        
        # Nexus itself is up if this code is running
        return {
            "aegis": aegis_health,
            "cryptex": cryptex_health,
            "nexus": {"status": "UP"}
        }
