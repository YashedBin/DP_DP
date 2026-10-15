# EOMS / YDVRC

**Online Examination Management System** — Your Digital Venue for Rigorous Certification.
Semester 5 Design Patterns Lab project.

## Migration status

This repository is being rebuilt as a **single-JVM Java 17+ Swing/AWT desktop
application** with PostgreSQL 16 accessed through plain JDBC. The old Spring
Boot/Maven server, REST client and browser assets have been removed. The current
desktop scaffold launches a small window that previews **unsaved** exam drafts.
Authentication, JDBC persistence, complete Swing workflows and ten integrated
patterns are still to come; the preview does not connect to the database.

The intended dependency flow is Swing UI → services/patterns → JDBC repositories
→ PostgreSQL. Dependencies will be constructed explicitly, with no framework,
HTTP layer or build tool. The existing driver is `lib/postgresql-42.7.13.jar`.

## What is here now

- `src/com/eoms/model/`: framework-free exam, question, answer, student, teacher
  and submission models retained from the earlier codebase.
- `src/com/eoms/Main.java`: Swing entry point; `config/ApplicationContext.java`
  constructs the draft service and passes it to the window explicitly.
- `src/com/eoms/service/`: exam draft validation, independent of Swing.
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

The PostgreSQL driver is included on the classpath for the later JDBC milestone;
the preview itself needs only the JDK. On non-Windows shells, use `:` instead of
`;` as the classpath separator.

## Database preservation

No database is modified by this migration. For a **new** database, create
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
