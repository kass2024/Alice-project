import { useEffect, useState } from "react";
import { api } from "../api";
import { label, when } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, OkNote } from "../ui";

export default function Notifications() {
  const { user } = useAuth();
  const [rows, setRows] = useState([]);
  const [prefs, setPrefs] = useState({ email: false, sms: false, inApp: true });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const manager = user.role !== "TEACHER";

  async function load() {
    setRows(await api("/api/notifications"));
    setPrefs(await api("/api/notifications/preferences"));
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, []);

  async function savePrefs(next) {
    setPrefs(next);
    await api("/api/notifications/preferences", { method: "PUT", body: next });
  }

  async function scan() {
    setError("");
    try {
      const result = await api("/api/documents/scan-missing", { method: "POST" });
      setOk(result.count + " teachers were reminded about a missing lesson plan.");
      await load();
    } catch (err) { setError(err.message); }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Notifications</h2>
          <p>Alerts for absence, late arrival, missing documents, and approval decisions.</p>
        </div>
        <button className="btn ghost" onClick={() => api("/api/notifications/read-all", { method: "POST" }).then(load)}>Mark all read</button>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <div className="grid two">
        <section className="card">
          <h3>Inbox</h3>
          <div className="list">
            {rows.map((row) => (
              <article key={row.id} className={"priority-" + row.priority.toLowerCase()} style={{ opacity: row.read ? 0.65 : 1 }}>
                <b>{row.title}</b> <span className="lead">{label(row.priority)} · {label(row.type)} · {when(row.createdAt)}</span>
                <div>{row.message}</div>
                {user.role === "ADMIN" && <div className="lead">To {row.userName}</div>}
                {!row.read && <button className="btn ghost" onClick={() => api("/api/notifications/" + row.id + "/read", { method: "POST" }).then(load)}>Mark read</button>}
              </article>
            ))}
            {!rows.length && <p>No alerts.</p>}
          </div>
        </section>
        <section className="card">
          <h3>Delivery preferences</h3>
          <p className="lead">In this student setup, alerts are shown inside the app. Email and SMS choices are saved for when a provider is connected.</p>
          <label className="check"><input type="checkbox" checked={prefs.inApp} onChange={(e) => savePrefs({ ...prefs, inApp: e.target.checked })} /> In-app</label>
          <label className="check"><input type="checkbox" checked={prefs.email} onChange={(e) => savePrefs({ ...prefs, email: e.target.checked })} /> Email</label>
          <label className="check"><input type="checkbox" checked={prefs.sms} onChange={(e) => savePrefs({ ...prefs, sms: e.target.checked })} /> SMS</label>
          {manager && <button className="btn" style={{ marginTop: 16 }} onClick={scan}>Check missing lesson plans</button>}
        </section>
      </div>
    </div>
  );
}
