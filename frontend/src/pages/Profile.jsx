import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../auth";
import { label, when } from "../brand";
import { ErrorNote, OkNote, Pill, Table } from "../ui";

export default function Profile() {
  const { id } = useParams();
  const { user } = useAuth();
  const teacherId = id || user.id;
  const [data, setData] = useState(null);
  const [currentPassword, setCurrent] = useState("");
  const [newPassword, setNext] = useState("");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api("/api/teachers/" + teacherId + "/profile").then(setData).catch((err) => setError(err.message));
  }, [teacherId]);

  async function change(e) {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api("/api/auth/password", { method: "POST", body: { currentPassword, newPassword } });
      setOk("Password changed.");
      setCurrent(""); setNext("");
    } catch (err) { setError(err.message); }
  }

  const teacher = data?.teacher;
  const summary = data?.summary || {};
  return (
    <div>
      <div className="page-head">
        <div>
          <h2>{teacher?.fullName || "Profile"}</h2>
          <p>{teacher ? `${label(teacher.role)} · ${teacher.department || ""} · ${teacher.subject || ""} · ${teacher.staffCode || ""}` : ""}</p>
        </div>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <div className="grid kpis">
        <article className="card stat good"><span>Present</span><b>{summary.present ?? 0}</b></article>
        <article className="card stat bad"><span>Absent</span><b>{summary.absent ?? 0}</b></article>
        <article className="card stat warn"><span>Late</span><b>{summary.late ?? 0}</b></article>
        <article className="card stat"><span>On leave</span><b>{summary.onLeave ?? 0}</b></article>
      </div>
      <div className="grid two" style={{ marginTop: 14 }}>
        <section className="card">
          <h3>Attendance history</h3>
          <Table rows={data?.attendance || []} columns={[
            { key: "date", label: "Date" },
            { key: "status", label: "Status", render: (row) => <Pill value={row.status} /> },
            { key: "method", label: "Method", render: (row) => label(row.method) },
            { key: "checkIn", label: "In", render: (row) => row.checkIn ? when(row.checkIn) : "—" }
          ]} />
        </section>
        <section>
          <div className="card">
            <h3>Documents</h3>
            {(data?.documents || []).map((doc) => (
              <div key={doc.id} style={{ display: "flex", justifyContent: "space-between", gap: 8, padding: "8px 0" }}>
                <span>{doc.title}</span><Pill value={doc.status} />
              </div>
            ))}
            {!data?.documents?.length && <p className="lead">No documents yet.</p>}
          </div>
          {String(teacherId) === String(user.id) && (
            <form className="card" style={{ marginTop: 14 }} onSubmit={change}>
              <h3>Change password</h3>
              <label className="field">Current<input type="password" value={currentPassword} onChange={(e) => setCurrent(e.target.value)} /></label>
              <label className="field">New<input type="password" value={newPassword} onChange={(e) => setNext(e.target.value)} /></label>
              <button className="btn" style={{ marginTop: 10 }}>Update password</button>
            </form>
          )}
        </section>
      </div>
    </div>
  );
}
