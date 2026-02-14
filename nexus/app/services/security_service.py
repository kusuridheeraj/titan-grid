import redis.asyncio as redis
import logging
import json
from app.core.config import settings

logger = logging.getLogger("nexus.services.security")

class SecurityService:
    def __init__(self):
        self.redis = redis.Redis(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            password=settings.REDIS_PASSWORD,
            decode_responses=True
        )

    async def get_recent_suspicious_activity(self, limit: int = 10) -> list[dict]:
        """
        Fetches the last N events from the 'suspicious_traffic' Redis Stream.
        Real-time security monitoring.
        """
        try:
            # XREVRANGE key + + COUNT limit
            events = await self.redis.xrevrange("suspicious_traffic", count=limit)
            parsed_events = []
            
            for event_id, data in events:
                # Redis returns data as dict, we just clean it up
                parsed_events.append({
                    "id": event_id,
                    "client_id": data.get("clientId"),
                    "ip": data.get("ip"),
                    "reason": data.get("reason"),
                    "timestamp": data.get("timestamp")
                })
            
            return parsed_events
        except Exception as e:
            logger.error(f"Failed to fetch suspicious activity: {e}")
            return []

    async def ban_ip(self, ip_address: str, duration_seconds: int = 3600) -> str:
        """
        Adds an IP to the blocklist in Redis.
        Logic: Sets a key 'blacklist:{ip}' with a TTL.
        """
        key = f"blacklist:{ip_address}"
        try:
            await self.redis.set(key, "banned", ex=duration_seconds)
            return f"IP {ip_address} has been banned for {duration_seconds} seconds."
        except Exception as e:
            logger.error(f"Failed to ban IP {ip_address}: {e}")
            return f"Error banning IP: {str(e)}"
