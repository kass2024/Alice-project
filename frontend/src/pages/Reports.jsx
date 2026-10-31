import { useEffect, useState } from "react";
import { Bar, BarChart, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, download } from "../api";
import { label, todayIso } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, OkNote } from "../ui";

const COLORS = ["#1b6b45", "#a33b32", "#c4841d", "#1d5f8a", "#6d645b"];

export default function Reports() {
  const { user } = useAuth();
  const [from, setFrom] = useState(() => shift(-30));
  const [to, setTo] = useState(todayIso());
  const [department, setDepartment] = useState("ALL");
  const [type, setType] = useState("attendance");
  const [format, setFormat] = useState("xlsx");
  const [time, setTime] = useState("18:00");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api(`/api/reports?from=${from}&to=${to}&department=${department}`)
      .then((row) => { setData(row); if (row.schedule) setTime(row.schedule); })
      .catch((err) => setError(err.message));
  }, [from, to, department]);

  async function saveTime() {
    setError("");
    try {
      await api("/api/reports/schedule", { method: "PUT", body: { time } });
      setOk("Daily summary will be prepared at " + time + ".");
    } catch (err) { setError(err.message); }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Reports and analytics</h2>
          <p>Attendance, document submissions, and teaching activity for the dates you choose.</p>
        </div>
      </div>
      <div className="filters">
        <label className="field">From<input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="field">To<input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
        <label className="field">Department
          <select value={department} onChange={(e) => setDepartment(e.target.value)}>
            {["ALL", "Sciences", "Languages", "Humanities", "ICT"].map((d) => <option key={d}>{d}</option>)}
          </select>
        </label>
        <label className="field">Report
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="attendance">Attendance</option>
            <option value="documents">Documents</option>
            <option value="activity">Teaching activity</option>
          </select>
        </label>
        <label className="field">Format
          <select value={format} onChange={(e) => setFormat(e.target.value)}>
            <option value="pdf">PDF</option>
            <option value="xlsx">Excel</option>
            <option value="csv">CSV</option>
          </select>
        </label>
        <button className="btn" onClick={() => download(`/api/reports/export?type=${type}&format=${format}&from=${from}&to=${to}`, `${type}.${format === "xlsx" ? "xlsx" : format}`).catch((err) => setError(err.message))}>Export</button>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <div className="grid two" style={{ marginTop: 14 }}>
        <section className="card">
          <h3>Attendance by day</h3>
          <div style={{ height: 240 }}>
            <ResponsiveContainer>
              <LineChart data={data?.daily || []}>
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line dataKey="attended" stroke="#1b6b45" strokeWidth={2} dot={false} />
                <Line dataKey="away" stroke="#a33b32" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
        <section className="card">
          <h3>Status mix</h3>
          <div style={{ height: 240 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie data={(data?.statusPie || []).map((row) => ({ ...row, name: label(row.name) }))} dataKey="value" nameKey="name" outerRadius={80}>
                  {(data?.statusPie || []).map((entry, i) => <Cell key={entry.name} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>
      <div className="grid two" style={{ marginTop: 14 }}>
        <section className="card">
          <h3>Teacher attendance rate</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={data?.teacherRates || []}>
                <XAxis dataKey="teacher" tick={{ fontSize: 11 }} interval={0} angle={-20} height={70} />
                <YAxis />
                <Tooltip />
                <Bar dataKey="rate" fill="#1b6b45" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>
        <section className="card">
          <h3>Documents submitted</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={(data?.documentBars || []).map((row) => ({ ...row, name: label(row.name) }))}>
                <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-20} height={70} />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="value" fill="#b5812f" />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <p className="lead">Classes completed: {data?.activity?.completed || 0}. Missed: {data?.activity?.missed || 0}. Delayed: {data?.activity?.delayed || 0}.</p>
          {user.role === "ADMIN" && (
            <div className="filters">
              <label className="field">Daily summary time<input type="time" value={time} onChange={(e) => setTime(e.target.value)} /></label>
              <button className="btn ghost" onClick={saveTime}>Save schedule</button>
            </div>
          )}
          {data?.lastRun && <p className="lead">Last automatic summary: {data.lastRun}</p>}
        </section>
      </div>
    </div>
  );
}

function shift(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}
