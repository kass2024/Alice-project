export const PROJECT_NAME = "Smart Teacher Attendance & Pedagogical Document Management System";
export const PROJECT_SUB = "Digital Teacher Attendance Tracking, Document Management & Teaching Activity Monitoring Platform";

export function label(value) {
  if (value === null || value === undefined || value === "") return "—";
  const text = String(value);
  const known = {
    HEAD_OF_STUDIES: "Head of Studies",
    LESSON_PLAN: "Lesson plan",
    SCHEME_OF_WORK: "Scheme of work",
    CLASS_REGISTER: "Class register",
    WEEKS_BREAKDOWN: "Weeks breakdown",
    ON_LEAVE: "On leave",
    ON_SCHEDULE: "On schedule",
    UNDER_REVIEW: "Under review",
    IN_APP: "In app"
  };
  if (known[text]) return known[text];
  if (["AI", "CSV", "ICT", "PDF", "REB", "TVET"].includes(text)) return text;
  const pretty = text.replaceAll("_", " ").replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
  return pretty.charAt(0).toUpperCase() + pretty.slice(1);
}

export function when(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ").slice(0, 16);
  return date.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function day(value) {
  if (!value) return "—";
  return String(value).slice(0, 10);
}

export function todayIso() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}
