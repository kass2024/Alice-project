import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, download } from "../api";
import { day, label, todayIso } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, OkNote, Pill, Table } from "../ui";

const STATUSES = ["PRESENT", "ABSENT", "LATE", "ON_LEAVE"];

export default function Attendance() {
  const { user } = useAuth();
  const manager = user?.role !== "TEACHER";
  const [view, setView] = useState("daily");
  const [date, setDate] = useState(todayIso());
  const [department, setDepartment] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [q, setQ] = useState("");
  const [sheet, setSheet] = useState(null);
  const [edits, setEdits] = useState({});
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const range = useMemo(() => span(date, view), [date, view]);

  async function loadSheet() {
    const data = await api(`/api/attendance/sheet?date=${date}&department=${department}`);
    setSheet(data);
    const next = {};
    data.rows.forEach((row) => {
      next[row.teacher.id] = { status: row.record?.status || "", note: row.record?.note || "" };
    });
    setEdits(next);
  }

  async function loadLogs() {
    const data = await api(`/api/attendance?from=${range.from}&to=${range.to}&department=${department}&status=${status}&q=${encodeURIComponent(q)}`);
    setLogs(data.records || []);
  }

  useEffect(() => {
    setError("");
    loadSheet().catch((err) => setError(err.message));
    loadLogs().catch(() => {});
  }, [date, department, status, q, view]);

  async function save() {
    setError(""); setOk("");
    try {
      const rows = Object.entries(edits)
        .filter(([, value]) => value.status)
        .map(([teacherId, value]) => ({ teacherId: Number(teacherId), status: value.status, note: value.note }));
      const result = await api("/api/attendance/bulk", { method: "POST", body: { date, rows } });
      setOk("Saved " + result.saved + " attendance rows.");
      await loadSheet();
      await loadLogs();
    } catch (err) {
      setError(err.message);
    }
  }

  function markAll(value) {
    const next = { ...edits };
    Object.keys(next).forEach((id) => { next[id] = { ...next[id], status: value }; });
    setEdits(next);
  }

  const weekDays = daysBetween(range.from, range.to).slice(0, 7);
  const byTeacher = {};
  logs.forEach((row) => {
    if (!byTeacher[row.teacherId]) byTeacher[row.teacherId] = { name: row.teacherName, days: {} };
    byTeacher[row.teacherId].days[row.date] = row.status;
  });

  const monthStart = new Date(date.slice(0, 7) + "-01T00:00:00");
  const monthDays = [];
  const cursor = new Date(monthStart);
  while (cursor.getMonth() === monthStart.getMonth()) {
    const iso = new Date(cursor.getTime() - cursor.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    monthDays.push(iso);
    cursor.setDate(cursor.getDate() + 1);
  }
  const counts = {};
  logs.forEach((row) => {
    counts[row.date] = counts[row.date] || { present: 0, total: 0 };
    counts[row.date].total++;
    if (row.status === "PRESENT" || row.status === "LATE") counts[row.date].present++;
  });

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Teacher attendance</h2>
          <p>Mark the day, review the week or month, and export the register.</p>
        </div>
        <div className="tabs">
          {["daily", "weekly", "monthly", "logs"].map((item) => (
            <button key={item} className={view === item ? "on" : ""} onClick={() => setView(item)}>{label(item)}</button>
          ))}
        </div>
      </div>
      <div className="filters" style={{ marginBottom: 12 }}>
        <label className="field">Date<input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></label>
        {manager && (
          <label className="field">Department
            <select value={department} onChange={(e) => setDepartment(e.target.value)}>
              {["ALL", "Sciences", "Languages", "Humanities", "ICT"].map((d) => <option key={d}>{d}</option>)}
            </select>
          </label>
        )}
        <label className="field">Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="ALL">All</option>
            {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </label>
        <label className="field">Search<input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Teacher name" /></label>
        <button className="btn ghost" onClick={() => download(`/api/attendance/export?format=csv&from=${range.from}&to=${range.to}`, "attendance.csv")}>CSV</button>
        <button className="btn ghost" onClick={() => download(`/api/attendance/export?format=xlsx&from=${range.from}&to=${range.to}`, "attendance.xlsx")}>Excel</button>
        <button className="btn ghost" onClick={() => download(`/api/attendance/export?format=pdf&from=${range.from}&to=${range.to}`, "attendance.pdf")}>PDF</button>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />

      {view === "daily" && (
        <section className="card">
          <div className="row-actions" style={{ marginBottom: 10 }}>
            <span>Summary: {sheet?.summary?.present || 0} present, {sheet?.summary?.absent || 0} absent, {sheet?.summary?.late || 0} late, {sheet?.summary?.onLeave || 0} on leave</span>
            {manager && <button className="btn ghost" onClick={() => markAll("PRESENT")}>Mark all present</button>}
            {manager && <button className="btn" onClick={save}>Save attendance</button>}
          </div>
          <Table
            rows={(sheet?.rows || []).map((row) => ({
              id: row.teacher.id,
              teacherName: row.teacher.fullName,
              department: row.teacher.department,
              staffCode: row.teacher.staffCode,
              status: edits[row.teacher.id]?.status,
              note: edits[row.teacher.id]?.note,
              checkIn: row.record?.checkIn
            }))}
            columns={[
              { key: "teacherName", label: "Teacher", render: (row) => <Link to={"/profile/" + row.id}>{row.teacherName}</Link> },
              { key: "department", label: "Department" },
              { key: "staffCode", label: "Code" },
              { key: "status", label: "Status", render: (row) => manager ? (
                <select value={edits[row.id]?.status || ""} onChange={(e) => setEdits({ ...edits, [row.id]: { ...edits[row.id], status: e.target.value } })}>
                  <option value="">Not marked</option>
                  {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                </select>
              ) : <Pill value={row.status || "DRAFT"} /> },
              { key: "note", label: "Note", render: (row) => manager ? (
                <input value={edits[row.id]?.note || ""} onChange={(e) => setEdits({ ...edits, [row.id]: { ...edits[row.id], note: e.target.value } })} />
              ) : (row.note || "—") },
              { key: "checkIn", label: "Check in", render: (row) => row.checkIn ? String(row.checkIn).slice(11, 16) : "—" }
            ]}
          />
        </section>
      )}

      {view === "weekly" && (
        <section className="card">
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Teacher</th>{weekDays.map((d) => <th key={d}>{d.slice(5)}</th>)}</tr>
              </thead>
              <tbody>
                {Object.values(byTeacher).map((row) => (
                  <tr key={row.name}>
                    <td>{row.name}</td>
                    {weekDays.map((d) => <td key={d}>{row.days[d] ? <Pill value={row.days[d]} /> : "—"}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {view === "monthly" && (
        <div className="calendar">
          {monthDays.map((d) => (
            <button key={d} className="cal-day" onClick={() => { setDate(d); setView("daily"); }}>
              <b>{Number(d.slice(8))}</b>
              <small>{counts[d] ? counts[d].present + "/" + counts[d].total + " in" : "No marks"}</small>
            </button>
          ))}
        </div>
      )}

      {view === "logs" && (
        <Table
          rows={logs.map((row) => ({ ...row, date: day(row.date) }))}
          columns={[
            { key: "date", label: "Date" },
            { key: "teacherName", label: "Teacher", render: (row) => <Link to={"/profile/" + row.teacherId}>{row.teacherName}</Link> },
            { key: "department", label: "Department" },
            { key: "status", label: "Status", render: (row) => <Pill value={row.status} /> },
            { key: "method", label: "Method", render: (row) => label(row.method) },
            { key: "checkIn", label: "In", render: (row) => row.checkIn ? String(row.checkIn).slice(11, 16) : "—" },
            { key: "checkOut", label: "Out", render: (row) => row.checkOut ? String(row.checkOut).slice(11, 16) : "—" },
            { key: "note", label: "Note" }
          ]}
        />
      )}
    </div>
  );
}

function span(date, view) {
  const base = new Date(date + "T00:00:00");
  if (view === "monthly") {
    const start = new Date(base.getFullYear(), base.getMonth(), 1);
    const end = new Date(base.getFullYear(), base.getMonth() + 1, 0);
    return { from: iso(start), to: iso(end) };
  }
  const dayNum = base.getDay() || 7;
  const monday = new Date(base);
  monday.setDate(base.getDate() - dayNum + 1);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  if (view === "daily") return { from: date, to: date };
  return { from: iso(monday), to: iso(sunday) };
}

function iso(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function daysBetween(from, to) {
  const out = [];
  const cursor = new Date(from + "T00:00:00");
  const end = new Date(to + "T00:00:00");
  while (cursor <= end) {
    out.push(iso(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return out;
}
