from datetime import datetime, timedelta, timezone

import bcrypt
import jwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.config import settings
from app.db import get_db
from app.errors import ApiError
from app.models import Role, User

_bearer = HTTPBearer(auto_error=False)


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt(12)).decode()


def verify_password(password: str, password_hash: str) -> bool:
    return bcrypt.checkpw(password.encode(), password_hash.encode())


def create_token(user: User) -> str:
    expires = datetime.now(timezone.utc) + timedelta(hours=settings.token_hours)
    return jwt.encode({"sub": user.id, "exp": expires}, settings.auth_secret, algorithm="HS256")


def current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise ApiError(401, "Connexion requise.")
    try:
        payload = jwt.decode(credentials.credentials, settings.auth_secret, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise ApiError(401, "Connexion requise.")
    user = db.get(User, payload.get("sub"))
    if user is None or not user.active:
        raise ApiError(401, "Connexion requise.")
    return user


def require_mother(user: User = Depends(current_user)) -> User:
    if user.role != Role.MOTHER:
        raise ApiError(403, "Accès refusé.")
    return user


def require_admin(user: User = Depends(current_user)) -> User:
    if user.role != Role.ADMIN:
        raise ApiError(403, "Accès refusé.")
    return user
