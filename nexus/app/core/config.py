from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional

class Settings(BaseSettings):
    # App Config
    APP_NAME: str = "Nexus AI Operator"
    VERSION: str = "1.0.0"
    LOG_LEVEL: str = "INFO"
    NEXUS_PORT: int = 8082

    # Infrastructure
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: Optional[str] = None
    
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "titan_grid"
    POSTGRES_USER: str = "titan_admin"
    POSTGRES_PASSWORD: str = "change_me"

    # Service Discovery
    AEGIS_URL: str = "http://localhost:8080"
    CRYPTEX_URL: str = "http://localhost:8081"

    # LLM Config
    LLM_PROVIDER: str = "ollama" # ollama, openai, anthropic
    OLLAMA_BASE_URL: str = "http://host.docker.internal:11434"
    OPENAI_API_KEY: Optional[str] = None
    ANTHROPIC_API_KEY: Optional[str] = None

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )

settings = Settings()
