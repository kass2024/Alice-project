import { useEffect, useState } from "react";
import { api } from "../api";
import { when } from "../brand";
import { ErrorNote, Table } from "../ui";

export default function SecurityPage() {
  const [data, setData] = useState(null);
  const [q, setQ] = useState("");
  const [error, setError] = useState("");

  async function load(query = q) {
    setData(await api("/api/security?q=" + encodeURIComponent(query)));
  }
  useEffect(() => { load("").catch((err) => setError(err.message)); }, []);

  async function toggle(key, enabled) {
    await api("/api/security/permissions", { method: "PUT", body: { key, enabled } });
    await load();
  }

  const perms = data?.permissions || {};
  return (
    <div>
      <div className="page-head"><div><h2>Security and privacy</h2><p>Passwords are hashed. Each sign-in gets its own session, and important actions are written to the audit log.</p></div></div>
      <ErrorNote error={error} />
      <div className="grid cards-3">
        <article className="card"><h3>Passwords</h3><p>{data?.passwordStorage}</p></article>
        <article className="card"><h3>Sessions</h3><p>{data?.token}</p><p className="lead">{data?.activeSessions ?? 0} active</p></article>
        <article className="card"><h3>Files and database</h3><p>{data?.files}</p><p className="lead">{data?.database}</p><p className="lead">{data?.https}</p></article>
      </div>
      <section className="card" style={{ marginTop: 14 }}>
        <h3>Access control</h3>
        {[
          ["teacher.upload", "Teachers can upload documents"],
          ["teacher.biometric", "Teachers can use face check-in"],
          ["hos.approve", "Head of Studies can approve"]
        ].map(([key, text]) => (
          <label className="check" key={key}>
            <input type="checkbox" checked={!!perms[key]} onChange={(e) => toggle(key, e.target.checked)} /> {text}
          </label>
        ))}
      </section>
      <section className="card" style={{ marginTop: 14 }}>
        <h3>Sessions</h3>
        <Table rows={data?.sessions || []} columns={[
          { key: "userName", label: "User" },
          { key: "ipAddress", label: "IP" },
          { key: "active", label: "Active", render: (row) => row.active ? "Yes" : "Ended" },
          { key: "createdAt", label: "Started", render: (row) => when(row.createdAt) },
          { key: "end", label: "", render: (row) => row.active ? <button className="btn ghost" onClick={() => api("/api/security/sessions/" + row.id + "/end", { method: "POST" }).then(() => load())}>End</button> : "—" }
        ]} />
      </section>
      <section className="card" style={{ marginTop: 14 }}>
        <div className="row-actions">
          <h3 style={{ margin: 0 }}>Audit log</h3>
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search action" />
          <button className="btn ghost" onClick={() => load(q)}>Search</button>
        </div>
        <Table rows={data?.audit || []} columns={[
          { key: "createdAt", label: "When", render: (row) => when(row.createdAt) },
          { key: "user", label: "User" },
          { key: "action", label: "Action" },
          { key: "detail", label: "Detail" },
          { key: "ipAddress", label: "IP" }
        ]} />
      </section>
    </div>
  );
}
