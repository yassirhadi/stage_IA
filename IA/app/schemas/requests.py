from typing import Optional

from pydantic import BaseModel, Field


class AnalyzeRequest(BaseModel):
    persist: bool = True
    file_path: str = Field(..., description="Chemin absolu du fichier à analyser")


class SensitiveDataItem(BaseModel):
    data_type: str
    detected_value: str
    masked_value: str
    confidence: float


class RiskItem(BaseModel):
    title: str
    description: str
    severity: str
    category: str


class AnalyzeResponse(BaseModel):
    extracted_text: str
    document_type: str
    confidentiality_level: str
    sensitive_data: list[SensitiveDataItem]
    risks: list[RiskItem]
    recommendations: list[dict] = []
    security_score: int = 100
    compliance_standards: list[str] = []


class ChatRequest(BaseModel):
    question: Optional[str] = ""
    query: Optional[str] = ""
    message: Optional[str] = None

    def get_text(self) -> str:
        return self.question or self.query or self.message or ""


class ChatResponse(BaseModel):
    answer: str
    sources: list[str] = []
