-- Non-destructive upgrade for installations created with the legacy schema.
ALTER TABLE students ADD COLUMN IF NOT EXISTS password_hash TEXT;
CREATE TABLE IF NOT EXISTS question_keys (
    question_id BIGINT PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    correct_answer TEXT NOT NULL
);
ALTER TABLE answers ADD COLUMN IF NOT EXISTS awarded_marks DOUBLE PRECISION;
