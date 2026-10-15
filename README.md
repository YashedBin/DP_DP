# EOMS / YDVRC

**Online Examination Management System** — Your Digital Venue for Rigorous Certification.
Semester 5 Design Patterns Lab project.

## Migration status

This repository is being rebuilt as a **single-JVM Java 17+ Swing/AWT desktop
application** with PostgreSQL 16 accessed through plain JDBC. The old Spring
Boot/Maven server, REST client and browser assets have been removed. The current
desktop scaffold launches a small window that previews **unsaved** exam drafts.
JDBC repositories and core services are implemented but not yet exposed in the UI.
Authentication, complete Swing workflows and ten integrated patterns are still
to come; the preview itself does not connect to the database.

The intended dependency flow is Swing UI → services/patterns → JDBC repositories
→ PostgreSQL. Dependencies will be constructed explicitly, with no framework,
HTTP layer or build tool. The existing driver is `lib/postgresql-42.7.13.jar`.

## What is here now

- `src/com/eoms/model/`: framework-free exam, question, answer, student, teacher
  and submission models retained from the earlier codebase.
- `src/com/eoms/Main.java`: Swing entry point; `config/ApplicationContext.java`
  constructs repositories and services and passes the exam service to the window.
- `src/com/eoms/config/DatabaseConnection.java`: environment-based JDBC connection helper.
- `src/com/eoms/repository/`: plain JDBC data access for exams, questions, students,
  submissions and answers, using prepared statements and closed resources.
- `src/com/eoms/service/`: exam creation/lifecycle, draft question editing, student
  registration and transactional submissions; usable independently of Swing.
- `src/com/eoms/ui/WelcomeFrame.java`: temporary, non-persistent desktop preview.
- `sql/schema.sql`: the existing tables, moved without changing their definitions.
- `docs/legacy/`: preserved historical project notes, UML and setup transcript.
  They describe the former server and are not current run instructions.

To compile and launch in PowerShell (JDK 17+), from the repository root:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src -Recurse -Filter *.java).FullName
java -cp "out;lib/postgresql-42.7.13.jar" com.eoms.Main
```

The PostgreSQL driver must be on the runtime classpath for database operations;
the preview itself needs only the JDK. On non-Windows shells, use `:` instead of
`;` as the classpath separator. To run the database-independent checks:

```powershell
javac -cp out -d out (Get-ChildItem tests -Recurse -Filter *.java).FullName
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CoreDomainTest
```

## Database preservation

No database is modified merely by starting the desktop preview. For a **new** database, create
`eoms_db` yourself, then explicitly run the schema using PostgreSQL's `psql`:

```powershell
psql -U postgres -d eoms_db -f sql/schema.sql
```

The schema uses `CREATE TABLE IF NOT EXISTS` and keeps the existing `students`,
`exams`, `questions`, `submissions`, `audit_log`, `answers` and `teachers` tables.
It does **not** upgrade the shape of pre-existing tables. Check your existing
database before applying future migrations; do not drop tables or reset data.
No credentials belong in tracked files. A previous database password was
committed in the removed Spring configuration: **rotate it** if it was real.

Before using the JDBC services, set these environment variables in the same
process/terminal that launches Java (example URL and username only):

```powershell
$env:EOMS_DB_URL = 'jdbc:postgresql://localhost:5432/eoms_db'
$env:EOMS_DB_USER = 'postgres'
# Set EOMS_DB_PASSWORD locally to your own password; never commit it.
```

`DatabaseConnection.open()` reports missing settings and connection failures.
Repositories do not initialize, drop or reset the schema. A submission and its
answers share a transaction; the exam row is locked during duplicate checking.
The database operations have **not** been verified against a live PostgreSQL
instance here. There is no authenticated UI yet: the service-level student-ID
check only confirms that a row exists, not that the caller owns that identity.
Do not treat the core services as secure student/teacher entry points until the
later authorization and question-access milestones are implemented.
