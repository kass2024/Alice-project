import { useMemo, useState } from "react";
import { label } from "./brand";

export function Pill({ value }) {
  const key = String(value || "").toLowerCase().replace(/[^a-z]/g, "");
  return <span className={"pill " + key}>{label(value)}</span>;
}

export function Table({ columns, rows, empty = "Nothing to show yet." }) {
  const [sort, setSort] = useState(null);
  const shown = useMemo(() => {
    if (!sort) return rows || [];
    return [...(rows || [])].sort((a, b) => {
      const av = String(a[sort.key] ?? "").toLowerCase();
      const bv = String(b[sort.key] ?? "").toLowerCase();
      if (av < bv) return sort.dir === "asc" ? -1 : 1;
      if (av > bv) return sort.dir === "asc" ? 1 : -1;
      return 0;
    });
  }, [rows, sort]);

  function toggle(key) {
    setSort((current) => {
      if (!current || current.key !== key) return { key, dir: "asc" };
      if (current.dir === "asc") return { key, dir: "desc" };
      return null;
    });
  }

  if (!shown.length) return <div className="empty">{empty}</div>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} onClick={() => toggle(col.key)}>
                {col.label}
                {sort?.key === col.key ? (sort.dir === "asc" ? " ↑" : " ↓") : ""}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {shown.map((row, index) => (
            <tr key={row.id ?? index}>
              {columns.map((col) => (
                <td key={col.key}>{col.render ? col.render(row) : row[col.key] ?? "—"}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Field({ label: text, children }) {
  return (
    <label className="field">
      <span>{text}</span>
      {children}
    </label>
  );
}

export function Modal({ title, onClose, children, wide }) {
  return (
    <div className="modal-back" onMouseDown={onClose}>
      <div className={"modal " + (wide ? "wide" : "")} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="btn ghost" onClick={onClose}>Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ErrorNote({ error }) {
  if (!error) return null;
  return <div className="note bad">{error}</div>;
}

export function OkNote({ text }) {
  if (!text) return null;
  return <div className="note good">{text}</div>;
}
