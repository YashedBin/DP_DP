# EOMS — Design Patterns UML Context

> Focused on the **6 GoF Design Patterns** used in the project.
> All class names and relationships are from the actual codebase.
> PlantUML code — paste into [plantuml.com](https://www.plantuml.com/plantuml/uml/) to render.

---

## Pattern 1 — Singleton (Creational)

### Identified In
`ExamClock` — single time-source instance for the entire application.

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `ExamClock` | **Singleton** — private constructor, static `getInstance()`, lazy init with `synchronized` |
| `SubmissionService` | Client — calls `ExamClock.getInstance().now()` for timestamps |
| `TimeWindowCheckHandler` | Client — calls `ExamClock.getInstance().now()` for time validation |

### How They Interact
`SubmissionService` and `TimeWindowCheckHandler` both call `ExamClock.getInstance()` to get the single shared clock. No direct instantiation — always through the static factory method. Thread-safe via `synchronized`.

### PlantUML

```plantuml
@startuml Singleton_ExamClock
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Singleton Pattern" #F5F5DC {
    class ExamClock <<Singleton>> {
        - {static} instance : ExamClock
        - ExamClock()
        + {static} getInstance() : ExamClock
        + now() : LocalDateTime
    }
}

class SubmissionService <<Client>> {
    + submit(examId, studentId) : Submission
}

class TimeWindowCheckHandler <<Client>> {
    # check(context : SubmissionContext)
}

SubmissionService ..> ExamClock : "getInstance().now()"
TimeWindowCheckHandler ..> ExamClock : "getInstance().now()"

note bottom of ExamClock
  Lazy initialization
  Thread-safe (synchronized)
end note
@enduml
```

---

## Pattern 2 — Observer (Behavioral)

### Identified In
Exam event system — decouples lifecycle events (exam started, submission recorded) from handlers (audit logging).

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `ExamEventPublisher` | **Subject** — maintains list of listeners, calls `onExamEvent()` on each |
| `ExamEventListener` | **Observer** (interface) — defines `onExamEvent(ExamEvent)` |
| `AuditLogObserver` | **ConcreteObserver** — writes event data to `audit_log` table via `JdbcTemplate` |
| `ExamEvent` | **Event Data** — carries type, examId, studentId, timestamp, details |
| `ExamService` | Event Producer — publishes `EXAM_STARTED` |
| `SubmissionService` | Event Producer — publishes `EXAM_SUBMITTED` |

### How They Interact
`ExamService` and `SubmissionService` create an `ExamEvent` and call `ExamEventPublisher.publish()`. The publisher iterates over all `ExamEventListener` beans (auto-collected by Spring DI as `List<ExamEventListener>`) and calls `onExamEvent()` on each. `AuditLogObserver` receives the event and writes an audit row to the database.

### PlantUML

```plantuml
@startuml Observer_ExamEvent
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Observer Pattern" #E8F5E9 {
    class ExamEventPublisher <<Subject>> {
        - listeners : List<ExamEventListener>
        + publish(event : ExamEvent)
    }

    interface ExamEventListener <<Observer>> {
        + onExamEvent(event : ExamEvent)
    }

    class AuditLogObserver <<ConcreteObserver>> {
        - jdbcTemplate : JdbcTemplate
        + onExamEvent(event : ExamEvent)
    }

    class ExamEvent <<EventData>> {
        + type : Type
        + examId : Long
        + studentId : Long
        + occurredAt : LocalDateTime
        + details : String
    }

    enum "ExamEvent.Type" as EventType {
        EXAM_STARTED
        EXAM_SUBMITTED
        EXAM_GRADED
        EXAM_PUBLISHED
    }
}

class ExamService <<Producer>> {
    + startExam(id)
}

class SubmissionService <<Producer>> {
    + submit(examId, studentId)
}

ExamEventPublisher o-- "0..*" ExamEventListener : "listeners"
AuditLogObserver ..|> ExamEventListener
ExamEventPublisher ..> ExamEvent : "publishes"
AuditLogObserver ..> ExamEvent : "receives"
ExamEvent --> EventType

ExamService ..> ExamEventPublisher : "publish(EXAM_STARTED)"
SubmissionService ..> ExamEventPublisher : "publish(EXAM_SUBMITTED)"

note bottom of AuditLogObserver
  INSERT INTO audit_log
  (event_type, exam_id, ...)
end note
@enduml
```

---

## Pattern 3 — Chain of Responsibility (Behavioral)

### Identified In
Submission validation — a chain of 3 handlers validates a submission request sequentially. Any handler can reject.

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `SubmissionHandler` | **Handler** (abstract) — holds `next` reference, template `handle()` calls `check()` then delegates |
| `AuthCheckHandler` | **ConcreteHandler 1** — verifies exam status is `ONGOING` |
| `TimeWindowCheckHandler` | **ConcreteHandler 2** — verifies current time is within exam window (uses `ExamClock`) |
| `AlreadySubmittedHandler` | **ConcreteHandler 3** — checks if student already submitted (queries DB) |
| `SubmissionContext` | **Request Object** — carries `Exam`, `studentId`, rejection state |
| `SubmissionService` | Client — builds chain, initiates `handle()` |

### How They Interact
`SubmissionService` constructor wires the chain: `AuthCheck → TimeWindow → AlreadySubmitted`. On `submit()`, it creates a `SubmissionContext` and calls `authCheckHandler.handle(context)`. Each handler runs its `check()` — if it doesn't reject, it passes to `next.handle()`. If any handler calls `context.reject(reason)`, the chain stops and `SubmissionService` throws an exception.

### PlantUML

```plantuml
@startuml COR_Submission
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Chain of Responsibility Pattern" #FFF3E0 {
    abstract class SubmissionHandler <<Handler>> {
        - next : SubmissionHandler
        + setNext(next) : SubmissionHandler
        + handle(context : SubmissionContext)
        # {abstract} check(context : SubmissionContext)
    }

    class AuthCheckHandler <<ConcreteHandler>> {
        # check(context)
    }

    class TimeWindowCheckHandler <<ConcreteHandler>> {
        # check(context)
    }

    class AlreadySubmittedHandler <<ConcreteHandler>> {
        - submissionRepository
        # check(context)
    }

    class SubmissionContext <<Request>> {
        - exam : Exam
        - studentId : Long
        - rejected : boolean
        - rejectionReason : String
        + reject(reason : String)
        + isRejected() : boolean
    }
}

class SubmissionService <<Client>> {
    + submit(examId, studentId)
}

SubmissionHandler <|-- AuthCheckHandler
SubmissionHandler <|-- TimeWindowCheckHandler
SubmissionHandler <|-- AlreadySubmittedHandler
SubmissionHandler o--> SubmissionHandler : "next"
SubmissionHandler ..> SubmissionContext : "processes"

SubmissionService ..> AuthCheckHandler : "initiates chain"

note right of SubmissionService
  Chain wired in constructor:
  Auth → TimeWindow → AlreadySubmitted
end note

note bottom of SubmissionContext
  Carries Exam + studentId
  Any handler can call reject()
  Chain stops on rejection
end note
@enduml
```

---

## Pattern 4 — Factory Method (Creational)

### Identified In
Question creation — different question types (`MCQ`, `SUBJECTIVE`, `CODING`) are created by their respective factory, looked up by type string.

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `QuestionFactory` | **Creator** (interface) — defines `createQuestion(examId, text, marks)` |
| `McqQuestionFactory` | **ConcreteCreator** — creates `Question` with `type="MCQ"` |
| `SubjectiveQuestionFactory` | **ConcreteCreator** — creates `Question` with `type="SUBJECTIVE"` |
| `CodingQuestionFactory` | **ConcreteCreator** — creates `Question` with `type="CODING"` |
| `Question` | **Product** — the entity created by the factories |
| `QuestionService` | Client — holds `Map<String, QuestionFactory>`, looks up by type |

### How They Interact
Spring auto-collects all `QuestionFactory` beans into a `Map<String, QuestionFactory>` using their `@Component` names (`"MCQ"`, `"SUBJECTIVE"`, `"CODING"`) as keys. `QuestionService.createQuestion()` receives a type string, looks up the matching factory from the map, and calls `factory.createQuestion()`. The factory returns a `Question` entity with the correct type pre-set.

### PlantUML

```plantuml
@startuml Factory_Question
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Factory Method Pattern" #E3F2FD {
    interface QuestionFactory <<Creator>> {
        + createQuestion(examId, text, marks) : Question
    }

    class McqQuestionFactory <<ConcreteCreator>> {
        + createQuestion(examId, text, marks) : Question
    }

    class SubjectiveQuestionFactory <<ConcreteCreator>> {
        + createQuestion(examId, text, marks) : Question
    }

    class CodingQuestionFactory <<ConcreteCreator>> {
        + createQuestion(examId, text, marks) : Question
    }

    class Question <<Product>> {
        - id : Long
        - examId : Long
        - type : String
        - text : String
        - marks : int
    }
}

class QuestionService <<Client>> {
    - factories : Map<String, QuestionFactory>
    + createQuestion(examId, type, text, marks)
}

QuestionFactory <|.. McqQuestionFactory : "@Component(\"MCQ\")"
QuestionFactory <|.. SubjectiveQuestionFactory : "@Component(\"SUBJECTIVE\")"
QuestionFactory <|.. CodingQuestionFactory : "@Component(\"CODING\")"

McqQuestionFactory ..> Question : "creates (type=MCQ)"
SubjectiveQuestionFactory ..> Question : "creates (type=SUBJECTIVE)"
CodingQuestionFactory ..> Question : "creates (type=CODING)"

QuestionService --> "0..*" QuestionFactory : "Map lookup by type"

note bottom of QuestionService
  factories.get("MCQ")
  → McqQuestionFactory
end note
@enduml
```

---

## Pattern 5 — Proxy (Structural)

### Identified In
Question bank access control — questions are only returned when the exam is `ONGOING`. The proxy gates access before delegating to the real implementation.

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `QuestionBank` | **Subject** (interface) — defines `getQuestions(examId)` |
| `RealQuestionBank` | **RealSubject** — fetches questions from `QuestionRepository` |
| `QuestionBankProxy` | **Proxy** — checks exam status; delegates to `RealQuestionBank` only if `ONGOING` |
| `QuestionService` | Client — calls proxy, unaware of the guard logic |

### How They Interact
`QuestionService` holds a `QuestionBankProxy` (injected by Spring). When `getQuestions()` is called, the proxy fetches the `Exam` from `ExamRepository` and checks its status. If `status == "ONGOING"`, it delegates to `RealQuestionBank.getQuestions()`. Otherwise, it returns an empty list — no DB query for questions at all.

### PlantUML

```plantuml
@startuml Proxy_QuestionBank
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Proxy Pattern" #F3E5F5 {
    interface QuestionBank <<Subject>> {
        + getQuestions(examId) : List<Question>
    }

    class RealQuestionBank <<RealSubject>> {
        - questionRepository
        + getQuestions(examId) : List<Question>
    }

    class QuestionBankProxy <<Proxy>> {
        - realQuestionBank : RealQuestionBank
        - examRepository : ExamRepository
        + getQuestions(examId) : List<Question>
    }
}

class QuestionService <<Client>> {
    + getQuestions(examId)
}

QuestionBank <|.. RealQuestionBank
QuestionBank <|.. QuestionBankProxy
QuestionBankProxy o-- RealQuestionBank : "delegates to"

QuestionService ..> QuestionBankProxy : "calls"

note bottom of QuestionBankProxy
  if exam.status == "ONGOING"
    → delegate to RealQuestionBank
  else
    → return empty list
end note
@enduml
```

---

## Pattern 6 — Bridge (Structural)

### Identified In
Notification system — the sending strategy (immediate, batched) and delivery channel (email, SMS) can vary independently.

### Classes Involved

| Class | Role in Pattern |
|---|---|
| `NotificationSender` | **Abstraction** (abstract class) — holds a `NotificationChannel` reference |
| `ImmediateSender` | **RefinedAbstraction** — sends immediately through the channel |
| `NotificationChannel` | **Implementor** (interface) — defines `send(recipient, message)` |
| `EmailChannel` | **ConcreteImplementor** — prints `[EMAIL to ...]` (simulated send) |
| `SubmissionService` | Client — calls `notificationSender.notify()` after submission |

### How They Interact
`ImmediateSender` extends `NotificationSender` and inherits the `channel` field (a `NotificationChannel`). When `notify()` is called, `ImmediateSender` delegates to `channel.send()`. Currently `EmailChannel` is the only concrete channel. The bridge allows swapping sender strategy OR channel independently — e.g., add `BatchedSender` without touching channels, or add `SmsChannel` without touching senders.

### PlantUML

```plantuml
@startuml Bridge_Notification
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter

package "Bridge Pattern" #FCE4EC {
    abstract class NotificationSender <<Abstraction>> {
        # channel : NotificationChannel
        + {abstract} notify(recipient, message)
    }

    class ImmediateSender <<RefinedAbstraction>> {
        + notify(recipient, message)
    }

    interface NotificationChannel <<Implementor>> {
        + send(recipient, message)
    }

    class EmailChannel <<ConcreteImplementor>> {
        + send(recipient, message)
    }
}

class SubmissionService <<Client>> {
    + submit(examId, studentId)
}

NotificationSender <|-- ImmediateSender
NotificationSender o-- NotificationChannel : "channel"
NotificationChannel <|.. EmailChannel

SubmissionService ..> ImmediateSender : "notify()"
ImmediateSender ..> EmailChannel : "channel.send()"

note bottom of NotificationSender
  Sender dimension: Immediate, Batched, Scheduled
  Channel dimension: Email, SMS, Push
  Either can vary independently
end note
@enduml
```

---

## Combined Overview — All 6 Patterns in Context

This shows how the patterns connect through the service layer — **not a full system diagram**, just the pattern wiring points.

```plantuml
@startuml Combined_PatternOverview
skinparam classAttributeIconSize 0
skinparam shadowing false
skinparam defaultFontName Inter
skinparam packageStyle rectangle

title "EOMS — Design Pattern Integration Overview"

package "Service Layer (Orchestration)" #ECEFF1 {
    class ExamService {
        + startExam(id)
    }
    class QuestionService {
        + createQuestion(examId, type, text, marks)
        + getQuestions(examId)
    }
    class SubmissionService {
        + submit(examId, studentId)
    }
}

package "①  Singleton" #F5F5DC {
    class ExamClock <<Singleton>> {
        + {static} getInstance() : ExamClock
        + now() : LocalDateTime
    }
}

package "②  Observer" #E8F5E9 {
    class ExamEventPublisher <<Subject>>
    interface ExamEventListener <<Observer>>
    class AuditLogObserver <<ConcreteObserver>>

    ExamEventPublisher o-- ExamEventListener
    AuditLogObserver ..|> ExamEventListener
}

package "③  Chain of Responsibility" #FFF3E0 {
    abstract class SubmissionHandler <<Handler>>
    class AuthCheckHandler
    class TimeWindowCheckHandler
    class AlreadySubmittedHandler

    SubmissionHandler <|-- AuthCheckHandler
    SubmissionHandler <|-- TimeWindowCheckHandler
    SubmissionHandler <|-- AlreadySubmittedHandler
    AuthCheckHandler --> TimeWindowCheckHandler : "next"
    TimeWindowCheckHandler --> AlreadySubmittedHandler : "next"
}

package "④  Factory Method" #E3F2FD {
    interface QuestionFactory <<Creator>>
    class McqQuestionFactory
    class SubjectiveQuestionFactory
    class CodingQuestionFactory

    QuestionFactory <|.. McqQuestionFactory
    QuestionFactory <|.. SubjectiveQuestionFactory
    QuestionFactory <|.. CodingQuestionFactory
}

package "⑤  Proxy" #F3E5F5 {
    interface QuestionBank <<Subject>>
    class QuestionBankProxy <<Proxy>>
    class RealQuestionBank <<RealSubject>>

    QuestionBank <|.. QuestionBankProxy
    QuestionBank <|.. RealQuestionBank
    QuestionBankProxy o-- RealQuestionBank
}

package "⑥  Bridge" #FCE4EC {
    abstract class NotificationSender <<Abstraction>>
    class ImmediateSender <<RefinedAbstraction>>
    interface NotificationChannel <<Implementor>>
    class EmailChannel <<ConcreteImplementor>>

    NotificationSender <|-- ImmediateSender
    NotificationSender o-- NotificationChannel
    NotificationChannel <|.. EmailChannel
}

' --- Wiring ---
ExamService ..> ExamEventPublisher : "②"
ExamService ..> ExamClock : "①"

QuestionService ..> QuestionFactory : "④"
QuestionService ..> QuestionBankProxy : "⑤"

SubmissionService ..> AuthCheckHandler : "③ initiates chain"
SubmissionService ..> ExamEventPublisher : "②"
SubmissionService ..> ImmediateSender : "⑥"
SubmissionService ..> ExamClock : "①"

TimeWindowCheckHandler ..> ExamClock : "①"
@enduml
```

---

## How to Render

Paste any of the `@startuml ... @enduml` blocks into:

| Tool | URL |
|---|---|
| **PlantUML Online** | [plantuml.com/plantuml/uml](https://www.plantuml.com/plantuml/uml/) |
| **PlantText** | [planttext.com](https://www.planttext.com/) |
| **VS Code** | Extension: `jebbs.plantuml` |
| **IntelliJ** | Built-in PlantUML plugin |

Each diagram renders independently — paste one at a time to get clean, focused images for your report or presentation.
