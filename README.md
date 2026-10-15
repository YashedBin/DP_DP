# EOMS / YDVRC

**Online Examination Management System** — Your Digital Venue for Rigorous Certification.
Semester 5 Design Patterns Lab project. This is a single-JVM Java 17+ Swing/AWT
desktop application backed by PostgreSQL 16 via plain JDBC. There is no server,
HTTP API, web frontend, Maven or Spring dependency.

Source is organized directly under `src/`: `Main.java`, `SetupAccounts.java`,
`ui/`, `model/`, `service/`, `repository/`, `database/`, `patterns/` and `config/`.
Additional classes such as `Answer`, the teacher/account repositories, and
`ExamEditorPanel` are retained because the application uses them. Tests live in
`tests/`; the additive SQL upgrade lives in `sql/migrations/`.

## Build and launch (PowerShell, repository root)

Requires JDK 17+, PostgreSQL 16, and the checked-in driver
`lib/postgresql-42.7.13.jar`. Ensure `java`, `javac` and `psql` are available.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src -Recurse -Filter *.java).FullName
javac -cp out -d out (Get-ChildItem tests -Recurse -Filter *.java).FullName
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CoreDomainTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CreationalPatternsTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.GradingPatternsTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CoordinationPatternsTest
java -cp "out;lib/postgresql-42.7.13.jar" Main
```

On non-Windows shells, replace `;` with `:` in the classpath. Compilation and
these tests need no PostgreSQL server; the tests use Java assertions (`-ea`).
The desktop requires a graphical environment. Operations involving JDBC use
background workers; errors are shown in the UI rather than blocking the EDT.

For an **opt-in** database integration check, provision a separate `eoms_test`
database with `sql/schema.sql` and a teacher account with `SetupAccounts`
using that database's connection variables. Set `EOMS_TEST_DB_URL`,
`EOMS_TEST_DB_USER`, `EOMS_TEST_DB_PASSWORD`, `EOMS_TEST_TEACHER` and
`EOMS_TEST_TEACHER_PASSWORD`, then run:

```powershell
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.JdbcIntegrationTest
```

This test **writes and retains** uniquely named rows; it refuses to run unless
the URL contains `eoms_test`. Never point it at a populated project database.

## Database setup (explicit; preserves existing rows)

Check whether `eoms_db` already exists with `psql -U postgres -l`. If it does,
**do not recreate it**. For a new installation only, create the database once
and apply the complete baseline:

```powershell
psql -U postgres -d postgres -c "CREATE DATABASE eoms_db;"
psql -v ON_ERROR_STOP=1 -U postgres -d eoms_db -f sql/schema.sql
```

For a database already created by the legacy backend, **do not reset or reload
the schema**. Back it up, inspect the current columns, then apply this additive
migration once (it adds credentials, grading columns and a question-key table):

```powershell
psql -v ON_ERROR_STOP=1 -U postgres -d eoms_db -f sql/migrations/001_credentials_and_grading.sql
```

The baseline uses `CREATE TABLE IF NOT EXISTS` and does **not** upgrade existing
columns; the migration uses `ADD COLUMN IF NOT EXISTS`. No schema SQL executes
automatically on application startup. Existing student and teacher rows remain
in place; their new password hashes are initially null and require deliberate
account enrollment. The legacy database password was committed in Git history:
**rotate it** if it was real. Never store new secrets in tracked files.

Set credentials in the terminal that launches Java (example URL and user):

```powershell
$env:EOMS_DB_URL = 'jdbc:postgresql://localhost:5432/eoms_db'
$env:EOMS_DB_USER = 'postgres'
$env:EOMS_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host -AsSecureString 'Database password')).Password
```

`DatabaseConnection` checks these variables when opening a connection. No
hardcoded password, automatic database reset or network service is used.

### Accounts

From an interactive console, run the following **after** the schema is applied
and environment variables are set. This explicitly creates the **first** teacher
only when `teachers` is empty, prompting for a password without echoing it:

```powershell
java -cp "out;lib/postgresql-42.7.13.jar" SetupAccounts
```

If legacy teacher rows already exist, have the database administrator verify
ownership, then set the selected existing teacher's password with
`SetupAccounts --teacher USERNAME`. To enroll an **existing** student,
use `SetupAccounts --student ID` only after verifying that student's
identity. A new student can register through the login window. The CLI's
database credentials grant administrator-level account setup: keep them private.
These local login controls are appropriate for a lab desktop app, not a hardened
multi-user production deployment.

## Workflows and boundaries

- Teacher: log in; create/configure drafts; add/edit/delete questions and MCQ
  keys; duplicate exams and their keys; save and restore in-session named draft
  snapshots; start/end scheduled exams; review answers, award manual marks,
  calculate grades and publish only after all submissions are graded.
- Student: register/log in, list ongoing exams, enter an eligible exam, answer
  MCQ/subjective/coding questions, submit once, receive confirmation and see
  only published grades. Blank subjective/coding responses receive zero;
  nonblank responses need a teacher mark. Missing MCQ keys also require review.
- Draft snapshots are **in memory for the open editor session**. Restoring a
  snapshot replaces stored draft questions in a transaction with new IDs; it
  does not restore submissions, grades or history. Draft question content and
  MCQ keys are restored with new IDs. Duplication also copies existing MCQ keys.

The UI calls services, services coordinate focused pattern objects and JDBC
repositories, and `ApplicationContext` constructs dependencies explicitly.
`src/patterns/` contains Builder (exam creation), Prototype (exam
duplication), Memento (draft history), Strategy (grading), Template Method
(grading sequence), Chain of Responsibility (submission checks), Mediator
(lifecycle), Observer (transactional audit and Swing refresh), Proxy (student
question access) and Bridge (console/Swing notifications). PostgreSQL's
`audit_log` retains lifecycle and submission events.

`docs/legacy/` preserves the former Spring-era notes and diagrams **as
historical references only**. They are not build or runtime instructions for
this application.

## Verification status

The tests above exercise domain and pattern behavior **without a database**.
The Swing screens and PostgreSQL CRUD have **not been verified end-to-end in
this environment** because no database credentials were supplied. Test with a
dedicated database before treating the application as ready for demonstration.
