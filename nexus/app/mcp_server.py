import logging
import json
import uuid
import secrets
import ipaddress
import redis
from mcp.server import Server
from mcp.types import Tool, TextContent
from app.services.health_service import HealthService
from app.services.security_service import SecurityService
from app.services.analyze_service import AnalyzeService
from app.core.config import settings

logger = logging.getLogger("nexus.mcp")

# Initialize Services
health_service = HealthService()
security_service = SecurityService()
analyze_service = AnalyzeService()

# Staff Strategy: Use Redis for resilient state management
# Ensures pending approvals survive Nexus restarts.
redis_client = redis.Redis(
    host=settings.REDIS_HOST,
    port=settings.REDIS_PORT,
    password=settings.REDIS_PASSWORD,
    decode_responses=True
)

APPROVAL_PREFIX = "nexus:approval:"
APPROVAL_TTL = 3600 # 1 hour

# List of tools that require human-in-the-loop approval
DANGEROUS_TOOLS = ["ban_suspicious_ip"]

# Initialize MCP Server
mcp = Server("nexus-operator")

def validate_ip(ip: str) -> bool:
    """Strictly validate IPv4 or IPv6 address."""
    try:
        ipaddress.ip_address(ip)
        return True
    except ValueError:
        return False

@mcp.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="check_system_health",
            description="Checks the health of all Titan Grid services (Aegis, Cryptex, Nexus).",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="get_security_events",
            description="Retrieves recent suspicious activity logs (blocked IPs) from Aegis.",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "default": 10}
                }
            }
        ),
        Tool(
            name="analyze_attack_patterns",
            description="Fetches security events and uses AI to analyze patterns and recommend actions.",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "default": 10}
                }
            }
        ),
        Tool(
            name="ban_suspicious_ip",
            description="Bans a specific IP address in Aegis. REQUIRES HUMAN APPROVAL.",
            inputSchema={
                "type": "object",
                "properties": {
                    "ip": {"type": "string", "description": "The IP address to ban (e.g., 1.2.3.4)"},
                    "hours": {"type": "integer", "default": 1, "description": "Duration of the ban in hours."}
                },
                "required": ["ip"]
            }
        ),
        Tool(
            name="approve_action",
            description="Executes a pending dangerous action using the provided security token.",
            inputSchema={
                "type": "object",
                "properties": {
                    "token": {"type": "string", "description": "The 8-character hex token provided during the ban request."}
                },
                "required": ["token"]
            }
        ),
        Tool(
            name="list_pending_actions",
            description="Lists all security actions currently waiting for human approval.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        )
    ]

@mcp.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    logger.info(f"Tool called: {name} with args: {arguments}")
    
    # 1. Input Validation for Dangerous Tools
    if name == "ban_suspicious_ip":
        ip = arguments.get("ip")
        if not validate_ip(ip):
            return [TextContent(type="text", text=f"Error: '{ip}' is not a valid IP address.")]

    # 2. Handle Safety Interlock for Dangerous Tools
    if name in DANGEROUS_TOOLS:
        token = secrets.token_hex(4) # 8-character hex token
        action_data = {
            "name": name,
            "arguments": arguments,
            "timestamp": str(uuid.uuid4())
        }
        
        # Persist to Redis with TTL
        redis_client.setex(
            f"{APPROVAL_PREFIX}{token}",
            APPROVAL_TTL,
            json.dumps(action_data)
        )
        
        msg = (
            f"⚠️ SAFETY INTERLOCK TRIGGERED\n"
            f"Action: {name}\n"
            f"Arguments: {json.dumps(arguments)}\n"
            f"Status: PENDING APPROVAL\n\n"
            f"To execute this command, call 'approve_action' with:\n"
            f"token: {token}"
        )
        return [TextContent(type="text", text=msg)]

    # 3. Handle Approval Logic
    if name == "approve_action":
        token = arguments.get("token")
        key = f"{APPROVAL_PREFIX}{token}"
        
        raw_data = redis_client.get(key)
        if not raw_data:
            return [TextContent(type="text", text="Error: Invalid, expired, or already used token.")]
        
        action = json.loads(raw_data)
        redis_client.delete(key) # Atomic consumption
        
        return await execute_action(action["name"], action["arguments"])

    if name == "list_pending_actions":
        keys = redis_client.keys(f"{APPROVAL_PREFIX}*")
        if not keys:
            return [TextContent(type="text", text="No actions pending approval.")]
        
        actions = {}
        for k in keys:
            token = k.replace(APPROVAL_PREFIX, "")
            actions[token] = json.loads(redis_client.get(k))
            
        return [TextContent(type="text", text=json.dumps(actions, indent=2))]

    # 4. Handle Safe Tools
    return await execute_action(name, arguments)

async def execute_action(name: str, arguments: dict) -> list[TextContent]:
    """Internal helper for executing verified tool logic."""
    try:
        if name == "check_system_health":
            health_data = await health_service.get_system_health()
            return [TextContent(type="text", text=json.dumps(health_data, indent=2))]
        
        elif name == "get_security_events":
            limit = arguments.get("limit", 10)
            events = await security_service.get_recent_suspicious_activity(limit=limit)
            return [TextContent(type="text", text=json.dumps(events, indent=2))]
        
        elif name == "analyze_attack_patterns":
            limit = arguments.get("limit", 10)
            events = await security_service.get_recent_suspicious_activity(limit=limit)
            if not events:
                return [TextContent(type="text", text="No suspicious activities found to analyze.")]
            analysis = await analyze_service.analyze_security_events(events)
            return [TextContent(type="text", text=analysis)]
        
        elif name == "ban_suspicious_ip":
            ip = arguments.get("ip")
            hours = arguments.get("hours", 1)
            result = await security_service.ban_ip(ip, duration_seconds=hours * 3600)
            return [TextContent(type="text", text=result)]
        
        else:
            raise ValueError(f"Unknown tool: {name}")
    except Exception as e:
        logger.error(f"Error executing {name}: {str(e)}")
        return [TextContent(type="text", text=f"System Error: {str(e)}")]

logger.info("Nexus MCP Server refined with Redis-backed interlock and IP validation.")
