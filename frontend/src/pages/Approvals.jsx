import { useEffect, useState } from "react";
import { api } from "../api";
import { label, when } from "../brand";
import { ErrorNote, OkNote, Pill, Table } from "../ui";

export default function Approvals() {
  const [status, setStatus] = useState("QUEUE");
  const [rows, setRows] = useState([]);
  const [selected, setSelected] = useState([]);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  async function load() {
    setRows(await api("/api/approvals?status=" + status));
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, [status]);

  async function one(id, action) {
    setError("");
    try {
      await api("/api/approvals/" + id, { method: "POST", body: { action, comment } });
      setOk(label(action) + ".");
      setSelected([]);
      await load();
    } catch (err) { setError(err.message); }
  }

  async function many(action) {
    setError("");
    try {
      await api("/api/approvals/bulk", { method: "POST", body: { ids: selected, action, comment } });
      setOk("Updated " + selected.length + " documents.");
      setSelected([]);
      await load();
    } catch (err) { setError(err.message); }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Approval queue</h2>
          <p>The Head of Studies reviews submitted teaching documents.</p>
        </div>
      </div>
      <div className="filters">
        <label className="field">Show
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="QUEUE">Waiting</option>
            {["SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED", "RETURNED"].map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </label>
        <label className="field" style={{ flex: 1 }}>Comment for the teacher
          <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Write the reason" />
        </label>
        <button className="btn ghost" disabled={!selected.length} onClick={() => many("UNDER_REVIEW")}>Mark under review</button>
        <button className="btn" disabled={!selected.length} onClick={() => many("APPROVED")}>Approve selected</button>
        <button className="btn danger" disabled={!selected.length} onClick={() => many("REJECTED")}>Reject selected</button>
      </div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <Table
        rows={rows}
        columns={[
          { key: "pick", label: "", render: (row) => <input type="checkbox" checked={selected.includes(row.id)} onChange={() => setSelected((c) => c.includes(row.id) ? c.filter((x) => x !== row.id) : [...c, row.id])} /> },
          { key: "title", label: "Document" },
          { key: "ownerName", label: "Teacher" },
          { key: "category", label: "Type", render: (row) => label(row.category) },
          { key: "status", label: "Status", render: (row) => <Pill value={row.status} /> },
          { key: "routedTo", label: "Routed to" },
          { key: "updatedAt", label: "Updated", render: (row) => when(row.updatedAt) },
          { key: "actions", label: "", render: (row) => (
            <span className="row-actions">
              <button className="btn ghost" onClick={() => one(row.id, "APPROVED")}>Approve</button>
              <button className="btn ghost" onClick={() => one(row.id, "RETURNED")}>Return</button>
            </span>
          ) }
        ]}
        empty="The queue is clear."
      />
    </div>
  );
}
