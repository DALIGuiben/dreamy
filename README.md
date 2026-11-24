# Dreamy Mobile

Native Android port of the Dreamy web app (SOS ISIMM · Latino Impact), with its own backend.
This folder is self-contained: it needs nothing from `dreamy-sosve`.

```
dreamy-mobile/
├── backend/   FastAPI service: auth, children, notes, events, AI, transcription
└── app/       Android app: Kotlin, Jetpack Compose, MVVM, Hilt
```

All data is fictional demo data. Children have no accounts.

---

## 1. Backend (FastAPI)

Requirements: Python 3.11 or newer.

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate            # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env            # macOS/Linux: cp .env.example .env
```

Edit `.env`:

| Variable | Purpose |
| --- | --- |
| `AUTH_SECRET` | **Required.** Random string of 32+ characters that signs access tokens. Generate one with `python -c "import secrets; print(secrets.token_urlsafe(48))"`. |
| `DATABASE_URL` | SQLite by default (`backend/dreamy.db`). MySQL: `mysql+pymysql://USER:PASSWORD@HOST:3306/dreamy`. |
| `GROQ_API_KEY` | GroqCloud key for the AI suggestions and voice transcription. Without it the manual features still work and AI calls return a clear error. |
| `GROQ_LLM_MODEL`, `GROQ_STT_MODEL` | Default to `openai/gpt-oss-20b` and `whisper-large-v3-turbo`. |
| `AI_ENABLED` | Set to `false` to switch AI off. |
| `DEMO_PASSWORD` | Password given to the demo accounts by the seed (default `sosve2026`). |

Then create the demo data and start the server:

```bash
python -m app.seed                                   # development only
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

API docs: http://localhost:8000/docs. Tests: `python -m pytest`.

Demo accounts (fictional), password `sosve2026` unless `DEMO_PASSWORD` was set:

| Role | E-mail |
| --- | --- |
| Organisation (admin) | `admin@dreamy.com` |
| Maman A | `mother1@dreamy.com` |
| Maman B | `mother2@dreamy.com` |

Tables are created automatically on start-up (`Base.metadata.create_all`). There are no migrations yet,
so a schema change on an existing database needs a manual migration or a fresh database.

---

## 2. Android app

Requirements: Android Studio (Ladybug or newer, bundled JDK 21) and Android SDK 36.
minSdk 24, targetSdk/compileSdk 36.

1. Open `dreamy-mobile/` in Android Studio (it writes `local.properties` with `sdk.dir`).
2. Point the app at the backend in `local.properties` (never committed):
   ```properties
   # Emulator -> backend on the host machine
   dreamy.baseUrl=http://10.0.2.2:8000/
   # Physical device on the same Wi-Fi: use the PC's LAN address
   # dreamy.baseUrl=http://192.168.1.20:8000/
   ```
   It becomes `BuildConfig.BASE_URL`. Debug builds allow plain HTTP; release builds require HTTPS.
3. Run the `app` configuration, or from a terminal:
   ```bash
   ./gradlew :app:assembleDebug        # Windows: gradlew.bat :app:assembleDebug
   ./gradlew :app:installDebug
   ```

### Stack

Kotlin 2.2 · Jetpack Compose (Material 3, BOM 2025.12) · MVVM (ViewModel + StateFlow) · Navigation Compose
(type-safe routes) · Hilt · Retrofit + OkHttp + kotlinx.serialization · DataStore · Coil 3 · java.time via
core-library desugaring. Versions live in `gradle/libs.versions.toml`.

### Package layout (`app/src/main/java/com/dreamy/mobile`)

| Package | Contents |
| --- | --- |
| `domain.model` | Data classes and enums (the former Prisma/TypeScript types) and small selectors (`pendingApprovals`, `upcoming`, `byNextBirthday`). |
| `data.remote` | `DreamyApi` (Retrofit), request bodies, `AuthInterceptor` (bearer token, 401/403 handling), `apiCall` error mapping. |
| `data.local` | `SessionStore`: token and user in DataStore. |
| `data.repository` | `AuthRepository`, `ChildRepository`, `EventRepository`. |
| `di` | Hilt module: DataStore, Json, OkHttp, Retrofit. |
| `navigation` | Routes (one per web route), `DreamyNavHost`, tab navigation. |
| `ui` | `theme`, shared `components`, `shell` (role layout) and one package per feature. |
| `util` | Date formatting (French), labels, serializers, `VoiceRecorder`. |

---

## 3. How the Next.js app maps to this project

### Routes → screens

| Next.js route | Android screen |
| --- | --- |
| `/` | `ui/home/HomeScreen` |
| `/login` | `ui/auth/LoginScreen` + `LoginViewModel` |
| `/unauthorized` | `ui/auth/UnauthorizedScreen` |
| `/presentation` | `ui/presentation/PresentationScreen` (slides bundled as drawables; download opens `/presentation/download`) |
| `/mother/layout`, `/admin/layout` (`Shell`) | `ui/shell/AppShell`: top bar (logo, role, name, logout) + bottom navigation |
| `/mother/dashboard` | `ui/mother/MotherDashboardScreen` |
| `/admin/dashboard` | `ui/admin/AdminDashboardScreen` |
| `/{role}/children` | `ui/children/ChildrenScreen` (admin search goes through `?q=`) |
| `/{role}/children/[id]` | `ui/children/ChildDetailScreen` (mother: notes, dictation, AI; admin: read-only) |
| `/mother/children/new`, `/[id]/edit` | `ui/children/ChildFormScreen` |
| `/{role}/birthdays` | `ui/birthdays/BirthdaysScreen` |
| `/{role}/events` | `ui/events/EventsScreen` |
| `/{role}/events/[id]` | `ui/events/EventDetailScreen` (event view + actions) |
| `/admin/events/new[?type=BIRTHDAY]`, `/[id]/edit` | `ui/events/EventFormScreen` |

Screens shared by both roles take an `admin` route argument, like the web's `role` prop.

### Concepts

| Next.js | Android |
| --- | --- |
| React components | `@Composable` functions (`ui/components`: `DreamyCard`, `Pill`, `EmptyState`, `EventList`, `ChildProfile`, …) |
| `useState` / react-hook-form + zod | `StateFlow` state in ViewModels, with the same validation rules and messages |
| Server components querying Prisma | ViewModels calling repositories in `viewModelScope`; dashboards and birthdays are computed from `/api/children` and `/api/events` |
| `router.refresh()` | Reload in the ViewModel after a mutation, and a silent refresh on `ON_RESUME` |
| `confirm()` | `ConfirmDialog` (Material `AlertDialog`) |
| NextAuth credentials + JWT cookie | `POST /api/auth/login` → bearer JWT stored in DataStore; `AuthInterceptor` adds it; a 401 signs out and a 403 opens "Accès refusé" |
| `pageUser(role)` layouts | Role check in `DreamyNavHost.shellScreen` plus server-side checks on every endpoint |
| `/api/*` route handlers | FastAPI routers in `backend/app/routers` (same paths, rules and French error messages) |
| Prisma schema | SQLAlchemy models in `backend/app/models.py` |
| `prisma/seed.ts` | `backend/app/seed.py` |
| `lib/ai.ts` (Groq, quotas) | `backend/app/ai.py` (same prompts, 30 LLM / 10 transcriptions per user per day, timeouts) |
| Tailwind theme (`globals.css`) | `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` (cyan `#08A9E0`, navy `#1D3768`, coral `#E2556A`, pale `#EFFAFF`, light `#CFEAF5`) |
| Arial / Helvetica | Platform sans-serif (no bundled font) |
| `lucide-react` icons | Material Icons Extended |
| `next/image` | Coil `AsyncImage` (slides) and `painterResource` (logo) |
| `MediaRecorder` (browser, WebM) | `android.media.MediaRecorder` → AAC/MP4 (`audio/mp4`), 90-second limit, mic permission requested at use |
| `.env` | Backend `.env`; the app only gets `dreamy.baseUrl` from `local.properties` → `BuildConfig.BASE_URL` |

---

## 4. Differences and known limitations

- **Auth:** NextAuth is replaced by a stateless JWT (12 h, the same lifetime as the web session). Logging out
  deletes the token on the device. There is no server-side revocation list.
- **Presentation page:** reachable from a "Découvrir la présentation" link on the home screen; the web
  page was reachable by URL only.
- **Feedback form:** the "Retour après l’événement" form is pre-filled with the saved answers instead of
  starting from Oui/Oui.
- **Transcription:** the backend reads the audio duration with `mutagen`, which handles MP4/M4A, OGG, WAV
  and MP3 but not WebM. That doesn't affect the app, which records MP4; a web client sending WebM would need
  another reader (for example ffprobe).
- **Text:** UI text is French and written inline in the composables, as in the web components. Moving it to
  `strings.xml` would be the next step if translation is needed.
- **Layout:** the web's two-column desktop grids are single columns on phones; there is no tablet-specific layout.
- **Dark mode:** not provided, matching the web app's single light theme.
