from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db import get_db
from app.errors import ApiError
from app.models import Child, ChildNote, NoteInputMethod, Role, User
from app.schemas import ChildIn, NoteIn
from app.security import current_user, require_mother
from app.serializers import child_out, note_out

router = APIRouter(prefix="/api/children")


def own_child(db: Session, child_id: str, mother: User) -> Child:
    child = db.scalar(select(Child).where(Child.id == child_id, Child.mother_id == mother.id))
    if child is None:
        raise ApiError(404, "Enfant introuvable.")
    return child


def _apply(child: Child, body: ChildIn) -> None:
    child.first_name = body.firstName
    child.birth_date = body.birthDate
    child.avatar = body.avatar
    child.description = body.description
    child.personality_tags = body.personalityTags
    child.interests = body.interests
    child.favorite_activities = body.favoriteActivities
    child.favorite_colors = body.favoriteColors
    child.birthday_themes = body.birthdayThemes
    child.gift_wishes = body.giftWishes
    child.comfort_preferences = body.comfortPreferences
    child.approved_planning_summary = body.approvedPlanningSummary


@router.get("")
def list_children(q: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    query = select(Child).order_by(Child.first_name)
    if user.role == Role.MOTHER:
        query = query.where(Child.mother_id == user.id)
    if q and q.strip():
        query = query.where(Child.first_name.ilike(f"%{q.strip()}%"))
    return [child_out(child) for child in db.scalars(query)]


@router.post("", status_code=201)
def create_child(body: ChildIn, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    child = Child(mother_id=user.id)
    _apply(child, body)
    db.add(child)
    db.commit()
    db.refresh(child)
    return child_out(child)


@router.get("/{child_id}")
def get_child(child_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)):
    child = db.get(Child, child_id)
    is_mother = user.role == Role.MOTHER
    if child is None or (is_mother and child.mother_id != user.id):
        raise ApiError(404, "Enfant introuvable.")
    # Private notes are only ever returned to the child's own mother.
    return child_out(child, with_notes=is_mother)


@router.put("/{child_id}")
def update_child(child_id: str, body: ChildIn, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    child = own_child(db, child_id, user)
    _apply(child, body)
    db.commit()
    return child_out(child)


@router.delete("/{child_id}")
def delete_child(child_id: str, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    db.delete(own_child(db, child_id, user))
    db.commit()
    return {"ok": True}


def _own_note(db: Session, child_id: str, note_id: str, mother: User) -> ChildNote:
    note = db.scalar(
        select(ChildNote).where(
            ChildNote.id == note_id, ChildNote.child_id == child_id, ChildNote.mother_id == mother.id
        )
    )
    if note is None:
        raise ApiError(404, "Note introuvable.")
    return note


@router.post("/{child_id}/notes", status_code=201)
def create_note(child_id: str, body: NoteIn, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    own_child(db, child_id, user)
    note = ChildNote(
        child_id=child_id, mother_id=user.id, content=body.content, input_method=NoteInputMethod(body.inputMethod)
    )
    db.add(note)
    db.commit()
    return note_out(note)


@router.put("/{child_id}/notes/{note_id}")
def update_note(
    child_id: str, note_id: str, body: NoteIn, user: User = Depends(require_mother), db: Session = Depends(get_db)
):
    note = _own_note(db, child_id, note_id, user)
    note.content = body.content
    note.input_method = NoteInputMethod(body.inputMethod)
    db.commit()
    return note_out(note)


@router.delete("/{child_id}/notes/{note_id}")
def delete_note(child_id: str, note_id: str, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    db.delete(_own_note(db, child_id, note_id, user))
    db.commit()
    return {"ok": True}
