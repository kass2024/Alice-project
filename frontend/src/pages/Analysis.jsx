import { useState } from "react";
import { api } from "../api";
import { ErrorNote, OkNote } from "../ui";

export default function Analysis() {
  const [track, setTrack] = useState("REB");
  const [subject, setSubject] = useState("Mathematics");
  const [className, setClassName] = useState("S2 A");
  const [primary, setPrimary] = useState(null);
  const [secondary, setSecondary] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [busy, setBusy] = useState(false);
  const reb = track === "REB";

  async function loadSample() {
    const names = reb ? ["reb-syllabus.txt", "reb-weeks.txt"] : ["tvet-curriculum.txt", "tvet-chronogram.txt"];
    const files = [];
    for (const name of names) {
      const res = await fetch("/samples/" + name);
      files.push(new File([await res.blob()], name, { type: "text/plain" }));
    }
    setPrimary(files[0]);
    setSecondary(files[1]);
    setSubject(reb ? "Mathematics" : "Computer Applications");
    setClassName(reb ? "S2 A" : "Level 3");
  }

  async function analyze(e) {
    e.preventDefault();
    setBusy(true); setError(""); setOk(""); setResult(null);
    const form = new FormData();
    form.append("track", track);
    form.append("subject", subject);
    form.append("className", className);
    form.append("primary", primary);
    if (secondary) form.append("secondary", secondary);
    try {
      setResult(await api("/api/documents/analyze", { method: "POST", form }));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    setError("");
    try {
      const saved = await api("/api/documents/scheme", {
        method: "POST",
        body: {
          title: (subject || "Scheme") + " scheme of work",
          track, subject, className, analysis: result
        }
      });
      setOk("Saved in the repository as a draft: " + saved.title);
    } catch (err) { setError(err.message); }
  }

  const scheme = Array.isArray(result?.scheme) ? result.scheme : [];
  const items = Array.isArray(result?.items) ? result.items : [];

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>Pedagogical document analysis</h2>
          <p>Gemini reads the two source documents and drafts a scheme of work. REB uses a Syllabus and a Weeks breakdown. TVET uses a Curriculum and a Chronogram.</p>
        </div>
      </div>
      <div className="tabs" style={{ marginBottom: 12 }}>
        <button className={reb ? "on" : ""} onClick={() => setTrack("REB")}>REB</button>
        <button className={!reb ? "on" : ""} onClick={() => setTrack("TVET")}>TVET</button>
      </div>
      <form className="card" onSubmit={analyze}>
        <div className="filters">
          <label className="field">Subject<input value={subject} onChange={(e) => setSubject(e.target.value)} /></label>
          <label className="field">Class<input value={className} onChange={(e) => setClassName(e.target.value)} /></label>
          <button type="button" className="btn ghost" onClick={loadSample}>Load sample files</button>
        </div>
        <div className="grid two" style={{ marginTop: 12 }}>
          <Drop label={reb ? "Syllabus" : "Curriculum"} file={primary} setFile={setPrimary} />
          <Drop label={reb ? "Weeks breakdown" : "Chronogram"} file={secondary} setFile={setSecondary} />
        </div>
        <button className="btn" style={{ marginTop: 12 }} disabled={busy || !primary}>{busy ? "Gemini is reading…" : "Analyse"}</button>
      </form>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      {result && (
        <section className="card" style={{ marginTop: 14 }}>
          <h3>Summary</h3>
          <p>{result.summary}</p>
          <p className="lead">{result.primaryLabel}: {result.primaryChars} characters. {result.secondaryLabel}: {result.secondaryChars} characters. Model: {result.model}</p>
          <h3>{reb ? "Units" : "Modules"}</h3>
          {items.map((item, i) => (
            <article key={i} style={{ marginBottom: 8 }}>
              <b>{item.code} {item.title}</b>
              <div className="lead">{(item.outcomes || []).join(" · ")}</div>
            </article>
          ))}
          <h3>Scheme of work</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>{["week", "unit", "lesson", "objectives", "activities", "assessment", "hours"].map((h) => <th key={h}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {scheme.map((row, i) => (
                  <tr key={i}>
                    <td>{row.week}</td><td>{row.unit}</td><td>{row.lesson}</td><td>{row.objectives}</td>
                    <td>{row.activities}</td><td>{row.assessment}</td><td>{row.hours}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button className="btn" style={{ marginTop: 12 }} onClick={save}>Save scheme to repository</button>
        </section>
      )}
    </div>
  );
}

function Drop({ label, file, setFile }) {
  return (
    <label className="drop" onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); setFile(e.dataTransfer.files[0]); }}>
      <input type="file" accept=".pdf,.docx,.txt" onChange={(e) => setFile(e.target.files[0])} />
      <strong>{label}</strong>
      {file ? file.name : "Drop a PDF, DOCX, or TXT file"}
    </label>
  );
}
