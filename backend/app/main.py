from fastapi import FastAPI
from fastapi.responses import FileResponse

from app.config import settings
from app.db import Base, engine
from app.errors import install_error_handlers
from app.routers import ai, auth, children, events

Base.metadata.create_all(engine)

app = FastAPI(title="Dreamy API")
install_error_handlers(app)
for module in (auth, children, events, ai):
    app.include_router(module.router)


@app.get("/presentation/download")
def download_presentation():
    return FileResponse(
        settings.static_dir / "presentation.pptx",
        media_type="application/vnd.openxmlformats-officedocument.presentationml.presentation",
        filename="Dreamy.pptx",
        headers={"Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"},
    )
