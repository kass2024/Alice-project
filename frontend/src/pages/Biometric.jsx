import { useEffect, useState } from "react";
import { api, saveText } from "../api";
import { label, when } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, Pill, Table } from "../ui";

export default function Biometric() {
  const { user } = useAuth();
  const [logs, setLogs] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [override, setOverride] = useState({ teacherId: "", status: "PRESENT", note: "" });
  const [result, setResult] = useState("");
  const [error, setError] = useState("");
  const manager = user?.role === "ADMIN" || user?.role === "HEAD_OF_STUDIES";

  function load() {
    api("/api/attendance/biometric").then(setLogs).catch(() => {});
    if (manager) {
      api("/api/attendance/sheet").then((data) => setTeachers(data.rows.map((row) => row.teacher))).catch(() => {});
    }
  }

  useEffect(() => { load(); }, [manager]);

  async function sendOverride(e) {
    e.preventDefault();
    setError("");
    try {
      await api("/api/attendance/override", { method: "POST", body: override });
      setResult("Manual override saved.");
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  function exportLogs() {
    const lines = ["Date,Teacher,Status,Check in,Check out,Distance,Method"];
    logs.forEach((row) => {
      lines.push([row.date, row.teacherName, row.status, row.checkIn || "", row.checkOut || "", row.faceDistance ?? "", row.method].join(","));
    });
    saveText("verified-attendance.csv", lines.join("\n"));
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Biometric attendance</h2>
          <p>Faces are enrolled and verified in the Android app, not in this website. This page shows the scans the phone already confirmed.</p>
        </div>
      </div>
      <section className="card">
        <h3>Android app</h3>
        <p>Open the project folder <b>app attendance</b> in Android Studio, install it on a phone, and point it at this computer’s API (port 8080). An admin enrolls each registered teacher. At the gate, Check in compares the live face with those enrollments.</p>
        <p className="lead">Before 08:15 the teacher is Present. After 08:15 the teacher is Late. A teacher who is already on leave is left unchanged. A face distance of 1.00 or lower is accepted.</p>
      </section>
      <div style={{ height: 14 }} />
      <div className="grid two">
        <section className="card">
          <h3>Manual override</h3>
          {manager ? (
            <form onSubmit={sendOverride} className="grid">
              <label className="field">Teacher
                <select value={override.teacherId} onChange={(e) => setOverride({ ...override, teacherId: e.target.value })} required>
                  <option value="">Choose</option>
                  {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}{t.hasFace ? "" : " (no face yet)"}</option>)}
                </select>
              </label>
              <label className="field">Status
                <select value={override.status} onChange={(e) => setOverride({ ...override, status: e.target.value })}>
                  {["PRESENT", "ABSENT", "LATE", "ON_LEAVE"].map((s) => <option key={s} value={s}>{label(s)}</option>)}
                </select>
              </label>
              <label className="field">Reason<textarea value={override.note} onChange={(e) => setOverride({ ...override, note: e.target.value })} /></label>
              <button className="btn">Save override</button>
            </form>
          ) : <p className="lead">An admin or the Head of Studies can override a failed scan.</p>}
          <ErrorNote error={error} />
          {result && <p className="result pass">{result}</p>}
        </section>
        <section className="card">
          <h3>Who has a face</h3>
          {manager ? (
            <div className="list">
              {teachers.map((t) => (
                <article key={t.id}><b>{t.fullName}</b> <span className="lead">{t.staffCode} · {t.hasFace ? "Enrolled on the phone" : "Not enrolled"}</span></article>
              ))}
            </div>
          ) : (
            <p>{user?.hasFace ? "Your face is enrolled. Check in with the Android app." : "Your face is not enrolled yet. Ask the admin to enroll you in the Android app."}</p>
          )}
        </section>
      </div>
      <div style={{ height: 14 }} />
      <section className="card">
        <div className="row-actions">
          <h3 style={{ margin: 0 }}>Biometric logs</h3>
          <button className="btn ghost" onClick={exportLogs}>Export verified attendance</button>
        </div>
        <Table
          rows={logs}
          columns={[
            { key: "date", label: "Date" },
            { key: "teacherName", label: "Teacher" },
            { key: "status", label: "Status", render: (row) => <Pill value={row.status} /> },
            { key: "method", label: "Method", render: (row) => label(row.method) },
            { key: "checkIn", label: "In", render: (row) => row.checkIn ? when(row.checkIn) : "—" },
            { key: "checkOut", label: "Out", render: (row) => row.checkOut ? when(row.checkOut) : "—" },
            { key: "faceDistance", label: "Distance" }
          ]}
          empty="No face or override records yet."
        />
      </section>
    </div>
  );
}
