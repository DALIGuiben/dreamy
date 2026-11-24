from datetime import timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db import get_db
from app.errors import ApiError
from app.models import (
    ApprovalStatus,
    Child,
    Event,
    EventFeedback,
    EventParticipant,
    EventStatus,
    EventTask,
    EventType,
    Role,
    User,
    utcnow,
)
from app.schemas import ApprovalIn, EventIn, FeedbackIn, StatusIn, TaskIn, TaskPatch
from app.security import current_user, require_admin, require_mother
from app.serializers import event_out, feedback_out, participant_out, task_out

router = APIRouter(prefix="/api/events")

LOCKED = (EventStatus.COMPLETED, EventStatus.CANCELLED)
TRANSITIONS: dict[EventStatus, set[EventStatus]] = {
    EventStatus.DRAFT: {EventStatus.AWAITING_APPROVAL, EventStatus.CANCELLED},
    EventStatus.AWAITING_APPROVAL: {EventStatus.CONFIRMED, EventStatus.CANCELLED},
    EventStatus.CHANGES_REQUESTED: {EventStatus.AWAITING_APPROVAL, EventStatus.CANCELLED},
    EventStatus.CONFIRMED: {EventStatus.COMPLETED, EventStatus.CANCELLED},
    EventStatus.COMPLETED: set(),
    EventStatus.CANCELLED: set(),
}


def _event(db: Session, event_id: str) -> Event:
    event = db.get(Event, event_id)
    if event is None:
        raise ApiError(404, "Événement introuvable.")
    return event


def _editable(db: Session, event_id: str) -> Event:
    event = _event(db, event_id)
    if event.status in LOCKED:
        raise ApiError(409, "Cet événement ne peut plus être modifié.")
    return event


def _validated_children(db: Session, body: EventIn) -> list[Child]:
    if body.type == "BIRTHDAY" and (not body.birthdayChildId or body.birthdayChildId not in body.childIds):
        raise ApiError(400, "Sélectionnez l’enfant concerné par l’anniversaire.")
    ids = set(body.childIds)
    children = list(db.scalars(select(Child).where(Child.id.in_(ids)))) if ids else []
    if len(children) != len(ids):
        raise ApiError(400, "Un enfant sélectionné est introuvable.")
    return children


def _apply(event: Event, body: EventIn) -> None:
    event.type = EventType(body.type)
    event.title = body.title
    event.description = body.description
    event.category = body.category
    event.event_date = body.eventDate.astimezone(timezone.utc).replace(tzinfo=None)
    event.location = body.location
    event.birthday_child_id = body.birthdayChildId if body.type == "BIRTHDAY" else None
    event.theme = body.theme
    event.planned_activities = body.plannedActivities
    event.suggested_gift_ideas = body.suggestedGiftIdeas
    event.preparation_notes = body.preparationNotes


@router.get("")
def list_events(user: User = Depends(current_user), db: Session = Depends(get_db)):
    query = select(Event).order_by(Event.event_date)
    if user.role == Role.MOTHER:
        query = query.where(
            Event.status != EventStatus.DRAFT,
            Event.participants.any(EventParticipant.mother_id == user.id),
        )
        return [event_out(e, mother_id=user.id) for e in db.scalars(query)]
    return [event_out(e) for e in db.scalars(query)]


@router.post("", status_code=201)
def create_event(body: EventIn, user: User = Depends(require_admin), db: Session = Depends(get_db)):
    children = _validated_children(db, body)
    if not children:
        raise ApiError(400, "Ajoutez un enfant participant.")
    event = Event(organizer_id=user.id, status=EventStatus.DRAFT)
    _apply(event, body)
    event.participants = [EventParticipant(child_id=c.id, mother_id=c.mother_id) for c in children]
    event.tasks = [EventTask(title=title, position=i) for i, title in enumerate(body.checklist)]
    db.add(event)
    db.commit()
    db.refresh(event)
    return event_out(event)


@router.get("/{event_id}")
def get_event(event_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)):
    event = db.get(Event, event_id)
    if user.role == Role.MOTHER:
        if (
            event is None
            or event.status == EventStatus.DRAFT
            or not any(p.mother_id == user.id for p in event.participants)
        ):
            raise ApiError(404, "Événement introuvable.")
        return event_out(event, mother_id=user.id)
    if event is None:
        raise ApiError(404, "Événement introuvable.")
    return event_out(event)


@router.put("/{event_id}")
def update_event(event_id: str, body: EventIn, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    event = _editable(db, event_id)
    children = _validated_children(db, body)
    _apply(event, body)
    # Any edit sends the project back to draft and resets every mother's approval.
    event.status = EventStatus.DRAFT
    existing = {p.child_id: p for p in event.participants}
    keep = {c.id for c in children}
    for child_id, participant in existing.items():
        if child_id not in keep:
            event.participants.remove(participant)
    for child in children:
        participant = existing.get(child.id)
        if participant is None:
            event.participants.append(EventParticipant(child_id=child.id, mother_id=child.mother_id))
        else:
            participant.approval_status = ApprovalStatus.PENDING
            participant.mother_comment = None
            participant.reviewed_at = None
    event.tasks.clear()
    db.flush()
    event.tasks = [EventTask(title=title, position=i) for i, title in enumerate(body.checklist)]
    db.commit()
    db.refresh(event)
    return event_out(event)


@router.delete("/{event_id}")
def delete_event(event_id: str, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    event = _event(db, event_id)
    if event.status != EventStatus.DRAFT:
        raise ApiError(409, "Seul un brouillon peut être supprimé.")
    db.delete(event)
    db.commit()
    return {"ok": True}


@router.post("/{event_id}/status")
def change_status(event_id: str, body: StatusIn, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    event = _event(db, event_id)
    target = EventStatus(body.status)
    if target not in TRANSITIONS[event.status]:
        raise ApiError(409, "Transition indisponible.")
    if target == EventStatus.AWAITING_APPROVAL and not event.participants:
        raise ApiError(409, "Ajoutez des participants avant de demander leur accord.")
    if target == EventStatus.CONFIRMED and any(
        p.approval_status != ApprovalStatus.APPROVED for p in event.participants
    ):
        raise ApiError(409, "Tous les accords sont requis avant confirmation.")
    if target == EventStatus.COMPLETED and event.event_date > utcnow():
        raise ApiError(409, "L’événement ne peut être terminé avant sa date.")
    event.status = target
    db.commit()
    return event_out(event)


@router.post("/{event_id}/tasks", status_code=201)
def add_task(event_id: str, body: TaskIn, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    event = _editable(db, event_id)
    task = EventTask(event_id=event.id, title=body.title, position=len(event.tasks))
    db.add(task)
    db.commit()
    return task_out(task)


def _task(db: Session, event_id: str, task_id: str) -> EventTask:
    task = db.scalar(select(EventTask).where(EventTask.id == task_id, EventTask.event_id == event_id))
    if task is None:
        raise ApiError(404, "Tâche introuvable.")
    if task.event.status in LOCKED:
        raise ApiError(409, "Cet événement ne peut plus être modifié.")
    return task


@router.patch("/{event_id}/tasks/{task_id}")
def update_task(
    event_id: str, task_id: str, body: TaskPatch, _: User = Depends(require_admin), db: Session = Depends(get_db)
):
    task = _task(db, event_id, task_id)
    if body.completed is not None:
        task.completed = body.completed
    if body.title is not None:
        task.title = body.title
    db.commit()
    return task_out(task)


@router.delete("/{event_id}/tasks/{task_id}")
def delete_task(event_id: str, task_id: str, _: User = Depends(require_admin), db: Session = Depends(get_db)):
    db.delete(_task(db, event_id, task_id))
    db.commit()
    return {"ok": True}


@router.patch("/{event_id}/participants/{participant_id}")
def review(
    event_id: str,
    participant_id: str,
    body: ApprovalIn,
    user: User = Depends(require_mother),
    db: Session = Depends(get_db),
):
    participant = db.scalar(
        select(EventParticipant).where(
            EventParticipant.id == participant_id,
            EventParticipant.event_id == event_id,
            EventParticipant.mother_id == user.id,
        )
    )
    if participant is None:
        raise ApiError(404, "Participation introuvable.")
    if participant.event.status not in (EventStatus.AWAITING_APPROVAL, EventStatus.CHANGES_REQUESTED):
        raise ApiError(409, "L’avis n’est pas ouvert.")
    participant.approval_status = ApprovalStatus(body.status)
    participant.mother_comment = body.comment
    participant.reviewed_at = utcnow()
    if participant.approval_status == ApprovalStatus.CHANGES_REQUESTED:
        participant.event.status = EventStatus.CHANGES_REQUESTED
    db.commit()
    return participant_out(participant)


@router.post("/{event_id}/feedback")
def give_feedback(event_id: str, body: FeedbackIn, user: User = Depends(require_mother), db: Session = Depends(get_db)):
    participant = db.scalar(
        select(EventParticipant).where(
            EventParticipant.event_id == event_id,
            EventParticipant.child_id == body.childId,
            EventParticipant.mother_id == user.id,
        )
    )
    if participant is None:
        raise ApiError(404, "Participation introuvable.")
    if participant.event.status != EventStatus.COMPLETED:
        raise ApiError(409, "Le retour est disponible après l’événement.")
    feedback = db.scalar(
        select(EventFeedback).where(EventFeedback.event_id == event_id, EventFeedback.child_id == body.childId)
    )
    if feedback is None:
        feedback = EventFeedback(event_id=event_id, child_id=body.childId, mother_id=user.id)
        db.add(feedback)
    feedback.enjoyed = body.enjoyed
    feedback.comfortable = body.comfortable
    feedback.favorite_activity = body.favoriteActivity
    feedback.comment = body.comment
    db.commit()
    return feedback_out(feedback)
