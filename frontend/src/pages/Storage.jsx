import { useEffect, useState } from "react";
import { api, download } from "../api";
import { ErrorNote, OkNote, Table } from "../ui";

export default function Storage() {
  const [data, setData] = useState(null);
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  async function load() {
    setData(await api("/api/storage"));
    setRows(await api("/api/academic"));
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, []);

  async function upload(e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    try {
      const result = await api("/api/academic/import", { method: "POST", form });
      setOk("Imported " + result.imported + " academic rows.");
      await load();
    } catch (err) { setError(err.message); }
  }

  async function restore(e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    try {
      const result = await api("/api/storage/restore", { method: "POST", form });
      setOk(result.message);
    } catch (err) { setError(err.message); }
  }

  const mb = data ? (data.uploadBytes / 1024 / 1024).toFixed(2) : "0";
  return (
    <div>
      <div className="page-head"><div><h2>Data storage and academic records</h2><p>The database, uploaded files, and imported marks live on this computer.</p></div></div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <div className="grid cards-3">
        <article className="card stat"><span>Database</span><b style={{ fontSize: 22 }}>{data?.database}</b><p className="lead">{data?.engine}</p></article>
        <article className="card stat"><span>Upload folder</span><b>{mb} MB</b></article>
        <article className="card stat"><span>Last academic import</span><b style={{ fontSize: 18 }}>{data?.lastImport || "Never"}</b></article>
      </div>
      <div style={{ height: 14 }} />
      <section className="card">
        <h3>What is stored</h3>
        <Table rows={(data?.tables || []).map((row, i) => ({ id: i, ...row }))} columns={[{ key: "name", label: "Table" }, { key: "rows", label: "Rows" }]} />
      </section>
      <section className="card" style={{ marginTop: 14 }}>
        <div className="row-actions">
          <h3 style={{ margin: 0 }}>Academic records</h3>
          <label className="check">
            <input type="checkbox" checked={!!data?.syncEnabled} onChange={(e) => api("/api/storage/sync", { method: "PUT", body: { enabled: e.target.checked } }).then(load)} />
            Allow import
          </label>
          <button className="btn ghost" onClick={() => download("/api/academic/export", "academic-records.csv")}>Export CSV</button>
          <label className="btn ghost">Import CSV<input hidden type="file" accept=".csv,text/csv" onChange={upload} /></label>
          <button className="btn" onClick={() => download("/api/storage/backup", "smart-teacher-backup.sql").catch((err) => setError(err.message))}>Backup database</button>
          <label className="btn ghost">Restore SQL<input hidden type="file" accept=".sql" onChange={restore} /></label>
        </div>
        <p className="lead">CSV columns: student, class, subject, term, score, teacher</p>
        <Table rows={rows} columns={[
          { key: "studentName", label: "Student" },
          { key: "className", label: "Class" },
          { key: "subject", label: "Subject" },
          { key: "termName", label: "Term" },
          { key: "score", label: "Score" },
          { key: "teacherName", label: "Teacher" }
        ]} />
      </section>
    </div>
  );
}
