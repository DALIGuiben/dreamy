import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

log = logging.getLogger("dreamy")


class ApiError(Exception):
    """Error returned to the client as `{"error": message}`."""

    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message


def install_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def _api_error(_: Request, exc: ApiError):
        return JSONResponse({"error": exc.message}, status_code=exc.status)

    @app.exception_handler(StarletteHTTPException)
    async def _http_error(_: Request, exc: StarletteHTTPException):
        # Malformed bodies, unknown routes, wrong methods: same `{"error"}` shape as everything else.
        message = "Vérifiez les informations saisies." if exc.status_code == 400 else "Ressource introuvable."
        return JSONResponse({"error": message}, status_code=exc.status_code)

    @app.exception_handler(RequestValidationError)
    async def _validation(_: Request, __: RequestValidationError):
        return JSONResponse({"error": "Vérifiez les informations saisies."}, status_code=400)

    @app.exception_handler(Exception)
    async def _unexpected(_: Request, exc: Exception):
        log.error("Request failed: %s", type(exc).__name__)
        return JSONResponse({"error": "Une erreur est survenue. Réessayez."}, status_code=500)
