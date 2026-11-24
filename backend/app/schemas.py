"""Request bodies. Field names stay camelCase so the JSON contract matches the mobile app."""

from datetime import date
from typing import Annotated, Literal

from pydantic import AwareDatetime, BaseModel, EmailStr, Field, StringConstraints

Item = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=80)]
Items = Annotated[list[Item], Field(max_length=30)]
Required = lambda n: Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=n)]  # noqa: E731
Optional = lambda n: Annotated[str | None, Field(default=None, max_length=n)]  # noqa: E731


class LoginIn(BaseModel):
    email: EmailStr
    password: Annotated[str, Field(min_length=1)]


class ChildIn(BaseModel):
    firstName: Required(80)
    birthDate: date
    avatar: Optional(120) = None
    description: Optional(3000) = None
    personalityTags: Items = []
    interests: Items = []
    favoriteActivities: Items = []
    favoriteColors: Items = []
    birthdayThemes: Items = []
    giftWishes: Items = []
    comfortPreferences: Items = []
    approvedPlanningSummary: Optional(3000) = None


class NoteIn(BaseModel):
    content: Required(5000)
    inputMethod: Literal["TEXT", "VOICE"]


class EventIn(BaseModel):
    type: Literal["BIRTHDAY", "SOS_EVENT"]
    title: Required(180)
    description: Optional(5000) = None
    category: Optional(100) = None
    eventDate: AwareDatetime
    location: Required(180)
    birthdayChildId: str | None = None
    theme: Optional(180) = None
    plannedActivities: Items = []
    suggestedGiftIdeas: Items = []
    preparationNotes: Optional(5000) = None
    childIds: Annotated[list[str], Field(max_length=100)]
    checklist: Items = []


class StatusIn(BaseModel):
    status: Literal["AWAITING_APPROVAL", "CONFIRMED", "COMPLETED", "CANCELLED"]


class TaskIn(BaseModel):
    title: Required(180)


class TaskPatch(BaseModel):
    completed: bool | None = None
    title: Annotated[str | None, StringConstraints(strip_whitespace=True, min_length=1, max_length=180)] = None


class ApprovalIn(BaseModel):
    status: Literal["APPROVED", "DECLINED", "CHANGES_REQUESTED"]
    comment: Optional(2000) = None


class FeedbackIn(BaseModel):
    childId: str
    enjoyed: bool
    comfortable: bool
    favoriteActivity: Optional(180) = None
    comment: Optional(3000) = None


# --- AI ---------------------------------------------------------------------------------------

Short = Annotated[list[Annotated[str, Field(max_length=180)]], Field(max_length=15)]


class OrganizedSuggestions(BaseModel):
    interests: Annotated[list[Annotated[str, Field(max_length=80)]], Field(max_length=15)]
    comfortPreferences: Annotated[list[Annotated[str, Field(max_length=120)]], Field(max_length=15)]
    giftWishes: Annotated[list[Annotated[str, Field(max_length=120)]], Field(max_length=15)]
    birthdayThemes: Annotated[list[Annotated[str, Field(max_length=120)]], Field(max_length=15)]


class BirthdayPlan(BaseModel):
    theme: Annotated[str, Field(max_length=180)]
    activities: Short
    giftIdeas: Short
    decorations: Short
    accommodations: Short
    checklist: Annotated[list[Annotated[str, Field(max_length=180)]], Field(max_length=20)]
    explanation: Annotated[str, Field(max_length=1000)]


class EventPlan(BaseModel):
    activities: Short
    materials: Short
    sequence: Short
    groupOrganization: Annotated[str, Field(max_length=1000)]
    accommodations: Short
    checklist: Annotated[list[Annotated[str, Field(max_length=180)]], Field(max_length=20)]


class OrganizeNoteIn(BaseModel):
    childId: str
    content: Required(5000)


class ApproveNoteIn(BaseModel):
    childId: str
    suggestions: OrganizedSuggestions


class BirthdayPlanIn(BaseModel):
    childId: str


class EventPlanIn(BaseModel):
    title: Annotated[str, Field(max_length=180)]
    category: Annotated[str, Field(max_length=100)]
    childIds: Annotated[list[str], Field(min_length=1, max_length=100)]
