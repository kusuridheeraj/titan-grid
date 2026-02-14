import logging
from typing import Optional
from app.core.config import settings
from langchain_ollama import OllamaLLM
from langchain_openai import ChatOpenAI
from langchain_anthropic import ChatAnthropic
from langchain_core.prompts import ChatPromptTemplate

logger = logging.getLogger("nexus.services.analyze")

class AnalyzeService:
    def __init__(self):
        self.provider = settings.LLM_PROVIDER
        self.llm = self._initialize_llm()

    def _initialize_llm(self):
        """Initializes the LLM based on the configured provider."""
        try:
            if self.provider == "ollama":
                logger.info(f"Initializing Local LLM (Ollama) at {settings.OLLAMA_BASE_URL}")
                return OllamaLLM(model="llama3", base_url=settings.OLLAMA_BASE_URL)
            elif self.provider == "openai":
                logger.info("Initializing OpenAI LLM")
                return ChatOpenAI(api_key=settings.OPENAI_API_KEY)
            elif self.provider == "anthropic":
                logger.info("Initializing Anthropic LLM")
                return ChatAnthropic(api_key=settings.ANTHROPIC_API_KEY)
            else:
                logger.warning(f"Unknown LLM provider: {self.provider}. Defaulting to Ollama.")
                return OllamaLLM(model="llama3", base_url=settings.OLLAMA_BASE_URL)
        except Exception as e:
            logger.error(f"Failed to initialize LLM: {e}")
            return None

    async def analyze_security_events(self, events: list[dict]) -> str:
        """Uses LLM to analyze security events and suggest actions."""
        if not self.llm:
            return "LLM not initialized. Cannot perform analysis."
        
        if not events:
            return "No events provided for analysis."

        prompt = ChatPromptTemplate.from_template("""
        You are a Staff Security Engineer. Analyze the following suspicious traffic events from our rate limiter (Aegis):
        
        {events_json}
        
        Tasks:
        1. Identify any clear attack patterns (e.g., brute force, scraping, DDoS).
        2. Identify the most aggressive IP addresses.
        3. Recommend specific actions (e.g., ban IP, investigate endpoint).
        4. Explain the potential business impact if not addressed.
        
        Keep your response concise, technical, and actionable.
        """)
        
        chain = prompt | self.llm
        
        try:
            # For Ollama (LLM) vs ChatModels, handling might differ slightly
            response = await chain.ainvoke({"events_json": str(events)})
            # Ollama returns a string, ChatModels return a Message object
            return getattr(response, 'content', str(response))
        except Exception as e:
            logger.error(f"Analysis failed: {e}")
            return f"Error during analysis: {str(e)}"
