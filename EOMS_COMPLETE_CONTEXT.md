# EOMS — complete project context

Current implementation reference for the Semester 5 Design Patterns Lab. **EOMS** means Online Examination Management System; **YDVRC** ("Your Digital Venue for Rigorous Certification") is a historical name found in `docs/legacy/`, not the current on-screen brand. Paths below are relative to the repository root. The Java files under `src/` and the root `sql/` are authoritative; `docs/legacy/` describes an earlier Spring implementation.

## 1. Project overview

EOMS is a local desktop examination application for teachers and students. Teachers compose and schedule exams, manage questions, conduct exams, review answers, grade and publish results. Students register or sign in, see currently available exams, answer questions and view only published scores. The intended lifecycle is `DRAFT → ONGOING → ENDED`, followed by grading and publication (`src/patterns/mediator/ExamLifecycleMediator.java`, `src/service/GradingService.java`). Each student can submit once per exam through the application.

**Presentation version:** “EOMS is a Java Swing examination system backed by PostgreSQL. Teachers prepare and run exams; students take them during a scheduled window; teachers grade and publish results. Ten design patterns address real steps of this workflow, such as controlled lifecycle transitions, reusable drafts, eligibility checks and grading. It is a single-JVM Design Patterns Lab project, not a hosted or proctored examination service.”

The app does not provide remote delivery, identity verification beyond local credentials, automatic coding assessment, or a teacher-facing exam deletion command. See sections 10 and 15.

## 2. Technology stack and architectural decisions

Dependencies: Java **17+** (records, modern `switch`, `Stream.toList()`), Swing/AWT and other JDK APIs, PostgreSQL (README targets version 16), and the checked-in JDBC driver `lib/postgresql-42.7.13.jar`. Source builds with `javac`; there is no current Maven build, Spring container, web client, REST server or ORM (`README.md`, `src/Main.java`, `src/database/DatabaseConnection.java`). The rationales below distinguish repository evidence from architectural interpretation; the old discussion in `docs/legacy/` is not proof of the current developers' motivations.

| Decision | Reason | Benefit | Trade-off |
| --- | --- | --- | --- |
| Java 17+ | **Observed:** README requires it; source uses Java records and modern syntax. **Inference:** one language keeps a lab project manageable. | Standard JDK toolchain and APIs. | Users need a compatible JDK/runtime. |
| Swing/AWT | **Observed:** all screens are `JFrame`/`JPanel` in `src/ui/`; README specifies a desktop app. **Inference:** a local demonstration needs no web stack. | One in-process user interface with no HTTP layer. | Manual layout/EDT discipline and no browser access. |
| PostgreSQL | **Observed:** `sql/schema.sql`, PostgreSQL JDBC URL, `RETURNING id`, row locks and advisory lock. **Inference:** relational records suit exam/question/submission links. | Persistent relational data and transaction support. | A separate running DB and administrator setup are required. |
| Plain JDBC, not ORM | **Observed:** repositories use `DriverManager`, `PreparedStatement`, `ResultSet`; no ORM dependency. **Inference:** explicit SQL makes lab persistence/transactions inspectable. | Fine control over queries and locks; few dependencies. | Mapping, connection ownership and SQL maintenance are manual. |
| Single JVM; no REST/web frontend | **Observed:** `Main` creates `ApplicationContext` and a Swing frame; UI calls services directly. **Inference:** no network API is needed for this local-client scope. | Smaller build and simpler call graph. | Not a multi-machine exam platform; a deployed desktop still needs DB connectivity. |
| Removed Maven/Spring Boot | **Observed:** current root has no `pom.xml`, controllers or Spring dependencies; `docs/legacy/` labels its Maven/REST content historical. **Inference:** removing a server/build framework reduces setup for the current application. | Direct `javac` build and visible wiring. | More manual build/package management. |
| UI/service/repository/model/pattern packages | **Observed:** actual `src/` boundaries and the calls described below. **Inference:** separate presentation, decisions, SQL and pattern mechanisms so they can change independently. | Easier tracing, testing and viva demonstration. | More classes and some cross-package coordination. |
| Explicit `ApplicationContext` | **Observed:** `src/config/ApplicationContext.java` constructs repositories, services, a clock, proxy, mediator, events and notifications. | Dependencies are visible; test constructor accepts `DatabaseConnection` and `Clock`. | Wiring changes are hand-maintained rather than injected by a framework. |

## 3. Complete project structure

The tree includes the current source and meaningful support files, not generated `out/` classes. `SKILL.md`, if present locally, is an untracked SwingTree *instruction document*, not a runtime dependency.

```text
README.md                         setup, workflows and verification caveats
.gitignore                        generated output and local secret exclusions
lib/postgresql-42.7.13.jar        JDBC driver used at runtime
sql/schema.sql                    baseline for a NEW database only
sql/migrations/
  001_credentials_and_grading.sql additive migration for an existing database
docs/legacy/                      archived Spring/REST discussion and UML, NOT current API
src/
  Main.java                       GUI entry point / EDT startup
  SetupAccounts.java              interactive account-enrollment CLI
  config/ApplicationContext.java  explicit dependency construction
  database/DatabaseConnection.java environment-based JDBC connections
  model/                           Exam, Question, Student, Teacher, Submission, Answer
  repository/                      Exam, Question, Student, Teacher, Submission, Answer repositories
  service/                         ExamService, DraftExamService, JdbcExamService,
                                   QuestionService, StudentService, TeacherService,
                                   SubmissionService, GradingService, PasswordHashes,
                                   StudentSession, TeacherSession
  patterns/
    builder/ExamBuilder.java
    prototype/ExamPrototype.java
    memento/{ExamDraft,DraftHistory}.java
    strategy/{GradingStrategy,McqStrategy,ManualStrategy}.java
    templatemethod/GradingWorkflow.java
    mediator/ExamLifecycleMediator.java
    observer/{ExamEvent,ExamEventListener,ExamEventPublisher,
              AuditLogObserver,SwingRefreshObserver}.java
    proxy/{QuestionBank,QuestionBankProxy,RealQuestionBank}.java
    chainofresponsibility/{SubmissionContext,SubmissionHandler,
                           AuthCheckHandler,ExamWindowHandler,DuplicateSubmissionHandler}.java
    bridge/{Notification,NotificationChannel,ImmediateNotification,
            DesktopChannel,ConsoleChannel,SwingChannel}.java
  ui/                              LoginFrame, TeacherDashboard, StudentDashboard,
                                   ExamEditorPanel, ExamAttemptPanel, ExamDateTimePicker,
                                   AppTheme, AppButton, UiTasks
tests/com/eoms/                    four offline main-method tests plus opt-in JdbcIntegrationTest
```

`Main.main` constructs the context, selects a cross-platform Look and Feel and opens login on the Swing EDT. `SetupAccounts.main` is a separate interactive provisioning tool, not an application startup hook (`src/Main.java`, `src/SetupAccounts.java`). `ExamService` is an interface implemented by `JdbcExamService`; `DraftExamService` builds in-memory drafts.

## 4. Application architecture

```text
 Main ──> ApplicationContext ──> LoginFrame / dashboards / editors
                       │                      │
                       │                 UiTasks (SwingWorker)
                       │                      ▼
                       ├── ExamService, QuestionService, StudentService,
                       │   TeacherService, SubmissionService, GradingService
                       │                      │
                       ├── builder / prototype / mediator / memento / proxy /
                       │   chain / strategies / grading template / observers / bridge
                       │                      │
                       └──────── repositories + DatabaseConnection ──JDBC──> PostgreSQL
```

Screens collect input and schedule service/JDBC calls through `UiTasks.run`, which runs `Callable` work in `SwingWorker.doInBackground` and delivers success/error on the EDT (`src/ui/UiTasks.java`). Services validate identity/state and orchestrate pattern objects; repositories own SQL rather than presentation. `ApplicationContext` is constructed once by `Main` and passed to screens; its explicit constructor also supports a dedicated test DB and deterministic clock. Model classes are simple mutable Java objects; `Answer` does not itself expose `awarded_marks`, although the SQL table and grading query do (`src/model/Answer.java`, `src/service/GradingService.java`).

`TeacherSession`/`StudentSession` have package-private constructors, are returned by login, and carry IDs. Teacher-only service methods check for a non-null `TeacherSession`; they do not implement per-exam teacher ownership. A student's questions go through `QuestionBankProxy`. These are application-level boundaries, not a separate security server (`src/service/*Session.java`, `src/service/QuestionService.java`). Keeping responsibilities separate lets offline tests exercise the builder/strategy/etc. without a database, but integration and UI behavior still need separate verification.

## 5. Database architecture

`sql/schema.sql` defines eight tables:

| Table | Important columns and relationships |
| --- | --- |
| `teachers` | `id` PK; unique `username`; nullable `password_hash`. |
| `students` | `id` PK; `name`, unique `email`; nullable `password_hash`. |
| `exams` | `id` PK; title, duration, start/end timestamps, `status` (default `DRAFT`), `grades_published` (default false). |
| `questions` | `id` PK; required `exam_id → exams.id`; type, text, marks, comma-separated options. |
| `question_keys` | `question_id` PK/FK → `questions.id` with `ON DELETE CASCADE`; MCQ correct answer stored separately. |
| `submissions` | `id` PK; required `exam_id → exams.id`, `student_id → students.id`; submission time, status, nullable total score. |
| `answers` | `id` PK; required `submission_id → submissions.id`, `question_id → questions.id`; answer text, nullable `awarded_marks`. |
| `audit_log` | `id` PK; event type, timestamp, details, exam/student ID values **without declared foreign keys**. |

Separating answers from submissions supports one record per question (including unanswered questions); a nullable mark/score distinguishes “needs review” from zero. Separating `question_keys` avoids returning an MCQ key in normal student question reads. Those are explanations inferred from the schema and its service usage, not extra DB constraints. The SQL does **not** declare unique `(exam_id, student_id)`, CHECKs for statuses/marks/time, cascading deletes except for `question_keys`, or a teacher-to-exam ownership FK. App code supplies many of those rules; manual SQL can bypass them (`src/service/SubmissionService.java`, `src/patterns/mediator/ExamLifecycleMediator.java`).

`DatabaseConnection()` captures `EOMS_DB_URL`, `EOMS_DB_USER`, `EOMS_DB_PASSWORD` from the Java process environment; `open()` validates the captured settings/JDBC URL prefix, then calls `DriverManager.getConnection`. Set the variables **before** launching Java; no DB is opened just by constructing the context. Most repositories use try-with-resources and parameterized `PreparedStatement`s; generated IDs use `INSERT ... RETURNING id` (`src/database/DatabaseConnection.java`, `src/repository/`). Several operations intentionally share a `Connection` inside an explicit transaction with commit/rollback: first-teacher bootstrap, exam duplication/restoration, lifecycle start/end with audit insertion, submission with answers/audit, and grade publication with audit. Other calls (individual question/mark updates, and grading's validate/load/update stages) use separate connections/autocommit; do **not** describe the entire grading flow as one atomic transaction (`src/service/GradingService.java`).

Schema setup is manual: use `sql/schema.sql` only for a **new** database. For an existing legacy DB, back it up, inspect it and apply `sql/migrations/001_credentials_and_grading.sql`; its `ADD COLUMN IF NOT EXISTS` operations and `question_keys` creation preserve existing rows. The baseline's `CREATE TABLE IF NOT EXISTS` is **not** a migration for pre-existing columns. Existing teacher/student password hashes may be null until deliberately enrolled (`README.md`, `src/SetupAccounts.java`).

## 6. Complete teacher workflow

Each row traces **action → UI → service → pattern (where used) → repository/database or result**. Teacher dashboard actions are shown contextually for selected exam status; the service/database remain the source of validation (`src/ui/TeacherDashboard.java`).

| Teacher action | Actual call chain and outcome |
| --- | --- |
| Sign in | `LoginFrame` sign-in → `TeacherService.login` → `TeacherRepository.credentials` (`teachers.password_hash`) → `PasswordHashes.matches` → `TeacherSession`; `TeacherDashboard` opens. |
| Create | “New examination” → `TeacherDashboard.examForm` (title, duration, `ExamDateTimePicker` calendar/24-hour spinner) → `ExamService.prepareDraft` / `DraftExamService` → `ExamBuilder.build` → `JdbcExamService.create` revalidates and `ExamRepository.save` inserts a `DRAFT`. |
| Configure draft | Select `DRAFT` → Configure → `examForm` prefilled from exam → `prepareDraft` → `JdbcExamService.configureDraft` → `ExamRepository.updateDraft`, SQL restricted to `status = 'DRAFT'`. |
| Edit questions and key | Edit questions → `ExamEditorPanel` → `QuestionService.listForEditor`/`add`/`update`/`remove`, which require teacher and draft → `QuestionRepository`; optional MCQ key via `GradingService.setMcqKey` to `question_keys`, restricted to draft MCQs. Editor list never displays stored key; editor refresh retrieves keys for snapshot alignment. Add/update followed by setting a key use separate JDBC operations, not one atomic transaction. |
| Duplicate | Duplicate → `JdbcExamService.duplicate` → `ExamPrototype.duplicate` resets exam/question IDs/status; transaction saves copied exam and questions through `ExamRepository`/`QuestionRepository`, and copies stored MCQ keys with `QuestionRepository.copyKey`. No submissions/grades are copied. |
| Save snapshot | Editor “Save snapshot” → `DraftHistory.save(name, draft)` → `ExamDraft.capture` / detached `Snapshot` (also uses `ExamPrototype` for copying). **No JDBC write**; history belongs to the current editor instance. |
| Restore snapshot | Editor “Restore snapshot” → `DraftHistory.restore(name, draft)` changes detached editor draft → `UiTasks.run` → `JdbcExamService.restoreDraft` → `ExamBuilder` validation, `ExamPrototype` detachment, row lock, `ExamRepository.updateDraft`, `QuestionRepository.deleteForDraft`/`save`/`saveKey` in one DB transaction. Questions/keys get new IDs; after success editor refreshes. Existing submissions/history are not restored. |
| Start/end | Start or End → `TeacherDashboard.changeStatus` → `JdbcExamService.start/end` → `ExamLifecycleMediator.start/end` → row lock/conditional `ExamRepository.transition`, transactional `AuditLogObserver` insert into `audit_log`, commit, then `SwingRefreshObserver` and `ImmediateNotification`/`DesktopChannel`. Start requires scheduled window; end requires `ONGOING`. |
| Review answers | “Review submissions” → `TeacherDashboard.showReview` → `SubmissionService.listForReview` → `SubmissionRepository.findByExamId`; Load answers → `SubmissionService.reviewAnswers` → `AnswerRepository.findBySubmissionId`; double-click selected answer to see full text. |
| Manual marking | “Mark selected answer” (or enter answer ID) → `GradingService.recordManualMark` → guarded SQL on `answers.awarded_marks`: subjective/coding only, unpublished exam, finite mark between zero and question maximum. Recalculate grade afterwards. |
| Grade calculation | “Calculate grade” → `GradingService.gradeForTeacher` → `GradingWorkflow.grade` template → load answers/keys/manual marks → per-type `McqStrategy` or `ManualStrategy` → update `submissions.score/status` (`NEEDS_REVIEW` or `GRADED`). Missing MCQ key or nonblank manual answer without a mark keeps score null/review pending. The grade service checks *unpublished*, not `ENDED`; only publishing requires an ended exam. |
| Publish | “Publish grades” → `GradingService.publish` → conditional `exams.grades_published = TRUE` only when `ENDED` and **no submission is ungraded**; transactional `EXAM_PUBLISHED` audit event, then post-commit notification/refresh. No separate grade table. An ended exam with zero submissions satisfies this SQL condition. |

Reason for this organization: UI gathers/display choices; services guard state and role; pattern objects isolate copying, versioning, transition coordination, evaluation, and event delivery; repositories execute persistent operations. The UI's contextual visibility is a usability aid rather than an authorization guarantee.

## 7. Complete student workflow

1. **Enroll/login:** `LoginFrame` student registration → `StudentService.register` → `PasswordHashes.hash` → `StudentRepository.save` (`students`). Existing rows need `SetupAccounts --student ID` after identity verification. Student login parses numeric ID and calls `StudentService.login` → stored hash comparison → `StudentSession` (`src/ui/LoginFrame.java`).
2. **Find/enter:** `StudentDashboard.refresh` calls `ExamService.findAll` (`ExamRepository`) in `UiTasks`; on the returned list it filters `ONGOING` and current start/end timestamps. “Enter examination” opens `ExamAttemptPanel`. This dashboard filter uses `LocalDateTime.now()` rather than the injectable service clock and does **not** filter exams already submitted by this student; the proxy rejects re-entry. The dashboard list is *not* the security boundary (`src/ui/StudentDashboard.java`).
3. **Load:** `ExamAttemptPanel.load` → `QuestionService.listForStudent` → `QuestionBankProxy.forStudent`: non-null session, ongoing exam within scheduled window, existing student and no prior submission; only then `RealQuestionBank` → `QuestionRepository.findByExamId`. The question query does not select `question_keys` (`src/patterns/proxy/`).
4. **Answer:** The scrollable paper renders all questions. MCQ uses a button group of radio choices derived from comma-separated options; subjective/coding use text areas (coding is monospaced); other types fall back to a text field. A map of `questionId → Supplier<String>` reads responses when submitting; progress counts nonblank inputs. Timer displays approximate remaining time; it is informational, not the server-side eligibility check (`src/ui/ExamAttemptPanel.java`).
5. **Submit:** Explicit final confirmation → `SubmissionService.submit` on a `SwingWorker`. Inside one transaction, it locks the exam row, runs `AuthCheckHandler → ExamWindowHandler → DuplicateSubmissionHandler`, rejects question IDs outside that exam, inserts a `SUBMITTED` submission, stores an answer row for **every** exam question (empty for skipped), writes an audit event, commits, then invokes post-commit observers and notification. Locking serializes submissions made through this service for the same exam before its duplicate lookup (`src/service/SubmissionService.java`).
6. **Results:** “Published grades” → `StudentService.publishedGrades` → `SubmissionRepository.findPublishedForStudent`: filters by that student ID and `exams.grades_published = TRUE`. UI displays exam ID and score; it does not display correct answers (`src/ui/StudentDashboard.java`).

No evidence of a persistent autosave or resumable attempt exists. Closing an unsubmitted attempt loses entered answers. A prior submit is blocked again by the proxy and submission chain; SQL itself does not have a unique student/exam pair constraint.

## 8. All implemented design patterns

All ten below have concrete implementations and callers in `src/`. Categories use common GoF terminology; application roles and limitations matter more than generic definitions.

| Pattern (category) | Actual problem, selection and implementation | Execution, benefit and trade-off |
| --- | --- | --- |
| **Builder** (creational) | Valid exam drafts need title, positive duration, computed/explicit times and default `DRAFT`. `patterns/builder/ExamBuilder` is called by `DraftExamService`, and revalidation in `JdbcExamService`. | Fluent setters → `build()` checks invariants and produces unsaved `Exam`. Centralizes creation rules; one extra class and no DB constraints mirroring all those rules. |
| **Prototype** (creational) | Duplicate an exam/question paper without copying identities/results. `patterns/prototype/ExamPrototype.duplicate` is called by `JdbcExamService.duplicate` and Memento's copy logic. | Makes `Copy(exam, questions)` with null IDs and `DRAFT`; service persists it and copies keys in a transaction. Saves manual reconstruction; copied schedule may need configuration and snapshot-specific title is corrected separately by `ExamDraft`. |
| **Strategy** (behavioral) | MCQs are evaluated against keys, text/code require manual marks. `patterns/strategy/GradingStrategy`, `McqStrategy`, `ManualStrategy`; selected by `GradingService` map. | `evaluate` returns marks or null (“needs review”). Isolates per-type algorithm; type strings/map and key quality still need managing. It does **not** automatically grade code. |
| **Template Method** (behavioral) | The grading sequence must remain validate → load → evaluate → sum → persist. `patterns/templatemethod/GradingWorkflow.grade` is the final skeleton overridden by `GradingService`. | Prevents a caller skipping a stage; subclass must implement all steps. Unlike Strategy, this fixes the *overall order*, not the per-question scoring rule. Its stages use multiple connections, not one atomic transaction. |
| **Mediator** (behavioral) | Lifecycle changes must coordinate exam status, audit, refresh and notification without dashboard-to-everything coupling. `patterns/mediator/ExamLifecycleMediator` receives DB/repository/publisher/notification/clock from `ApplicationContext`; `JdbcExamService.start/end` delegates. | `change` locks/checks/transitions and audits transactionally; after commit it triggers UI observer and notification. Central coordination keeps lifecycle rules together; mediator itself knows several collaborators and teacher check remains at the service. |
| **Memento** (behavioral) | Return a draft to a named earlier content state without exposing/mutating a saved snapshot. `patterns/memento/ExamDraft` (originator), nested `Snapshot`, `DraftHistory` (caretaker), `ExamEditorPanel`. | `capture` copies exam/questions/keys; `restore` replaces detached editor state; `JdbcExamService.restoreDraft` then explicitly replaces DB draft questions/keys transactionally. In-memory names disappear when editor closes; restoring DB changes IDs. |
| **Observer** (behavioral) | Multiple effects follow a lifecycle/submission/publication event. `patterns/observer/ExamEventPublisher`, `ExamEventListener`, `AuditLogObserver`, `SwingRefreshObserver`. | `publish(event, connection)` writes audit via shared transaction; `afterCommit` schedules dashboard refresh on EDT. Loose coupling for event reactions, but post-commit callback/notification errors cannot undo the already committed DB change. Unlike Mediator, Observer broadcasts *what happened* rather than deciding whether a transition may occur. |
| **Proxy** (structural) | Students must not see exam questions early or after submitting. `patterns/proxy/QuestionBank`, `QuestionBankProxy`, `RealQuestionBank`, injected into `QuestionService`. | Proxy checks session, exam status/window, student row and existing submission before delegate queries questions. Consistent access gate for this path; checks occur before a separate delegate connection, so no all-encompassing atomic read. |
| **Chain of Responsibility** (behavioral) | Submission requires ordered, independently explainable checks. `patterns/chainofresponsibility/SubmissionHandler`, `SubmissionContext`, `AuthCheckHandler`, `ExamWindowHandler`, `DuplicateSubmissionHandler` assembled by `SubmissionService`. | Each `check` either rejects or forwards. Clear validation stages, but order matters and a chain alone does not create a database uniqueness constraint. |
| **Bridge** (structural) | Notifications should not hardwire lifecycle/submission logic to a specific display. `patterns/bridge/Notification`, `ImmediateNotification` and `NotificationChannel` implemented by `DesktopChannel`, `ConsoleChannel`, `SwingChannel`. | `send(recipient,message)` delegates to channel; desktop uses Swing when a listener is installed, otherwise console. Can swap delivery without changing service calls; it is transient UI/console delivery, not durable messaging or recipient-specific inbox routing. |

## 9. Swing UI architecture

`src/Main.java` schedules the login frame on the EDT. `src/ui/AppTheme.java` centralizes EOMS's near-black colors, fonts, panels, inputs and renderers; `AppButton.java` paints button hierarchy/focus/hover; `Main` selects the JDK cross-platform Look and Feel. This is **vanilla Swing**, not SwingTree: an untracked `SKILL.md` mentioning SwingTree is not imported or installed in source.

- `LoginFrame`: role tabs on a focused sign-in panel; teacher username/password or student numeric ID/password; student registration dialog. On success opens a role dashboard and disposes the login frame.
- `TeacherDashboard`: list of exams with status badges; selected-status-dependent controls for configure/edit/duplicate/start/end/review/publish, plus new-exam and refresh. `examForm` uses `ExamDateTimePicker`'s calendar popup and 24-hour time spinner. `showReview` creates a split-pane submissions/answers window; selecting an answer can display full text, record a manual mark, or recalculate the selected submission's grade.
- `StudentDashboard`: filtered available-exam list, enter (also double-click), refresh and secondary published-grade dialog.
- `ExamEditorPanel`: writing area, existing questions, and separate named session snapshot controls. The normal question list excludes MCQ keys even though the teacher-only refresh also loads keys into the private draft for snapshotting.
- `ExamAttemptPanel`: one scrollable paper with all questions rather than one-at-a-time navigation; answer controls, progress, remaining-time label and final confirmation.
- `UiTasks`: `SwingWorker` keeps login/registration, JDBC reads/writes and related service calls off the EDT; `done()` displays errors or updates Swing on the EDT. `SwingRefreshObserver`/`SwingChannel` also marshal callbacks with `SwingUtilities.invokeLater`. `UiTasks.show` uses `JOptionPane` and translates the two invalid-credentials strings to friendlier wording while showing other backend messages. The calendar and confirmation dialogs run on Swing's UI thread, not JDBC workers.

Reason: Swing widgets must be manipulated on the EDT; blocking it for database calls freezes the interface. `AppTheme`/`AppButton` reduce inconsistent screen-by-screen styling; no external UI framework is required (`src/ui/`, `src/patterns/observer/SwingRefreshObserver.java`).

## 10. Security, validation and reliability

- **Credentials:** `PasswordHashes` uses JDK PBKDF2-HMAC-SHA256, a random 16-byte salt, 210,000 iterations and a constant-time derived-hash comparison; new passwords must be at least eight characters. `SetupAccounts` prompts in an interactive console, wipes its `char[]` after hashing, and either bootstraps the first teacher only when none exist or updates a verified existing teacher/student. `LoginFrame` clears password widgets, but passing `String` passwords still places immutable secret copies in memory (`src/service/PasswordHashes.java`, `src/SetupAccounts.java`).
- **Roles:** Non-null sessions are checked by teacher services; students receive sessions only after password comparison. Proxy guards question reads; chain guards submissions. No per-teacher exam assignment, network isolation or external identity proof is implemented. Anyone with DB administrative access or direct repository access can bypass service-level checks.
- **Time and duplicates:** Mediator validates status and start window; proxy and `ExamWindowHandler` validate ongoing window; submission locks exam row before duplicate check/inserts. Dashboard/time display uses UI clock; final eligibility uses service `Clock`. DB lacks CHECK/unique pair constraints and direct SQL could create invalid states.
- **Answer confidentiality:** Student question read excludes `question_keys`. Teacher draft APIs can retrieve keys for snapshots; ordinary editor list clears key field on selection. Published student result query returns scores, not answer keys.
- **SQL/transactions:** Query parameters are bound in prepared statements; try-with-resources closes most JDBC resources. Explicit rollback on errors is present for certain multi-write flows described in section 5; not every sequence, notably grading and add-question-plus-key, is atomic. Post-commit display failures cannot roll back a commit.
- **Configuration/error handling:** No source-embedded DB password; `EOMS_DB_*` must be set in the process running Java (an IDE needs its own run configuration). `UiTasks` surfaces exception messages; it does not provide retry/recovery or structured error logs. README warns an old DB password existed in Git history and should be rotated if real. A direct desktop client with DB credentials is a **lab application**, not hardened multi-user production security.

## 11. Testing and verification

Tests are Java `main` methods with `assert`; always run with `-ea` (`tests/com/eoms/`, `README.md`). They are not JUnit and require no Maven.

| Program | Actual checks |
| --- | --- |
| `CoreDomainTest` | Builder defaults/validation, missing DB environment failure, some input checks, context construction without DB/GUI. |
| `CreationalPatternsTest` | Builder defaults; Prototype detached IDs/content; named Memento restoration of exam, questions and aligned key, including repeat restore. |
| `GradingPatternsTest` | MCQ/manual strategies (including pending review), password hashing, Template Method stage order, ended-exam window rejection. |
| `CoordinationPatternsTest` | Proxy rejects null session without JDBC; post-commit Observer refresh and Bridge Swing delivery run on EDT. |
| `JdbcIntegrationTest` | **Opt-in** dedicated `eoms_test` DB only: login/register, create/questions/keys, duplicate, restore, proxy access, submission/duplicate denial, end, grade, publish and visible result. It writes and retains rows; needs test DB credentials and a teacher. It is *not* a GUI test. |

**Verification evidence for this documentation task:** `javac` compiled `src/` and `tests/`, and the four offline programs above completed with their success messages using `java -ea`. The live `JdbcIntegrationTest` was **not run** here; no test DB credentials were supplied. Current GUI flows, packaging, and production database state were **not verified end-to-end** here. Legacy transcript reports old Spring runs, which do not prove the present Swing application works live.

## 12. Build, database setup and execution (Windows PowerShell)

Run commands from repository root with JDK 17+, PostgreSQL and `psql` available. The checked-in JDBC driver is included only at runtime; compilation uses JDK APIs.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src -Recurse -Filter *.java).FullName
javac -cp out -d out (Get-ChildItem tests -Recurse -Filter *.java).FullName
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CoreDomainTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CreationalPatternsTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.GradingPatternsTest
java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.CoordinationPatternsTest
```

**First check whether the DB already exists** (`psql -U postgres -l`). For an entirely **new** DB only:

```powershell
psql -U postgres -d postgres -c "CREATE DATABASE eoms_db;"
psql -v ON_ERROR_STOP=1 -U postgres -d eoms_db -f sql/schema.sql
```

For an **existing legacy** `eoms_db`, do **not** recreate it or apply the baseline as an upgrade: back it up, inspect existing tables/columns, then apply the additive migration:

```powershell
pg_dump -U postgres -d eoms_db -Fc -f "eoms_db_backup.dump"
psql -v ON_ERROR_STOP=1 -U postgres -d eoms_db -f sql/migrations/001_credentials_and_grading.sql
```

In the **same PowerShell session** that runs Java, configure DB credentials privately (example host/database/user; supply your own DB password at the prompt):

```powershell
$env:EOMS_DB_URL = 'jdbc:postgresql://localhost:5432/eoms_db'
$env:EOMS_DB_USER = 'postgres'
$env:EOMS_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host -AsSecureString 'Database password')).Password
```

First teacher **only when `teachers` is empty**: `java -cp "out;lib/postgresql-42.7.13.jar" SetupAccounts`. For an existing teacher, verify their username first, then `java -cp "out;lib/postgresql-42.7.13.jar" SetupAccounts --teacher "EXISTING USERNAME"`; for an existing student, verify ID/identity, then `java -cp "out;lib/postgresql-42.7.13.jar" SetupAccounts --student 123`. CLI password prompts are for *account login*, distinct from the DB password. New students may register in the GUI. Launch with `java -cp "out;lib/postgresql-42.7.13.jar" Main`. Environment variables set in one terminal do not automatically reach an already-running IDE/another shell (`src/database/DatabaseConnection.java`).

For the **opt-in integration test**, make a separate `eoms_test` DB with `sql/schema.sql`, enroll its own teacher, set `EOMS_TEST_DB_URL`, `EOMS_TEST_DB_USER`, `EOMS_TEST_DB_PASSWORD`, `EOMS_TEST_TEACHER` and `EOMS_TEST_TEACHER_PASSWORD` for *that DB*, then run `java -ea -cp "out;lib/postgresql-42.7.13.jar" com.eoms.JdbcIntegrationTest`. It rejects URLs without `eoms_test` but still writes retained rows; never point it at production (`tests/com/eoms/JdbcIntegrationTest.java`). No `jpackage` script or supported packaging instructions are present in the repository; an `.exe` is a possible future deliverable, not a verified build path here.

## 13. Complete execution walkthrough

**Prerequisites:** correct schema/migration, DB environment variables in the launched process, enrolled teacher and student logins, and a schedule permitting the teacher to start before the end time. This is a **trace of implemented calls**, not a claim that this precise GUI scenario was executed in this environment.

1. `Main` → `ApplicationContext` → `LoginFrame`; teacher enters username/password → `TeacherService.login` returns `TeacherSession` → `TeacherDashboard`.
2. Teacher clicks New examination, chooses start date in `ExamDateTimePicker` and time with spinner; `TeacherDashboard.examForm` → `DraftExamService.prepareDraft` / `ExamBuilder` → `JdbcExamService.create` → `ExamRepository.save`. Result is a `DRAFT`.
3. Teacher selects that draft → Edit questions → `ExamEditorPanel` → `QuestionService.add` for an MCQ and perhaps a subjective question; optionally `GradingService.setMcqKey` for the MCQ. Stored keys do not appear in the general list.
4. Editor refreshes question content and aligned teacher-only keys into `ExamDraft`. Teacher clicks Save snapshot, names it `before-edit` → `DraftHistory.save` retains a detached `Snapshot` **in memory**. After changing saved draft questions, teacher clicks Restore snapshot, enters the name → `DraftHistory.restore` → `JdbcExamService.restoreDraft`; questions and keys are replaced transactionally with *new IDs*. Closing the editor loses snapshot history.
5. During the schedule window, teacher clicks Start exam → `JdbcExamService.start` → `ExamLifecycleMediator.start`; status/audit commit, then refresh/notification. Merely reaching the scheduled time does not automatically transition `DRAFT` to `ONGOING`.
6. Student signs in → `StudentService.login` → `StudentDashboard.refresh` filters the available list → Enter examination → `ExamAttemptPanel.load` → `QuestionService.listForStudent` → `QuestionBankProxy` → `QuestionRepository`. Student selects an MCQ option and enters text as needed.
7. Student clicks Submit examination and confirms → `SubmissionService.submit`: chain checks, locked exam, saved submission/one answer per question/audit transaction. The dashboard cannot subsequently use the proxy to reopen the submitted paper.
8. Teacher clicks End exam (`ExamLifecycleMediator.end`), opens Review submissions (`SubmissionService.listForReview`), loads answers (`reviewAnswers`), optionally marks nonblank subjective/coding answer (`GradingService.recordManualMark`), then clicks Calculate grade (`GradingWorkflow` + strategies). A missing MCQ key or nonblank unmarked manual answer yields `NEEDS_REVIEW` until addressed; an MCQ key cannot be newly set after exam leaves `DRAFT` through the normal API.
9. Once exam is `ENDED` and all submissions are `GRADED`, Publish grades → `GradingService.publish` sets `grades_published`; student clicks Published grades → `StudentService.publishedGrades` and sees their score. No publication occurs automatically when grading finishes.

## 14. Why we designed EOMS this way (viva rationale)

The academic objective is to explain *when* a pattern solves a real problem, not to list names. Builder protects valid draft creation; Prototype and Memento support reusable papers and reversible editor content; Mediator owns lifecycle coordination while Observer fans out effects; Chain and Proxy restrict entry to the examination; Strategy chooses a scoring rule inside the Template Method's fixed grading sequence; Bridge separates notification intent from desktop/console delivery. Each has a calling path from actual application actions (`src/config/ApplicationContext.java`, `src/ui/`, `src/service/`, `src/patterns/`).

Separation keeps Swing code from constructing SQL and keeps repositories from deciding grading or roles. Plain JDBC/explicit wiring make dependencies, resource use and transaction boundaries visible during a viva, while a single JVM and no web stack reduce scope. The costs are more manual wiring, more classes than a minimal CRUD app, explicit SQL mapping and local database administration. Explain trade-offs honestly: transaction-backed workflows exist, but not every multi-step operation is atomic; local sessions/credentials demonstrate roles without production-grade identity management. A future iteration could add schema invariants, better integration/UI coverage and packaged deployment without pretending those features exist now.

## 15. Known limitations and future improvements

**Confirmed limitations:** no committed executable/installer instructions or packaging script; PostgreSQL and process environment setup remain manual; no exam-delete UI; no persistent autosave/resume; snapshots are scoped to one editor window; subjective/coding content is manually marked; missing MCQ key blocks grading even after an exam starts, while the normal key setter only accepts draft MCQs (`GradingService`); an already submitted exam can still appear on the student list until its window ends; login sessions carry only IDs with no ownership model; direct DB access can bypass service validation; schema has no unique `(exam_id, student_id)` or CHECK constraints; grading stages and question-plus-key saves are not atomic; UI countdown and dashboard filtering use local UI time; no automated full GUI test and no live integration test run reported in this document. `README.md`'s warning that the old DB password was committed in history still applies if it was real.

**Proposals, not implemented features:** add DB constraints/unique submission pair and ownership rules; make question-plus-key and grade updates more consistently transactional; consider a safe admin cleanup workflow, resumable draft answers, richer reviewer context, test-DB integration coverage and GUI automation; provide a documented `jpackage` build with external credential handling rather than embedding DB secrets. Revisit exam scheduling/clock consistency for multiple machines or time zones before treating it as a remote examination service.
