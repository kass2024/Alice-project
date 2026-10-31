import { useEffect, useState } from "react";
import { api } from "../api";
import { label } from "../brand";
import { ErrorNote, Modal, OkNote, Pill, Table } from "../ui";

const EMPTY = { fullName: "", username: "", email: "", phone: "", department: "Sciences", subject: "", staffCode: "", role: "TEACHER", password: "password123" };

export default function Users() {
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [edit, setEdit] = useState(null);
  const [perms, setPerms] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  async function load() {
    setRows(await api("/api/users"));
    const security = await api("/api/security");
    setPerms(security.permissions);
  }
  useEffect(() => { load().catch((err) => setError(err.message)); }, []);

  function set(key, value) { setForm({ ...form, [key]: value }); }

  async function create(e) {
    e.preventDefault();
    setError("");
    try {
      await api("/api/users", { method: "POST", body: form });
      setForm(EMPTY);
      setOk("Account created. Default password is password123 unless you typed another one.");
      await load();
    } catch (err) { setError(err.message); }
  }

  async function reset(id) {
    const result = await api("/api/users/" + id + "/reset-password", { method: "POST" });
    setOk(result.username + " can now sign in with " + result.password);
  }

  async function toggle(key, enabled) {
    await api("/api/security/permissions", { method: "PUT", body: { key, enabled } });
    setPerms({ ...perms, [key]: enabled });
  }

  return (
    <div>
      <div className="page-head"><div><h2>Users and access</h2><p>Add staff, assign a role, and deactivate an account.</p></div></div>
      <ErrorNote error={error} />
      <OkNote text={ok} />
      <form className="card" onSubmit={create}>
        <h3>New account</h3>
        <div className="filters">
          {["fullName", "username", "email", "phone", "department", "subject", "staffCode"].map((key) => (
            <label className="field" key={key}>{label(key)}<input value={form[key]} onChange={(e) => set(key, e.target.value)} /></label>
          ))}
          <label className="field">Role
            <select value={form.role} onChange={(e) => set("role", e.target.value)}>
              <option value="TEACHER">Teacher</option>
              <option value="HEAD_OF_STUDIES">Head of Studies</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <label className="field">Password<input value={form.password} onChange={(e) => set("password", e.target.value)} /></label>
        </div>
        <button className="btn" style={{ marginTop: 10 }}>Create account</button>
      </form>
      <div style={{ height: 14 }} />
      <Table
        rows={rows}
        columns={[
          { key: "fullName", label: "Name" },
          { key: "username", label: "Username" },
          { key: "role", label: "Role", render: (row) => label(row.role) },
          { key: "department", label: "Department" },
          { key: "active", label: "Status", render: (row) => row.active ? "Active" : "Inactive" },
          { key: "actions", label: "", render: (row) => (
            <span className="row-actions">
              <button className="btn ghost" onClick={() => setEdit(row)}>Edit</button>
              <button className="btn ghost" onClick={() => reset(row.id)}>Reset password</button>
              <button className="btn ghost" onClick={() => api("/api/users/" + row.id, { method: "PUT", body: { active: !row.active } }).then(load)}>{row.active ? "Deactivate" : "Activate"}</button>
            </span>
          ) }
        ]}
      />
      <section className="card" style={{ marginTop: 14 }}>
        <h3>Permission matrix</h3>
        {perms && [
          ["teacher.upload", "Teachers can upload documents"],
          ["teacher.biometric", "Teachers can use face check-in"],
          ["hos.approve", "Head of Studies can approve documents"]
        ].map(([key, text]) => (
          <label className="check" key={key}>
            <input type="checkbox" checked={!!perms[key]} onChange={(e) => toggle(key, e.target.checked)} /> {text}
          </label>
        ))}
      </section>
      {edit && <EditUser user={edit} onClose={() => { setEdit(null); load(); }} />}
    </div>
  );
}

function EditUser({ user, onClose }) {
  const [form, setForm] = useState(user);
  const [error, setError] = useState("");
  async function save(e) {
    e.preventDefault();
    try {
      await api("/api/users/" + user.id, { method: "PUT", body: form });
      onClose();
    } catch (err) { setError(err.message); }
  }
  return (
    <Modal title={"Edit " + user.fullName} onClose={onClose}>
      <form onSubmit={save} className="grid">
        <ErrorNote error={error} />
        <label className="field">Name<input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></label>
        <label className="field">Email<input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
        <label className="field">Phone<input value={form.phone || ""} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
        <label className="field">Department<input value={form.department || ""} onChange={(e) => setForm({ ...form, department: e.target.value })} /></label>
        <label className="field">Subject<input value={form.subject || ""} onChange={(e) => setForm({ ...form, subject: e.target.value })} /></label>
        <button className="btn">Save</button>
      </form>
    </Modal>
  );
}
