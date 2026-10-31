import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api";
import { label, todayIso } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, Pill } from "../ui";

export default function Dashboard() {
  const { user } = useAuth();
  const [from, setFrom] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 13);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  });
  const [to, setTo] = useState(todayIso());
  const [department, setDepartment] = useState("ALL");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    setError("");
    api(`/api/dashboard?from=${from}&to=${to}&department=${department}`)
      .then(setData)
      .catch((err) => setError(err.message));
  }, [from, to, department]);

  const k = data?.kpis || {};
  const manager = user?.role !== "TEACHER";
  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Dashboard</h2>
          <p>Today’s attendance, documents waiting for review, and class coverage.</p>
        </div>
        <div className="filters">
          <label className="field">From<input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
          <label className="field">To<input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
          {manager && (
            <label className="field">Department
              <select value={department} onChange={(e) => setDepartment(e.target.value)}>
                {["ALL", "Sciences", "Languages", "Humanities", "ICT"].map((d) => <option key={d}>{d}</option>)}
              </select>
            </label>
          )}
        </div>
      </div>
      <ErrorNote error={error} />
      <div className="grid kpis">
        <article className="card stat good"><span>Present today</span><b>{k.presentToday ?? "–"}</b></article>
        <article className="card stat bad"><span>Absent today</span><b>{k.absentToday ?? "–"}</b></article>
        <article className="card stat warn"><span>Late / on leave</span><b>{(k.lateToday ?? 0) + (k.onLeaveToday ?? 0)}</b></article>
        <article className="card stat"><span>Pending approvals</span><b>{k.pendingApprovals ?? "–"}</b></article>
      </div>
      <div style={{ height: 14 }} />
      <div className="grid two">
        <section className="card">
          <h3>Attendance summary</h3>
          <div style={{ height: 240 }}>
            <ResponsiveContainer>
              <BarChart data={data?.trend || []}>
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="present" fill="#1b6b45" />
                <Bar dataKey="late" fill="#c4841d" />
                <Bar dataKey="absent" fill="#a33b32" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>
        <section className="card">
          <h3>Quick actions</h3>
          <div className="row-actions">
            {manager && <Link className="btn" to="/attendance">Mark attendance</Link>}
            <Link className="btn ghost" to="/biometric">Biometric logs</Link>
            <Link className="btn ghost" to="/documents">Upload a document</Link>
            {manager && <Link className="btn ghost" to="/approvals">Approval queue</Link>}
          </div>
          <p className="lead">Documents this month: {k.documentsThisMonth ?? 0}. Classes today: {k.classesToday ?? 0}. Missed: {k.missedClasses ?? 0}. Unread alerts: {k.unread ?? 0}.</p>
        </section>
      </div>
      <div style={{ height: 14 }} />
      <div className="grid two">
        <section className="card">
          <h3>Document submission tracker</h3>
          {(data?.tracker || []).map((row) => (
            <div key={row.teacherId} style={{ display: "flex", justifyContent: "space-between", gap: 8, padding: "8px 0", borderBottom: "1px solid var(--line)" }}>
              <span>{row.teacher}</span>
              <span className="tracker">
                <Flag on={row.lessonPlan} text="Lesson plan" />
                <Flag on={row.scheme} text="Scheme" />
                <Flag on={row.register} text="Register" />
              </span>
            </div>
          ))}
          {!data?.tracker?.length && <p className="lead">No teachers in this filter.</p>}
        </section>
        <section className="card">
          <h3>Approval queue</h3>
          {(data?.queue || []).map((doc) => (
            <article key={doc.id} style={{ padding: "8px 0", borderBottom: "1px solid var(--line)" }}>
              <b>{doc.title}</b>
              <div className="lead">{doc.ownerName} · <Pill value={doc.status} /></div>
            </article>
          ))}
          {!data?.queue?.length && <p className="lead">No documents are waiting.</p>}
          <h3 style={{ marginTop: 18 }}>Compliance</h3>
          {(data?.compliance || []).slice(0, 6).map((row, i) => (
            <div key={i} className="lead">{row.teacher}: {row.issue}</div>
          ))}
        </section>
      </div>
      <div style={{ height: 14 }} />
      <div className="grid two">
        <section className="card">
          <h3>Teaching schedule today</h3>
          {(data?.schedule || []).map((row) => (
            <div key={row.id} style={{ display: "flex", justifyContent: "space-between", gap: 8, padding: "8px 0" }}>
              <span>{row.startTime?.slice(0, 5)} · {row.teacherName} · {row.className}</span>
              <Pill value={row.status} />
            </div>
          ))}
          {!data?.schedule?.length && <p className="lead">No classes on this date.</p>}
        </section>
        <section className="card">
          <h3>Recent activity</h3>
          {(data?.recent || []).map((row, i) => (
            <article key={i}>
              <b>{label(row.action)}</b>
              <div className="lead">{row.user} · {row.detail}</div>
            </article>
          ))}
        </section>
      </div>
    </div>
  );
}

function Flag({ on, text }) {
  return <span><i className={on ? "dot on" : "dot off"} /> {text}</span>;
}
