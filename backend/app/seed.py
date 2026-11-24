"""Fictional demo data, same content as the web app's prisma/seed.ts. Run: python -m app.seed"""

import os
from datetime import date, datetime, timedelta

from sqlalchemy.orm import Session

from app.db import Base, SessionLocal, engine
from app.models import (
    ApprovalStatus,
    Child,
    ChildNote,
    Event,
    EventFeedback,
    EventParticipant,
    EventStatus,
    EventTask,
    EventType,
    NoteInputMethod,
    Role,
    User,
    utcnow,
)
from app.security import hash_password

NAMES = ["Yasmine", "Adam", "Inès", "Youssef", "Maya", "Rayan", "Lina", "Sami", "Nour", "Ali"]
AVATARS = ["🎨", "🚀", "🌸", "⚽", "🦋", "🎵", "📚", "🌟", "🐱", "🎮"]


def days(offset: int) -> datetime:
    return utcnow() + timedelta(days=offset)


def birth(offset: int, year: int) -> date:
    day = days(offset).date()
    try:
        return day.replace(year=year)
    except ValueError:  # 29 February in a non-leap year
        return day.replace(year=year, day=28)


def mother_of(index: int) -> str:
    return "demo-mother-a" if index < 6 else "demo-mother-b"


def upsert(db: Session, model, id_: str, **values):
    row = db.get(model, id_)
    if row is None:
        row = model(id=id_, **values)
        db.add(row)
    return row


def seed(db: Session, password: str) -> None:
    password_hash = hash_password(password)
    for id_, name, email, role in [
        ("demo-admin", "Admin Demo", "admin@dreamy.com", Role.ADMIN),
        ("demo-mother-a", "Mother1", "mother1@dreamy.com", Role.MOTHER),
        ("demo-mother-b", "Mother2", "mother2@dreamy.com", Role.MOTHER),
    ]:
        user = upsert(db, User, id_, name=name, email=email, role=role, password_hash=password_hash)
        user.name, user.email, user.role, user.password_hash, user.active = name, email, role, password_hash, True
    db.flush()

    for i, name in enumerate(NAMES):
        birth_date = birth(20 if i == 0 else 50 if i == 6 else 80 + i * 13, 2015 + i % 5)
        child = upsert(
            db,
            Child,
            f"demo-child-{i}",
            mother_id=mother_of(i),
            first_name=name,
            birth_date=birth_date,
            avatar=AVATARS[i],
            description=f"{name} aime partager de bons moments avec les autres.",
            interests=["dessin", "animaux"] if i == 0 else ["musique", "lecture"] if i == 6 else ["jeux créatifs", "nature"],
            favorite_activities=["peinture", "coloriage"] if i == 0 else ["atelier créatif"],
            birthday_themes=["animaux", "art"] if i == 0 else ["couleurs"],
            gift_wishes=["crayons de couleur"] if i == 0 else ["livres"],
            comfort_preferences=["petits groupes", "espace calme"] if i == 0 else ["pauses régulières"],
            approved_planning_summary=(
                "Aime le dessin et les animaux. Préfère un petit groupe et un coin calme."
                if i == 0
                else "Apprécie les activités créatives et les pauses régulières."
            ),
        )
        child.birth_date = birth_date
    db.flush()

    upsert(
        db,
        ChildNote,
        "demo-note-1",
        child_id="demo-child-0",
        mother_id="demo-mother-a",
        input_method=NoteInputMethod.TEXT,
        content="Yasmine aime dessiner les animaux. Elle préfère les petites fêtes calmes et voudrait des crayons de couleur.",
    )
    upsert(
        db,
        ChildNote,
        "demo-note-2",
        child_id="demo-child-6",
        mother_id="demo-mother-b",
        input_method=NoteInputMethod.TEXT,
        content="Lina aime écouter de la musique et lire après les activités de groupe.",
    )

    events = [
        ("demo-birthday-1", EventType.BIRTHDAY, "Anniversaire créatif de Yasmine", 20, "Maison SOS · Salle des ateliers",
         "demo-child-0", [0, 1, 2], EventStatus.AWAITING_APPROVAL, "Animaux et couleurs",
         ["Dessiner des animaux", "Atelier coloriage"], ["Préparer les crayons", "Installer un coin calme"]),
        ("demo-birthday-2", EventType.BIRTHDAY, "Anniversaire musical de Lina", 50, "Maison SOS · Jardin",
         "demo-child-6", [6, 7, 8], EventStatus.DRAFT, "Musique douce",
         ["Atelier de rythme", "Lecture partagée"], ["Préparer les instruments", "Décorer le jardin"]),
        ("demo-event-1", EventType.SOS_EVENT, "Journée nature et création", 35, "Parc municipal",
         None, [0, 3, 6, 9], EventStatus.AWAITING_APPROVAL, None,
         ["Découverte du parc", "Collage de feuilles"], ["Vérifier les autorisations", "Prévoir de l’eau"]),
        ("demo-event-2", EventType.SOS_EVENT, "Atelier des couleurs", -12, "Maison SOS · Salle créative",
         None, [1, 2, 7, 8], EventStatus.COMPLETED, None,
         ["Peinture collective", "Exposition des créations"], ["Préparer le matériel", "Ranger la salle"]),
    ]
    for id_, type_, title, offset, location, birthday_child, kids, status, theme, activities, tasks in events:
        event = db.get(Event, id_)
        if event is None:
            event = Event(
                id=id_,
                type=type_,
                title=title,
                event_date=days(offset),
                location=location,
                birthday_child_id=birthday_child,
                theme=theme,
                planned_activities=activities,
                suggested_gift_ideas=["Matériel créatif"] if type_ == EventType.BIRTHDAY else [],
                status=status,
                organizer_id="demo-admin",
                category="Anniversaire" if type_ == EventType.BIRTHDAY else "Atelier collectif",
            )
            event.participants = [
                EventParticipant(
                    child_id=f"demo-child-{k}",
                    mother_id=mother_of(k),
                    approval_status=ApprovalStatus.APPROVED if status == EventStatus.COMPLETED else ApprovalStatus.PENDING,
                )
                for k in kids
            ]
            event.tasks = [
                EventTask(id=f"{id_}-task-{p}", title=t, position=p, completed=status == EventStatus.COMPLETED)
                for p, t in enumerate(tasks)
            ]
            db.add(event)
    db.flush()

    if db.query(EventFeedback).filter_by(event_id="demo-event-2", child_id="demo-child-1").first() is None:
        db.add(
            EventFeedback(
                event_id="demo-event-2",
                child_id="demo-child-1",
                mother_id="demo-mother-a",
                enjoyed=True,
                comfortable=True,
                favorite_activity="Peinture collective",
                comment="Adam était fier de montrer son dessin.",
            )
        )
    db.commit()


if __name__ == "__main__":
    demo_password = os.getenv("DEMO_PASSWORD") or "sosve2026"
    Base.metadata.create_all(engine)
    with SessionLocal() as session:
        seed(session, demo_password)
    print("Fictional demo data ready: admin@dreamy.com, mother1@dreamy.com, mother2@dreamy.com.")
