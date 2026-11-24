import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


class Settings:
    database_url = os.getenv("DATABASE_URL", f"sqlite:///{BASE_DIR / 'dreamy.db'}")
    auth_secret = os.getenv("AUTH_SECRET", "")
    token_hours = 12
    groq_api_key = os.getenv("GROQ_API_KEY", "")
    groq_llm_model = os.getenv("GROQ_LLM_MODEL") or "openai/gpt-oss-20b"
    groq_stt_model = os.getenv("GROQ_STT_MODEL") or "whisper-large-v3-turbo"
    ai_enabled = os.getenv("AI_ENABLED", "true") != "false"
    static_dir = BASE_DIR / "static"

    @property
    def ai_configured(self) -> bool:
        return (
            self.ai_enabled
            and bool(self.groq_api_key)
            and self.groq_api_key not in ("YOUR_GROQ_API_KEY", "REPLACE_WITH_API_KEY")
        )


settings = Settings()

if len(settings.auth_secret) < 32:
    raise RuntimeError("AUTH_SECRET must be set to at least 32 characters (see .env.example).")
