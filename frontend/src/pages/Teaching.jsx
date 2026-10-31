import { useEffect, useState } from "react";
import { api, download } from "../api";
import { label, todayIso } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, OkNote, Pill, Table } from "../ui";

export default function Teaching() {
  const { user } = useAuth();
  const manager = user.role !== "TEACHER";
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [status, setStatus] = useState("ALL");
  const [data, setData] = useState(null);
  const [teachers, setTeachers] = useState([]);
  const [form, setForm] = useState({ teacherId: "", className: "S2 A", subject: "Mathematics", room: "B12", date: todayIso(), startTime: "08:00", endTime: "08:40", note: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  async function load() {
    setData(await api(`/api/activities?from=${from}&to=${to}&status=${status}`));
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, [from, to, status]);
  useEffect(() => {
    if (manager) api("/api/attendance/sheet").then((sheet) => setTeachers(sheet.rows.map((r) => r.teacher))).catch(() => {});
  }, [manager]);

  async function create(e) {
    e.preventDefault();
    setError("");
    try {
      await api("/api/activities", { method: "POST", body: form });
      setOk("Class added to the schedule.");
      await load();
    } catch (err) { setError(err.message); }
  }

  async function punch(id, action) {
    setError("");
    try {
      await api(`/api/activities/${id}/${action}`, { method: "POST" });
      await load();
    } catch (err) { setError(err.message); }
  }

  const stats = data?.stats || {};
  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Class and teaching activity</h2>
          <p>See who is on schedule, delayed, or missed, and start or end a class.</p>
        </div>
        {manager && <button className="btn ghost" onClick={() => download(`/api/reports/export?type=activity&format=csv&from=${from}&to=${to}`, "activity.csv").catch((err) => setError(err.message))}>Export logs</button>}
      </div>
      <div className="grid kpis">
        <article className="card stat good"><span>On schedule</span><b>{stats.onSchedule ?? 0}</b></article>
        <article className="card stat warn"><span>Delayed</span><b>{stats.delayed ?? 0}</b></article>
        <article className="card stat bad"><span>Missed</span><b>{stats.missed ?? 0}</b></article>
        <article className="card stat"><span>Completed</span><b>{stats.completed ?? 0}</b></article>
      </div>
      <div className="filters" style={{ margin: "12px 0" }}>
        <label className="field">From<input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="field">To<input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
        <label className="field">Compliance
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="ALL">All</option>
            {["SCHEDULED", "ON_SCHEDULE", "DELAYED", "MISSED", "COMPLETED"].map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </label>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <Table
        rows={data?.sessions || []}
        columns={[
          { key: "date", label: "Date" },
          { key: "startTime", label: "Time", render: (row) => String(row.startTime).slice(0, 5) + "–" + String(row.endTime).slice(0, 5) },
          { key: "teacherName", label: "Teacher" },
          { key: "className", label: "Class" },
          { key: "subject", label: "Subject" },
          { key: "room", label: "Room" },
          { key: "status", label: "Compliance", render: (row) => <Pill value={row.status} /> },
          { key: "note", label: "Note" },
          { key: "go", label: "", render: (row) => (
            <span className="row-actions">
              {row.status === "SCHEDULED" && <button className="btn" onClick={() => punch(row.id, "start")}>Start</button>}
              {(row.status === "ON_SCHEDULE" || row.status === "DELAYED") && <button className="btn ghost" onClick={() => punch(row.id, "end")}>End</button>}
            </span>
          ) }
        ]}
      />
      {manager && (
        <form className="card" style={{ marginTop: 14 }} onSubmit={create}>
          <h3>Add a class</h3>
          <div className="filters">
            <label className="field">Teacher
              <select value={form.teacherId} onChange={(e) => setForm({ ...form, teacherId: e.target.value })} required>
                <option value="">Choose</option>
                {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
              </select>
            </label>
            <label className="field">Class<input value={form.className} onChange={(e) => setForm({ ...form, className: e.target.value })} /></label>
            <label className="field">Subject<input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} /></label>
            <label className="field">Room<input value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} /></label>
            <label className="field">Date<input type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></label>
            <label className="field">Start<input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></label>
            <label className="field">End<input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></label>
          </div>
          <button className="btn" style={{ marginTop: 10 }}>Add to schedule</button>
        </form>
      )}
    </div>
  );
}
