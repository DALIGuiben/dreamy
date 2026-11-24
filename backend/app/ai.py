"""GroqCloud client with per-user daily quotas, ported from the web app's lib/ai.ts."""

import json
from datetime import datetime, time, timezone
from typing import TypeVar

import httpx
from pydantic import BaseModel, ValidationError
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.config import settings
from app.errors import ApiError
from app.models import AiRequest

GROQ = "https://api.groq.com/openai/v1"
OUTAGE = "Le service IA est temporairement indisponible. Vous pouvez continuer votre travail manuellement."
RATE_LIMITED = "Le quota du service IA est atteint. Réessayez plus tard ou continuez manuellement."
DAILY_LIMIT = {"llm": 30, "stt": 10}

T = TypeVar("T", bound=BaseModel)


def _headers() -> dict[str, str]:
    return {"Authorization": f"Bearer {settings.groq_api_key}"}


def _upstream_error(response: httpx.Response) -> ApiError:
    return ApiError(429, RATE_LIMITED) if response.status_code == 429 else ApiError(503, OUTAGE)


def quota(db: Session, user_id: str, kind: str) -> None:
    if not settings.ai_configured:
        raise ApiError(503, OUTAGE)
    since = datetime.combine(datetime.now(timezone.utc).date(), time.min)
    count = db.scalar(
        select(func.count()).select_from(AiRequest).where(
            AiRequest.user_id == user_id, AiRequest.kind == kind, AiRequest.created_at >= since
        )
    )
    if count >= DAILY_LIMIT[kind]:
        raise ApiError(
            429, "Votre limite quotidienne de demandes IA est atteinte. Continuez manuellement ou réessayez demain."
        )
    db.add(AiRequest(user_id=user_id, kind=kind))
    db.commit()


def verify_model(model: str) -> None:
    try:
        response = httpx.get(f"{GROQ}/models", headers=_headers(), timeout=8)
    except httpx.HTTPError:
        raise ApiError(503, OUTAGE)
    if response.status_code != 200:
        raise _upstream_error(response)
    data = response.json().get("data")
    if not isinstance(data, list) or not any(item.get("id") == model for item in data):
        raise ApiError(503, OUTAGE)


def generate(db: Session, user_id: str, system: str, payload: object, schema: type[T]) -> T:
    quota(db, user_id, "llm")
    model = settings.groq_llm_model
    verify_model(model)
    try:
        response = httpx.post(
            f"{GROQ}/chat/completions",
            headers=_headers(),
            json={
                "model": model,
                "temperature": 0.2,
                "response_format": {"type": "json_object"},
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
                ],
            },
            timeout=25,
        )
    except httpx.HTTPError:
        raise ApiError(503, OUTAGE)
    if response.status_code != 200:
        raise _upstream_error(response)
    choices = response.json().get("choices") or [{}]
    content = (choices[0].get("message") or {}).get("content")
    if not isinstance(content, str):
        raise ApiError(502, "La réponse IA est incomplète. Réessayez ou continuez manuellement.")
    try:
        return schema.model_validate(json.loads(content))
    except json.JSONDecodeError:
        raise ApiError(502, "La réponse IA est invalide. Continuez manuellement.")
    except ValidationError:
        raise ApiError(502, "La réponse IA ne peut pas être utilisée. Réessayez ou continuez manuellement.")


def transcribe(db: Session, user_id: str, filename: str, content_type: str, data: bytes, language: str | None) -> str:
    quota(db, user_id, "stt")
    model = settings.groq_stt_model
    verify_model(model)
    form = {"model": model, "response_format": "json"}
    if language:
        form["language"] = language
    try:
        response = httpx.post(
            f"{GROQ}/audio/transcriptions",
            headers=_headers(),
            data=form,
            files={"file": (filename, data, content_type)},
            timeout=30,
        )
    except httpx.HTTPError:
        raise ApiError(503, OUTAGE)
    if response.status_code != 200:
        raise _upstream_error(response)
    text = response.json().get("text")
    if not isinstance(text, str) or not text.strip():
        raise ApiError(502, "Aucune parole reconnue. Réessayez ou saisissez la note.")
    return text.strip()
