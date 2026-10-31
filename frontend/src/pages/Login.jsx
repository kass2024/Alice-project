import { useState } from "react";
import { PROJECT_NAME, PROJECT_SUB } from "../brand";
import { useAuth } from "../auth";
import { ErrorNote } from "../ui";

const DEMOS = [
  ["admin", "Admin"],
  ["patrick", "Head of Studies"],
  ["alice", "Teacher"]
];

export default function Login() {
  const { login } = useAuth();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(username, password);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <section className="login-hero">
        <h1>{PROJECT_NAME}</h1>
        <p>{PROJECT_SUB}</p>
        <p>Mark teacher attendance, check in with a phone camera, keep lesson documents, and let the Head of Studies approve them.</p>
      </section>
      <section className="login-panel">
        <form className="login-card card" onSubmit={submit}>
          <h2 style={{ fontFamily: "var(--serif)", marginTop: 0 }}>Sign in</h2>
          <ErrorNote error={error} />
          <label className="field">Username
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
          </label>
          <div style={{ height: 10 }} />
          <label className="field">Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
          </label>
          <div style={{ height: 14 }} />
          <button className="btn" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button>
          <div className="demo">
            {DEMOS.map(([name, title]) => (
              <button type="button" key={name} onClick={() => { setUsername(name); setPassword("password123"); }}>
                {title}
              </button>
            ))}
          </div>
          <p className="lead">Every demo account uses the password <b>password123</b>.</p>
        </form>
      </section>
    </div>
  );
}
