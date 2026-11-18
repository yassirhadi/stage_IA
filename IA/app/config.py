from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Copilot RSSI - AI Service"
    debug: bool = True
    host: str = "0.0.0.0"
    port: int = 8000
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = ""
    mysql_password: str = ""
    mysql_database: str = ""
    backend_base_url: str = "http://localhost:8080/api"

    # LLM Configuration
    llm_provider: str = "ollama"  # options: "openai", "ollama"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.1:8b"

    class Config:
        env_file = Path(__file__).resolve().parents[1] / ".env"
        env_file_encoding = "utf-8"


settings = Settings()
