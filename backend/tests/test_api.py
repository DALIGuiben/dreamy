import os
import tempfile

os.environ["DATABASE_URL"] = f"sqlite:///{tempfile.mkdtemp()}/test.db"
os.environ["AUTH_SECRET"] = "x" * 40
os.environ["AI_ENABLED"] = "false"

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from app.db import SessionLocal  # noqa: E402
from app.main import app  # noqa: E402
from app.seed import seed  # noqa: E402

PASSWORD = "test-password"


@pytest.fixture(scope="module")
def client():
    with SessionLocal() as db:
        seed(db, PASSWORD)
    return TestClient(app)


def login(client, email):
    response = client.post("/api/auth/login", json={"email": email, "password": PASSWORD})
    assert response.status_code == 200, response.text
    return {"Authorization": f"Bearer {response.json()['token']}"}


def test_login_rejects_bad_password(client):
    response = client.post("/api/auth/login", json={"email": "admin@dreamy.com", "password": "nope"})
    assert response.status_code == 401
    assert response.json() == {"error": "Identifiants incorrects ou compte inactif."}


def test_me_requires_token(client):
    assert client.get("/api/me").status_code == 401
    assert client.get("/api/me", headers=login(client, "mother1@dreamy.com")).json()["role"] == "MOTHER"


def test_mother_sees_only_her_children_and_notes(client):
    mother = login(client, "mother1@dreamy.com")
    children = client.get("/api/children", headers=mother).json()
    assert len(children) == 6
    assert all(c["motherId"] == "demo-mother-a" for c in children)
    assert client.get("/api/children/demo-child-6", headers=mother).status_code == 404
    assert "notes" in client.get("/api/children/demo-child-0", headers=mother).json()
    admin = login(client, "admin@dreamy.com")
    assert "notes" not in client.get("/api/children/demo-child-0", headers=admin).json()
    assert [c["firstName"] for c in client.get("/api/children?q=yas", headers=admin).json()] == ["Yasmine"]


def test_child_validation_message(client):
    mother = login(client, "mother1@dreamy.com")
    response = client.post("/api/children", headers=mother, json={"firstName": " ", "birthDate": "2018-01-01"})
    assert response.status_code == 400
    assert response.json()["error"] == "Vérifiez les informations saisies."


def test_note_crud(client):
    mother = login(client, "mother1@dreamy.com")
    created = client.post(
        "/api/children/demo-child-1/notes", headers=mother, json={"content": "Aime le foot", "inputMethod": "TEXT"}
    )
    assert created.status_code == 201
    note_id = created.json()["id"]
    updated = client.put(
        f"/api/children/demo-child-1/notes/{note_id}", headers=mother, json={"content": "Aime le foot", "inputMethod": "VOICE"}
    )
    assert updated.json()["inputMethod"] == "VOICE"
    assert client.delete(f"/api/children/demo-child-1/notes/{note_id}", headers=mother).json() == {"ok": True}


def test_event_lifecycle(client):
    admin = login(client, "admin@dreamy.com")
    mother = login(client, "mother1@dreamy.com")
    body = {
        "type": "SOS_EVENT",
        "title": "Sortie test",
        "eventDate": "2020-01-01T10:00:00+01:00",
        "location": "Parc",
        "childIds": ["demo-child-1"],
        "checklist": ["Eau"],
    }
    event = client.post("/api/events", headers=admin, json=body).json()
    assert event["status"] == "DRAFT" and event["eventDate"] == "2020-01-01T09:00:00.000Z"
    # Drafts stay hidden from mothers.
    assert client.get(f"/api/events/{event['id']}", headers=mother).status_code == 404
    assert client.post(f"/api/events/{event['id']}/status", headers=admin, json={"status": "AWAITING_APPROVAL"}).status_code == 200
    blocked = client.post(f"/api/events/{event['id']}/status", headers=admin, json={"status": "CONFIRMED"})
    assert blocked.json()["error"] == "Tous les accords sont requis avant confirmation."
    participant = client.get(f"/api/events/{event['id']}", headers=mother).json()["participants"][0]
    assert client.patch(
        f"/api/events/{event['id']}/participants/{participant['id']}", headers=mother, json={"status": "APPROVED"}
    ).status_code == 200
    assert client.post(f"/api/events/{event['id']}/status", headers=admin, json={"status": "CONFIRMED"}).status_code == 200
    assert client.post(f"/api/events/{event['id']}/status", headers=admin, json={"status": "COMPLETED"}).status_code == 200
    feedback = client.post(
        f"/api/events/{event['id']}/feedback",
        headers=mother,
        json={"childId": "demo-child-1", "enjoyed": True, "comfortable": False},
    )
    assert feedback.status_code == 200
    locked = client.post(f"/api/events/{event['id']}/tasks", headers=admin, json={"title": "Trop tard"})
    assert locked.json()["error"] == "Cet événement ne peut plus être modifié."


def test_edit_resets_approvals(client):
    admin = login(client, "admin@dreamy.com")
    event = client.get("/api/events/demo-event-1", headers=admin).json()
    body = {
        "type": "SOS_EVENT",
        "title": event["title"],
        "eventDate": event["eventDate"].replace("Z", "+00:00"),
        "location": event["location"],
        "childIds": ["demo-child-0", "demo-child-3"],
        "checklist": ["Une seule tâche"],
    }
    updated = client.put("/api/events/demo-event-1", headers=admin, json=body).json()
    assert updated["status"] == "DRAFT"
    assert sorted(p["childId"] for p in updated["participants"]) == ["demo-child-0", "demo-child-3"]
    assert [t["title"] for t in updated["tasks"]] == ["Une seule tâche"]


def test_roles_are_enforced(client):
    mother = login(client, "mother1@dreamy.com")
    response = client.post("/api/events/demo-event-1/status", headers=mother, json={"status": "CANCELLED"})
    assert response.status_code == 403
    assert response.json()["error"] == "Accès refusé."


def test_ai_disabled_returns_clear_error(client):
    mother = login(client, "mother1@dreamy.com")
    response = client.post("/api/ai/organize-note", headers=mother, json={"childId": "demo-child-0", "content": "x"})
    assert response.status_code == 503
    assert "temporairement indisponible" in response.json()["error"]
    assert client.get("/api/ai/status", headers=mother).json()["available"] is False


def test_presentation_download(client):
    response = client.get("/presentation/download")
    assert response.status_code == 200
    assert response.headers["content-type"].startswith("application/vnd.openxmlformats")


def test_android_recording_duration_is_read(client):
    # Real MediaRecorder output (AAC/MP4, ~2.5 s); mutagen alone reports 0 s for it.
    mother = login(client, "mother1@dreamy.com")
    with open(os.path.join(os.path.dirname(__file__), "fixtures", "android_note.m4a"), "rb") as audio:
        response = client.post(
            "/api/transcription",
            headers=mother,
            data={"childId": "demo-child-0", "duration": "3", "language": "fr"},
            files={"file": ("note.m4a", audio, "audio/mp4")},
        )
    # The audio passes validation and stops at the disabled AI service.
    assert response.status_code == 503, response.text
