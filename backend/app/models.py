import enum
import uuid
from datetime import date, datetime, timezone

from sqlalchemy import JSON, Boolean, Date, DateTime, Enum, ForeignKey, Index, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base


def new_id() -> str:
    return uuid.uuid4().hex


def utcnow() -> datetime:
    """Naive UTC timestamp; every datetime column stores UTC."""
    return datetime.now(timezone.utc).replace(tzinfo=None)


class Role(str, enum.Enum):
    MOTHER = "MOTHER"
    ADMIN = "ADMIN"


class NoteInputMethod(str, enum.Enum):
    TEXT = "TEXT"
    VOICE = "VOICE"


class EventType(str, enum.Enum):
    BIRTHDAY = "BIRTHDAY"
    SOS_EVENT = "SOS_EVENT"


class EventStatus(str, enum.Enum):
    DRAFT = "DRAFT"
    AWAITING_APPROVAL = "AWAITING_APPROVAL"
    CHANGES_REQUESTED = "CHANGES_REQUESTED"
    CONFIRMED = "CONFIRMED"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class ApprovalStatus(str, enum.Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    DECLINED = "DECLINED"
    CHANGES_REQUESTED = "CHANGES_REQUESTED"


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column("createdAt", DateTime, default=utcnow)
    updated_at: Mapped[datetime] = mapped_column("updatedAt", DateTime, default=utcnow, onupdate=utcnow)


class User(TimestampMixin, Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(String(120))
    email: Mapped[str] = mapped_column(String(190), unique=True)
    password_hash: Mapped[str] = mapped_column("passwordHash", String(120))
    role: Mapped[Role] = mapped_column(Enum(Role))
    active: Mapped[bool] = mapped_column(Boolean, default=True)


class Child(TimestampMixin, Base):
    __tablename__ = "children"
    __table_args__ = (Index("ix_children_birth", "birthDate"),)
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    mother_id: Mapped[str] = mapped_column("motherId", ForeignKey("users.id", ondelete="RESTRICT"), index=True)
    first_name: Mapped[str] = mapped_column("firstName", String(80))
    birth_date: Mapped[date] = mapped_column("birthDate", Date)
    avatar: Mapped[str | None] = mapped_column(String(120))
    description: Mapped[str | None] = mapped_column(Text)
    personality_tags: Mapped[list | None] = mapped_column("personalityTags", JSON)
    interests: Mapped[list | None] = mapped_column(JSON)
    favorite_activities: Mapped[list | None] = mapped_column("favoriteActivities", JSON)
    favorite_colors: Mapped[list | None] = mapped_column("favoriteColors", JSON)
    birthday_themes: Mapped[list | None] = mapped_column("birthdayThemes", JSON)
    gift_wishes: Mapped[list | None] = mapped_column("giftWishes", JSON)
    comfort_preferences: Mapped[list | None] = mapped_column("comfortPreferences", JSON)
    approved_planning_summary: Mapped[str | None] = mapped_column("approvedPlanningSummary", Text)

    mother: Mapped[User] = relationship()
    notes: Mapped[list["ChildNote"]] = relationship(
        back_populates="child", cascade="all, delete-orphan", order_by="desc(ChildNote.created_at)"
    )


class ChildNote(TimestampMixin, Base):
    __tablename__ = "child_notes"
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    child_id: Mapped[str] = mapped_column("childId", ForeignKey("children.id", ondelete="CASCADE"), index=True)
    mother_id: Mapped[str] = mapped_column("motherId", ForeignKey("users.id", ondelete="RESTRICT"), index=True)
    content: Mapped[str] = mapped_column(Text)
    input_method: Mapped[NoteInputMethod] = mapped_column("inputMethod", Enum(NoteInputMethod))

    child: Mapped[Child] = relationship(back_populates="notes")


class Event(TimestampMixin, Base):
    __tablename__ = "events"
    __table_args__ = (Index("ix_events_date_status", "eventDate", "status"),)
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    type: Mapped[EventType] = mapped_column(Enum(EventType))
    title: Mapped[str] = mapped_column(String(180))
    description: Mapped[str | None] = mapped_column(Text)
    category: Mapped[str | None] = mapped_column(String(100))
    event_date: Mapped[datetime] = mapped_column("eventDate", DateTime)
    location: Mapped[str] = mapped_column(String(180))
    birthday_child_id: Mapped[str | None] = mapped_column(
        "birthdayChildId", ForeignKey("children.id", ondelete="SET NULL")
    )
    theme: Mapped[str | None] = mapped_column(String(180))
    planned_activities: Mapped[list | None] = mapped_column("plannedActivities", JSON)
    suggested_gift_ideas: Mapped[list | None] = mapped_column("suggestedGiftIdeas", JSON)
    preparation_notes: Mapped[str | None] = mapped_column("preparationNotes", Text)
    status: Mapped[EventStatus] = mapped_column(Enum(EventStatus), default=EventStatus.DRAFT)
    organizer_id: Mapped[str] = mapped_column("organizerId", ForeignKey("users.id", ondelete="RESTRICT"), index=True)

    participants: Mapped[list["EventParticipant"]] = relationship(
        back_populates="event", cascade="all, delete-orphan"
    )
    tasks: Mapped[list["EventTask"]] = relationship(
        back_populates="event", cascade="all, delete-orphan", order_by="EventTask.position"
    )
    feedback: Mapped[list["EventFeedback"]] = relationship(back_populates="event", cascade="all, delete-orphan")


class EventParticipant(Base):
    __tablename__ = "event_participants"
    __table_args__ = (
        UniqueConstraint("eventId", "childId"),
        Index("ix_participants_mother_status", "motherId", "approvalStatus"),
    )
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column("eventId", ForeignKey("events.id", ondelete="CASCADE"))
    child_id: Mapped[str] = mapped_column("childId", ForeignKey("children.id", ondelete="CASCADE"))
    mother_id: Mapped[str] = mapped_column("motherId", ForeignKey("users.id", ondelete="RESTRICT"))
    approval_status: Mapped[ApprovalStatus] = mapped_column(
        "approvalStatus", Enum(ApprovalStatus), default=ApprovalStatus.PENDING
    )
    mother_comment: Mapped[str | None] = mapped_column("motherComment", Text)
    reviewed_at: Mapped[datetime | None] = mapped_column("reviewedAt", DateTime)

    event: Mapped[Event] = relationship(back_populates="participants")
    child: Mapped[Child] = relationship()


class EventTask(TimestampMixin, Base):
    __tablename__ = "event_tasks"
    __table_args__ = (Index("ix_tasks_event_position", "eventId", "position"),)
    id: Mapped[str] = mapped_column(String(80), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column("eventId", ForeignKey("events.id", ondelete="CASCADE"))
    title: Mapped[str] = mapped_column(String(180))
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    position: Mapped[int] = mapped_column(Integer, default=0)

    event: Mapped[Event] = relationship(back_populates="tasks")


class EventFeedback(TimestampMixin, Base):
    __tablename__ = "event_feedback"
    __table_args__ = (UniqueConstraint("eventId", "childId"),)
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column("eventId", ForeignKey("events.id", ondelete="CASCADE"))
    child_id: Mapped[str] = mapped_column("childId", ForeignKey("children.id", ondelete="CASCADE"))
    mother_id: Mapped[str] = mapped_column("motherId", ForeignKey("users.id", ondelete="RESTRICT"), index=True)
    enjoyed: Mapped[bool] = mapped_column(Boolean)
    favorite_activity: Mapped[str | None] = mapped_column("favoriteActivity", String(180))
    comfortable: Mapped[bool] = mapped_column(Boolean)
    comment: Mapped[str | None] = mapped_column(Text)

    event: Mapped[Event] = relationship(back_populates="feedback")


class AiRequest(Base):
    __tablename__ = "ai_requests"
    __table_args__ = (Index("ix_ai_user_created", "userId", "createdAt"),)
    id: Mapped[str] = mapped_column(String(40), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column("userId", ForeignKey("users.id", ondelete="CASCADE"))
    kind: Mapped[str] = mapped_column(String(10))
    created_at: Mapped[datetime] = mapped_column("createdAt", DateTime, default=utcnow)
