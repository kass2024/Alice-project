import { createContext, useContext, useEffect, useState } from "react";
import { api } from "./api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("user");
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (!localStorage.getItem("token")) return;
    api("/api/auth/me").then((fresh) => {
      localStorage.setItem("user", JSON.stringify(fresh));
      setUser(fresh);
    }).catch(() => {});
  }, []);

  async function login(username, password) {
    const data = await api("/api/auth/login", { method: "POST", body: { username, password } });
    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(data.user));
    setUser(data.user);
  }

  async function logout() {
    try { await api("/api/auth/logout", { method: "POST" }); } catch { /* session may already be closed */ }
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  }

  function refresh(next) {
    localStorage.setItem("user", JSON.stringify(next));
    setUser(next);
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
