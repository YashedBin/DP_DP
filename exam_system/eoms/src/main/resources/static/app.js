const BASE_URL = "/api";

let currentExamId = "";
let loggedInRole = "";
let loggedInName = "";

function show(id) {
  document.querySelectorAll(".screen").forEach(s => s.classList.add("hidden"));
  document.getElementById(id).classList.remove("hidden");
}

function goToLogin() {
  show("login-screen");
}

function loginTeacher() {
  const name = document.getElementById("teacher-username").value.trim();
  const err = document.getElementById("login-error");
  if (!name) {
    err.textContent = "Please enter a username.";
    return;
  }
  err.textContent = "";
  loggedInRole = "Teacher";
  loggedInName = name;
  document.getElementById("teacher-user-label").textContent = "Teacher: " + name;
  post("/teachers/login", { username: name });
  show("teacher-screen");
}

async function loginStudent() {
  const roll = document.getElementById("student-rollno").value.trim();
  const err = document.getElementById("login-error");
  if (!roll || isNaN(Number(roll))) {
    err.textContent = "Please enter a numeric Roll No.";
    return;
  }
  err.textContent = "";
  loggedInRole = "Student";
  loggedInName = roll;
  document.getElementById("student-user-label").textContent = "Student: " + roll;
  document.getElementById("submit-rollno-label").textContent = roll;
  await post("/students/" + roll + "/ensure", {});
  show("student-screen");
}

function setCurrentExam(id) {
  currentExamId = id;
  const display = id ? "Exam #" + id : "No exam loaded";
  document.getElementById("teacher-exam-label").textContent = display;
  document.getElementById("student-exam-label").textContent = display;
  document.getElementById("control-exam-id").value = id;
  document.getElementById("student-exam-id").value = id;
  document.getElementById("submissions-exam-id").value = id;
}

function setStatus(elId, text, ok) {
  const el = document.getElementById(elId);
  el.textContent = text;
  el.className = "status " + (ok ? "ok" : "error");
}

async function post(path, body) {
  try {
    const res = await fetch(BASE_URL + path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    const text = await res.text();
    if (!text) return {};
    try {
      return JSON.parse(text);
    } catch (parseErr) {
      return { error: text };
    }
  } catch (e) {
    return { error: "Connection failed — is the server running?" };
  }
}

async function get(path) {
  try {
    const res = await fetch(BASE_URL + path);
    const text = await res.text();
    if (!text) return {};
    try {
      return JSON.parse(text);
    } catch (parseErr) {
      return { error: text };
    }
  } catch (e) {
    return { error: "Connection failed — is the server running?" };
  }
}

// ---------- Teacher actions ----------

async function createExam() {
  const title = document.getElementById("exam-title").value;
  const duration = document.getElementById("exam-duration").value;
  const start = document.getElementById("exam-start").value;
  const end = document.getElementById("exam-end").value;

  const resp = await post("/exams", {
    title: title,
    durationMinutes: Number(duration),
    startTime: start,
    endTime: end,
    status: "DRAFT"
  });

  if (resp.id) {
    setCurrentExam(resp.id);
    setStatus("create-status", "Exam #" + resp.id + " created", true);
  } else {
    setStatus("create-status", resp.error || "Failed to create exam", false);
  }
}

async function loadExam() {
  const id = document.getElementById("control-exam-id").value.trim();
  if (!id) { setStatus("control-status", "Enter an exam ID", false); return; }
  const resp = await get("/exams/" + id);
  if (resp.status) {
    setCurrentExam(id);
    setStatus("control-status", "Exam #" + id + " — " + resp.status, true);
    refreshQuestionList(id);
  } else {
    setStatus("control-status", resp.error || "Exam not found", false);
  }
}

async function startExam() {
  const id = document.getElementById("control-exam-id").value.trim();
  if (!id) { setStatus("control-status", "Enter an exam ID", false); return; }
  const resp = await post("/exams/" + id + "/start", {});
  if (resp.error) {
    setStatus("control-status", resp.error, false);
  } else {
    setCurrentExam(id);
    setStatus("control-status", "Exam #" + id + " is now ONGOING", true);
  }
}

async function addQuestion() {
  const id = document.getElementById("control-exam-id").value.trim();
  if (!id) { setStatus("question-status", "Load or create an exam first", false); return; }

  const resp = await post("/exams/" + id + "/questions", {
    type: document.getElementById("question-type").value,
    text: document.getElementById("question-text").value,
    marks: Number(document.getElementById("question-marks").value),
    extra: document.getElementById("question-extra").value
  });

  if (resp.id) {
    setStatus("question-status", "Question #" + resp.id + " added", true);
    document.getElementById("question-text").value = "";
    document.getElementById("question-extra").value = "";
    refreshQuestionList(id);
  } else {
    setStatus("question-status", resp.error || "Failed to add question", false);
  }
}

async function refreshQuestionList(examId) {
  const list = document.getElementById("question-list");
  list.innerHTML = "";
  const questions = await get("/exams/" + examId + "/questions");
  if (!Array.isArray(questions) || questions.length === 0) {
    list.innerHTML = "<div>No questions yet (or exam isn't ONGOING).</div>";
    return;
  }
  questions.forEach(q => {
    const line = document.createElement("div");
    let text = "#" + q.id + " [" + q.type + ", " + q.marks + " marks] " + q.text;
    if (q.options) text += " (" + q.options + ")";
    line.textContent = text;
    list.appendChild(line);
  });
}

async function viewSubmissions() {
  const id = document.getElementById("submissions-exam-id").value.trim();
  if (!id) { setStatus("submissions-status", "Enter an exam ID", false); return; }

  const list = document.getElementById("submissions-list");
  list.innerHTML = "";
  const submissions = await get("/exams/" + id + "/submissions");

  if (!Array.isArray(submissions) || submissions.length === 0) {
    list.innerHTML = "<div>No submissions yet for this exam.</div>";
    setStatus("submissions-status", "0 submissions", true);
    return;
  }

  submissions.forEach(s => {
    const line = document.createElement("div");
    line.textContent = "#" + s.id + " — Student " + s.studentId + " — " + s.status + " — " + s.submittedAt;
    list.appendChild(line);
  });
  setStatus("submissions-status", submissions.length + " submission(s)", true);
}

// ---------- Student actions ----------

let countdownInterval = null;

function startTimer(durationMinutes) {
  if (countdownInterval) clearInterval(countdownInterval);
  const end = Date.now() + durationMinutes * 60000;
  const timerEl = document.getElementById("exam-timer");

  function tick() {
    const diff = end - Date.now();
    if (diff <= 0) {
      timerEl.textContent = "Time's up!";
      timerEl.style.color = "var(--error)";
      clearInterval(countdownInterval);
      return;
    }
    const h = Math.floor(diff / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    const s = Math.floor((diff % 60000) / 1000);
    const pad = n => String(n).padStart(2, "0");
    timerEl.textContent = "Time remaining: " + pad(h) + ":" + pad(m) + ":" + pad(s);
  }
  tick();
  countdownInterval = setInterval(tick, 1000);
}

async function loadQuestions() {
  const id = document.getElementById("student-exam-id").value.trim();
  if (!id) { setStatus("load-status", "Enter an exam ID", false); return; }
  setCurrentExam(id);

  const exam = await get("/exams/" + id);
  if (exam.durationMinutes) {
    startTimer(exam.durationMinutes);
  }

  const container = document.getElementById("question-cards");
  container.innerHTML = "";
  const questions = await get("/exams/" + id + "/questions");

  if (!Array.isArray(questions) || questions.length === 0) {
    container.innerHTML = "<p>No questions yet — exam might not be ONGOING.</p>";
    setStatus("load-status", "Loaded, but no questions found", false);
    return;
  }

  questions.forEach((q, idx) => {
    const card = document.createElement("div");
    card.className = "q-card";
    card.dataset.questionId = q.id;

    const tagClass = q.type === "MCQ" ? "mcq" : q.type === "CODING" ? "coding" : "subjective";
    let html = `<div class="q-header">
      <strong>Q${idx + 1}</strong>
      <span class="q-tag ${tagClass}">${q.type}</span>
      <span class="q-marks">${q.marks} marks</span>
    </div>
    <div class="q-text">${escapeHtml(q.text)}</div>`;

    if (q.type === "MCQ" && q.options) {
      const opts = q.options.split(",");
      opts.forEach((opt, i) => {
        const letter = String.fromCharCode(65 + i);
        html += `<label class="q-option">
          <input type="radio" name="answer-${q.id}" value="${escapeHtml(opt.trim())}">
          ${letter}) ${escapeHtml(opt.trim())}
        </label>`;
      });
    } else if (q.type === "SUBJECTIVE") {
      html += `<textarea id="answer-${q.id}" rows="3" placeholder="Write your answer..."></textarea>`;
    } else if (q.type === "CODING") {
      const lang = q.options && q.options.startsWith("language:") ? q.options.substring(9) : "any";
      html += `<p class="q-marks">Language: ${lang}</p>
      <textarea id="answer-${q.id}" rows="5" placeholder="Write your code..." style="font-family: Consolas, monospace;"></textarea>`;
    }

    card.innerHTML = html;
    container.appendChild(card);
  });

  setStatus("load-status", "Loaded " + questions.length + " question(s)", true);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function collectAnswers() {
  const cards = document.querySelectorAll(".q-card");
  const answers = [];
  cards.forEach(card => {
    const qId = Number(card.dataset.questionId);
    const radio = card.querySelector(`input[type="radio"]:checked`);
    const textarea = card.querySelector("textarea");
    let answer = "";
    if (radio) {
      answer = radio.value;
    } else if (textarea) {
      answer = textarea.value;
    }
    answers.push({ questionId: qId, answer: answer });
  });
  return answers;
}

async function submitExam() {
  const id = document.getElementById("student-exam-id").value.trim();
  if (!id) { setStatus("submit-status", "Enter exam ID above", false); return; }
  if (loggedInRole !== "Student" || !loggedInName) {
    setStatus("submit-status", "Login as Student first", false);
    return;
  }

  const answers = collectAnswers();
  const resp = await post("/exams/" + id + "/submissions", {
    studentId: Number(loggedInName),
    answers: answers
  });

  if (resp.id) {
    setStatus("submit-status", "Submitted! (submission #" + resp.id + ")", true);
  } else {
    setStatus("submit-status", resp.error || "Submission failed", false);
  }
}