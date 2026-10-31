import { useEffect, useState } from "react";
import { api, download } from "../api";
import { label, when } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote, Modal, OkNote, Pill, Table } from "../ui";

const CATEGORIES = ["LESSON_PLAN", "SCHEME_OF_WORK", "CLASS_REGISTER", "REPORT", "SYLLABUS", "WEEKS_BREAKDOWN", "CURRICULUM", "CHRONOGRAM"];

function categoriesFor(track) {
  const shared = ["LESSON_PLAN", "SCHEME_OF_WORK", "CLASS_REGISTER", "REPORT"];
  if (track === "REB") return shared.concat(["SYLLABUS", "WEEKS_BREAKDOWN"]);
  if (track === "TVET") return shared.concat(["CURRICULUM", "CHRONOGRAM"]);
  return CATEGORIES;
}

export default function Documents() {
  const { user } = useAuth();
  const [rows, setRows] = useState([]);
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [track, setTrack] = useState("ALL");
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState("");
  const [newCategory, setNewCategory] = useState("LESSON_PLAN");
  const [newTrack, setNewTrack] = useState("GENERAL");
  const [selected, setSelected] = useState([]);
  const [open, setOpen] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  async function load() {
    const data = await api(`/api/documents?q=${encodeURIComponent(q)}&category=${category}&status=${status}&track=${track}`);
    setRows(data);
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, [q, category, status, track]);

  async function upload(e) {
    e.preventDefault();
    setError(""); setOk("");
    const form = new FormData();
    form.append("title", title);
    form.append("category", newCategory);
    form.append("track", newTrack);
    form.append("file", file);
    try {
      await api("/api/documents", { method: "POST", form });
      setOk("Document saved as a draft.");
      setTitle(""); setFile(null);
      await load();
    } catch (err) { setError(err.message); }
  }

  async function bulk(action) {
    setError("");
    try {
      await api("/api/documents/bulk", { method: "POST", body: { ids: selected, action } });
      setSelected([]);
      setOk(action === "delete" ? "Deleted." : "Submitted.");
      await load();
    } catch (err) { setError(err.message); }
  }

  function toggle(id) {
    setSelected((curr) => curr.includes(id) ? curr.filter((x) => x !== id) : [...curr, id]);
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Document repository</h2>
          <p>Lesson plans, schemes of work, class registers, and reports.</p>
        </div>
      </div>
      <form className="card" onSubmit={upload}>
        <h3>Upload</h3>
        <div className="filters">
          <label className="field">Title<input value={title} onChange={(e) => setTitle(e.target.value)} required /></label>
          <label className="field">Type
            <select value={newCategory} onChange={(e) => setNewCategory(e.target.value)}>
              {categoriesFor(newTrack).map((c) => <option key={c} value={c}>{label(c)}</option>)}
            </select>
          </label>
          <label className="field">Track
            <select value={newTrack} onChange={(e) => {
              const next = e.target.value;
              setNewTrack(next);
              if (!categoriesFor(next).includes(newCategory)) setNewCategory("LESSON_PLAN");
            }}>
              <option value="GENERAL">General</option>
              <option value="REB">REB</option>
              <option value="TVET">TVET</option>
            </select>
          </label>
        </div>
        <label className="drop" style={{ marginTop: 12 }}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => { e.preventDefault(); setFile(e.dataTransfer.files[0]); }}>
          <input type="file" onChange={(e) => setFile(e.target.files[0])} />
          <strong>{file ? file.name : "Drop a file here or browse"}</strong>
          PDF, DOCX, TXT, or a picture
        </label>
        <button className="btn" style={{ marginTop: 12 }} disabled={!file}>Save draft</button>
      </form>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <div className="filters" style={{ margin: "12px 0" }}>
        <label className="field">Search<input value={q} onChange={(e) => setQ(e.target.value)} /></label>
        <label className="field">Type
          <select value={category} onChange={(e) => setCategory(e.target.value)}><option>ALL</option>{CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select>
        </label>
        <label className="field">Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option>ALL</option>
            {["DRAFT", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED", "RETURNED"].map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </label>
        <label className="field">Track
          <select value={track} onChange={(e) => setTrack(e.target.value)}><option>ALL</option><option>REB</option><option>TVET</option><option>GENERAL</option></select>
        </label>
        <button className="btn ghost" disabled={!selected.length} onClick={() => bulk("submit")}>Submit selected</button>
        <button className="btn danger" disabled={!selected.length} onClick={() => bulk("delete")}>Delete selected</button>
      </div>
      <Table
        rows={rows}
        columns={[
          { key: "pick", label: "", render: (row) => <input type="checkbox" checked={selected.includes(row.id)} onChange={() => toggle(row.id)} /> },
          { key: "title", label: "Title" },
          { key: "category", label: "Type", render: (row) => label(row.category) },
          { key: "track", label: "Track" },
          { key: "ownerName", label: "Owner" },
          { key: "status", label: "Status", render: (row) => <Pill value={row.status} /> },
          { key: "version", label: "Version" },
          { key: "updatedAt", label: "Updated", render: (row) => when(row.updatedAt || row.createdAt) },
          { key: "open", label: "", render: (row) => <button className="btn ghost" onClick={() => api("/api/documents/" + row.id).then(setOpen)}>Open</button> }
        ]}
      />
      {open && <DocModal doc={open} me={user} onClose={() => { setOpen(null); load(); }} />}
    </div>
  );
}

function DocModal({ doc, me, onClose }) {
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const canReview = me.role === "ADMIN" || me.role === "HEAD_OF_STUDIES";

  async function act(action) {
    try {
      if (action === "submit") await api("/api/documents/" + doc.id + "/submit", { method: "POST" });
      else await api("/api/approvals/" + doc.id, { method: "POST", body: { action, comment } });
      onClose();
    } catch (err) { setError(err.message); }
  }

  return (
    <Modal wide title={doc.title} onClose={onClose}>
      <p>{label(doc.category)} · {doc.track} · v{doc.version} · routed to {doc.routedTo || "Head of Studies"}</p>
      <Pill value={doc.status} />
      <ErrorNote error={error} />
      <div className="row-actions" style={{ margin: "12px 0" }}>
        <button className="btn ghost" onClick={() => download("/api/documents/" + doc.id + "/file", doc.originalName || "document")}>Download</button>
        {(doc.status === "DRAFT" || doc.status === "RETURNED" || doc.status === "REJECTED") && <button className="btn" onClick={() => act("submit")}>Submit</button>}
        {canReview && ["UNDER_REVIEW", "APPROVED", "REJECTED", "RETURNED"].map((action) => (
          <button key={action} className="btn ghost" onClick={() => act(action)}>{label(action)}</button>
        ))}
      </div>
      {canReview && <label className="field">Review comment<textarea value={comment} onChange={(e) => setComment(e.target.value)} /></label>}
      {doc.reviewerComment && <p className="note info">{doc.reviewerComment}</p>}
      <h3>Version history</h3>
      <div className="timeline">
        {(doc.versions || []).map((v) => <div key={v.id}><b>Version {v.version}</b><div className="lead">{v.note} · {when(v.createdAt)}</div></div>)}
      </div>
      <h3>Approval history</h3>
      <div className="timeline">
        {(doc.approvals || []).length === 0 && <div className="lead">No review yet.</div>}
        {(doc.approvals || []).map((a) => <div key={a.id}><b>{label(a.action)}</b> by {a.reviewer}<div className="lead">{a.comment} · {when(a.createdAt)}</div></div>)}
      </div>
    </Modal>
  );
}
