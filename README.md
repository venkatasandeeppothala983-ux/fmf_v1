# Find My Faculty — College Portal

A Java 11 / Spring Boot portal backed by MySQL, with a single role-checked login for **Student**, **Faculty**, and **Admin**. The existing *Find My Faculty* brand is retained; **College Portal** is the broader product label.

## What is included

- Responsive, keyboard-accessible login and role-specific dashboards; password show/hide; persistent light/dark theme; session logout; account-recovery guidance.
- Database-backed login using BCrypt password hashes. The selected login type is checked against the role stored on the account; it is not an authorization source. Admin, faculty, and student routes are protected server-side.
- Student profile, section timetable and current/next period context, faculty directory/search, appointment requests, notifications, and announcements.
- Faculty timetable/status view, temporary self-reported status and location with a required expiry, meeting-request workflow, and notifications.
- Admin overview/reports and create/update/deactivate flows for students, faculty profiles, departments, subjects, classrooms, linked accounts, faculty/class timetables, and announcements. Destructive actions are confirmed in the UI; referenced catalog records are deactivated rather than hard-deleted.
- Normalized JPA entities, MySQL schema/data script, source-derived sample timetables, and integration tests.

### Data provenance and availability

The seed data preserves the supplied **31 faculty profiles**, **19 class sections**, **528 class timetable slots**, and **214 course rows**. Source-derived faculty/course labels and class-incharge names are retained as provided. No cabin, email, live presence, department, or unverified room is fabricated. Missing values stay empty. `database.sql` includes the five student directory records the user explicitly approved for public deployment (names and IDs), assigned to ADND / III Year / class-05 (S3); they have no login accounts. The SQL script contains no demo passwords or user accounts; a synthetic demo student profile is created only by the isolated preview profile. Availability is labelled **faculty-reported**, **calculated from timetable**, or **not currently known**. Timetable gaps are not represented as proof of availability, and no holiday is inferred from an empty day.

### Technology note

This repository already uses Spring Boot 2.7, Java 11, the embedded Servlet/Tomcat stack, Spring Security, and Spring Data JPA. Persistence calls use parameterized JPA repositories (backed by prepared SQL statements) rather than handwritten JDBC. This preserves the existing, tested architecture while delivering the requested Java/Servlet/MySQL application.

## Requirements

- Java 11
- Maven 3.8+ (the project has been built with Maven 3.9.9)
- MySQL 8.0+ for persistent use
- Docker Compose is optional; the application can also connect to a separately managed MySQL server.

## Render Free public demo

The user selected a Render Free demo. `Dockerfile`, `render.yaml`, and `deploy/render/README.md` configure the Spring Boot preview profile with ephemeral in-memory H2. This is for friends to try, not a 24/7 or production service: Render Free sleeps after 15 minutes idle, and H2 data resets when the instance restarts. The five user-approved directory records are seeded in the demo without individual logins. Render's built-in free Postgres is not used. **No Render deployment has occurred**: there is no Render account or Git repository connected here, so the project must first be pushed to a GitHub repository and deployed from the Render dashboard. Set demo credentials only in Render's environment settings; never commit secrets.

An OCI Always Free VM + HeatWave alternative remains documented in `deploy/oracle-free/README.md` and `deploy/oracle-free/college-portal.service`. It is also best-effort, not an uptime commitment. For an always-on portal, use a paid always-running service with a persistent database and backups.

## Run with MySQL

1. Copy `.env.example` to `.env` and replace every placeholder with unique values. Do not use the example values in production. `.env` is ignored by Git.
2. Start MySQL and import the schema/data. With Docker Compose, the SQL script is mounted into MySQL's first-start initialization directory:

   ```bash
   docker compose up -d mysql
   ```

   The initialization script runs only when the MySQL data volume is first created. For an existing database, import it manually into a new/empty schema:

   ```bash
   mysql -h 127.0.0.1 -u "$MYSQL_USER" -p < database.sql
   ```

3. Export the application settings from `.env`, then run Spring Boot from the project root:

   ```bash
   set -a
   . ./.env
   set +a
   mvn spring-boot:run
   ```

   Open `http://localhost:8080`.

`ADMIN_USERNAME` and `ADMIN_PASSWORD` create the first admin account at startup when set. The password must be at least 12 characters; the application stores a BCrypt hash. If those variables are unset, no admin account is silently created. Use the Admin **Accounts** area to create faculty logins and reset passwords. Faculty logins must link to an existing faculty profile; student account creation in the Students area requires a username and password, while directory-only student records can remain without login access.

`database.sql` creates the normalized tables and imports the source-derived schedule/catalog data into a fresh database. It also creates the user-approved ADND department label and five public directory-only student rows; it creates no demo credentials, personal contact details, or live status. Add only verified departments/rooms through the portal or a reviewed migration. Hibernate uses `ddl-auto: update` for compatible additive development changes; production schema changes should be reviewed and applied as controlled migrations.

## Run an isolated demo without MySQL

The `preview` profile uses an in-memory H2 database in MySQL compatibility mode and seeds the supplied faculty/section schedules. Supply synthetic demo account credentials in the environment—there is no role impersonation or default password:

```bash
ADMIN_USERNAME=portal-admin ADMIN_PASSWORD='replace-with-unique-admin-password' \
PREVIEW_STUDENT_USERNAME=demo-student PREVIEW_STUDENT_PASSWORD='replace-with-unique-student-password' \
PREVIEW_FACULTY_USERNAME=demo-faculty PREVIEW_FACULTY_PASSWORD='replace-with-unique-faculty-password' \
SPRING_PROFILES_ACTIVE=preview mvn spring-boot:run
```

All passwords must be 12–72 characters. The preview profile permits iframe embedding for the Arena live preview and sets its session cookie `SameSite=None; Secure`. Because some browsers block third-party iframe cookies entirely, preview login also issues a random 30-minute in-memory bearer token only after password/role verification; the browser keeps it in tab-scoped storage and logout revokes it. The production/default profile has no bearer fallback, retains frame denial and `SameSite=Lax`, and uses the regular server session. In preview only, the role selector may read the configured synthetic credentials from a read-only endpoint and autofill the form; all sign-ins still use `POST /api/auth/login` and verify a stored hashed password. No role-only login endpoint exists. The generic demo student login uses a synthetic profile (`DEMO-STU-001`). The preview profile also seeds five user-approved student directory records under the user-provided `ADND` label and the verified `class-05` cohort (III Year, V Semester, S3); those five entries have no individual login accounts. The user has separately approved including these same identifiable records on the intended permanent public site; do not add other personal records without approval. The demo faculty login links to the existing Dr. M. Saraswathi source record (`staff-07`), not a fabricated faculty person. H2 data is ephemeral and is lost when the application stops. Never expose preview credentials or the preview profile in a public production deployment.

## Build and test

```bash
mvn test
mvn package
```

The current automated suite has **17 passing tests** (15 API/session integration tests plus two timetable-data tests). It covers public routes, schedule/section data, session login, generic role-mismatch login failure, unauthenticated and cross-role access control, account disablement during an existing session, admin-created student accounts with hashed passwords and profiles, admin-authored announcement persistence/visibility, and the preview-only bearer fallback with logout revocation.

## Test cases and expected results

| Case | Expected result |
|---|---|
| `GET /api/health` without a session | `200`, `{"status":"ok"}` |
| `GET /api/faculty`, `GET /api/sections` | `200`; source set has 31 profiles and 19 sections |
| Login with matching username/password and login type | `200`; session is established from the stored account role |
| Correct password but mismatched login type | Generic `401 Invalid username or password` |
| Missing/invalid credentials | `401`; no account-existence detail is disclosed |
| Student requests `/api/admin/**` or writes faculty availability | `403` |
| Anonymous user requests protected admin or status write | `401` |
| Account is disabled while a session exists | Existing session is invalidated on its next request |
| Admin creates student via `/api/admin/students` | `201`; hashed password and student profile are persisted; new account can log in |
| Admin publishes an announcement | `201`; author is persisted; admin/student audience views return the appropriate record |
| Faculty submits a temporary status | Stored as a faculty-reported update until its expiry; accepted manual values are Available, In a Meeting, On Duty, or Unavailable |
| Current/next period calculation | Derived only from matching day/time timetable intervals; missing/empty schedule is reported as unknown/no source schedule |
| Timetable write overlaps another period | `409 Conflict` for detected faculty/class-section or same-table classroom overlap |
| Logout | Session invalidated; subsequent protected request requires login |

The tests use H2 and do not substitute for a MySQL 8 acceptance test.

## Important API routes

- Authentication: `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- Students: `GET/PUT /api/students/me`, `GET /api/students/me/timetable`, `GET /api/students/me/today`
- Faculty: `GET /api/faculty/me/status`, `GET /api/faculty/me/timetable`, `POST/DELETE /api/faculty/{id}/status`
- Portal: `GET /api/announcements`, `GET /api/notifications`, `POST /api/notifications/{id}/read`
- Meetings: `POST /api/appointments`, `GET /api/appointments/mine`, `PUT /api/appointments/{id}/decision`
- Admin: `/api/admin/summary`, `/api/admin/reports/overview`, and CRUD routes for `/students`, `/faculty`, `/departments`, `/subjects`, `/classrooms`, `/accounts`, `/timetables/faculty`, `/timetables/classes`, and `/announcements`.

A 30-minute servlet session timeout and HttpOnly/SameSite=Lax cookie are configured. Set `SESSION_COOKIE_SECURE=true` when serving over HTTPS. Logout invalidates the session, login rotates an existing session ID, and disabled accounts are rechecked on authenticated requests. The frontend uses same-origin relative API routes and does not depend on external assets/CDNs.

## Project tree

```text
find-my-faculty-backend/
├── database.sql
├── Dockerfile
├── render.yaml
├── docker-compose.yml
├── deploy/
│   ├── render/README.md       # Render Free public demo runbook
│   └── oracle-free/           # OCI Always Free alternative runbook
├── .dockerignore
├── .env.example
├── README.md
├── timetable-import-template.csv
├── pom.xml
└── src/
    ├── main/java/com/findmyfaculty/
    │   ├── api/          # Auth, student/faculty/admin controllers and DTOs
    │   ├── config/       # Session/security, CORS, active-account filter
    │   ├── domain/       # Normalized JPA entities and enums
    │   ├── repo/         # Spring Data repositories
    │   └── service/      # Availability, timetable import/seed, CRUD services; preview-only demo profiles
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-preview.yml
    │   ├── seed/         # Supplied staff/class timetable JSON
    │   └── static/index.html
    └── test/java/com/findmyfaculty/
        ├── api/          # API/session/role integration tests
        └── service/      # Source timetable data tests
```

## Verification notes

- Maven compile and all 17 automated tests pass in the provided environment, including preview directory-only student records and the Saraswathi faculty login mapping.
- The live Cloudflare preview was smoke-tested against H2: CORS-origin login, source counts, all five approved student directory records, the student-record no-login state, and Dr. M. Saraswathi's S3 timetable returned expected responses. Earlier checks also covered role blocking, admin summary, announcements, status, and appointment APIs.
- Chromium QA covered all 10 Admin navigation pages at desktop (1440 px) and mobile (390 px) widths: no page overflow or uncaught page errors; student-table headers/cells align and actions do not overlap. On mobile, directory rows display as labeled cards.
- Docker is not installed in the provided environment, so the MySQL container, MySQL 8 import, and persistent MySQL workflow were not executed here. Run the MySQL steps above in an environment with Docker/MySQL before production use.
