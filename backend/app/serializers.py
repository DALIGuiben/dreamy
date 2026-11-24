"""Convert ORM rows to the camelCase JSON the mobile app reads."""

from datetime import datetime

from app.models import Child, ChildNote, Event, EventFeedback, EventParticipant, EventTask, User


def iso(value: datetime | None) -> str | None:
    return None if value is None else value.isoformat(timespec="milliseconds") + "Z"


def strings(value) -> list[str]:
    return [item for item in value if isinstance(item, str)] if isinstance(value, list) else []


def user_out(user: User) -> dict:
    return {"id": user.id, "name": user.name, "email": user.email, "role": user.role.value}


def note_out(note: ChildNote) -> dict:
    return {
        "id": note.id,
        "childId": note.child_id,
        "content": note.content,
        "inputMethod": note.input_method.value,
        "createdAt": iso(note.created_at),
        "updatedAt": iso(note.updated_at),
    }


def child_out(child: Child, with_notes: bool = False) -> dict:
    data = {
        "id": child.id,
        "motherId": child.mother_id,
        "motherName": child.mother.name,
        "firstName": child.first_name,
        "birthDate": child.birth_date.isoformat(),
        "avatar": child.avatar,
        "description": child.description,
        "personalityTags": strings(child.personality_tags),
        "interests": strings(child.interests),
        "favoriteActivities": strings(child.favorite_activities),
        "favoriteColors": strings(child.favorite_colors),
        "birthdayThemes": strings(child.birthday_themes),
        "giftWishes": strings(child.gift_wishes),
        "comfortPreferences": strings(child.comfort_preferences),
        "approvedPlanningSummary": child.approved_planning_summary,
    }
    if with_notes:
        data["notes"] = [note_out(note) for note in child.notes]
    return data


def participant_out(p: EventParticipant) -> dict:
    return {
        "id": p.id,
        "childId": p.child_id,
        "childFirstName": p.child.first_name,
        "motherId": p.mother_id,
        "approvalStatus": p.approval_status.value,
        "motherComment": p.mother_comment,
        "reviewedAt": iso(p.reviewed_at),
    }


def task_out(task: EventTask) -> dict:
    return {"id": task.id, "title": task.title, "completed": task.completed, "position": task.position}


def feedback_out(f: EventFeedback) -> dict:
    return {
        "id": f.id,
        "childId": f.child_id,
        "enjoyed": f.enjoyed,
        "comfortable": f.comfortable,
        "favoriteActivity": f.favorite_activity,
        "comment": f.comment,
    }


def event_out(event: Event, mother_id: str | None = None) -> dict:
    """`mother_id` limits participants and feedback to that mother's children."""
    participants = [p for p in event.participants if mother_id is None or p.mother_id == mother_id]
    feedback = [f for f in event.feedback if mother_id is None or f.mother_id == mother_id]
    return {
        "id": event.id,
        "type": event.type.value,
        "title": event.title,
        "description": event.description,
        "category": event.category,
        "eventDate": iso(event.event_date),
        "location": event.location,
        "birthdayChildId": event.birthday_child_id,
        "theme": event.theme,
        "plannedActivities": strings(event.planned_activities),
        "suggestedGiftIdeas": strings(event.suggested_gift_ideas),
        "preparationNotes": event.preparation_notes,
        "status": event.status.value,
        "participants": [participant_out(p) for p in participants],
        "tasks": [task_out(t) for t in event.tasks],
        "feedback": [feedback_out(f) for f in feedback],
    }
