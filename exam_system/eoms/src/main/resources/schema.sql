CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS exams (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    type VARCHAR(20) NOT NULL,
    text TEXT NOT NULL,
    marks INT NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    student_id BIGINT NOT NULL REFERENCES students(id),
    submitted_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    score DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    exam_id BIGINT,
    student_id BIGINT,
    logged_at TIMESTAMP NOT NULL,
    details TEXT
);
