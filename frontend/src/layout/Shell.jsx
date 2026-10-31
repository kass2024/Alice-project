import { NavLink, useNavigate } from "react-router-dom";
import { PROJECT_NAME, PROJECT_SUB } from "../brand";
import { useAuth } from "../auth";

const GROUPS = [
  {
    label: "Overview",
    items: [{ to: "/", label: "Dashboard", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] }]
  },
  {
    label: "Attendance",
    items: [
      { to: "/attendance", label: "Attendance", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] },
      { to: "/biometric", label: "Biometric", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] }
    ]
  },
  {
    label: "Documents",
    items: [
      { to: "/documents", label: "Repository", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] },
      { to: "/analysis", label: "AI analysis", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] },
      { to: "/approvals", label: "Approvals", roles: ["ADMIN", "HEAD_OF_STUDIES"] }
    ]
  },
  {
    label: "Teaching",
    items: [
      { to: "/teaching", label: "Class activity", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] },
      { to: "/reports", label: "Reports", roles: ["ADMIN", "HEAD_OF_STUDIES"] },
      { to: "/notifications", label: "Notifications", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] }
    ]
  },
  {
    label: "Administration",
    items: [
      { to: "/users", label: "Users", roles: ["ADMIN"] },
      { to: "/storage", label: "Storage", roles: ["ADMIN"] },
      { to: "/security", label: "Security", roles: ["ADMIN"] },
      { to: "/profile", label: "My profile", roles: ["ADMIN", "HEAD_OF_STUDIES", "TEACHER"] }
    ]
  }
];

export default function Shell({ open, setOpen, children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div className="shell">
      <aside className={open ? "sidebar open" : "sidebar"}>
        <div className="brand-mark">
          <div className="mark">ST</div>
          <div>
            <strong>Smart Teacher</strong>
            <span>{user?.fullName}</span>
          </div>
        </div>
        {GROUPS.map((group) => {
          const items = group.items.filter((item) => item.roles.includes(user?.role));
          if (!items.length) return null;
          return (
            <div key={group.label}>
              <div className="nav-label">{group.label}</div>
              {items.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.to === "/"} onClick={() => setOpen(false)}
                  className={({ isActive }) => "nav-btn" + (isActive ? " active" : "")}>
                  {item.label}
                </NavLink>
              ))}
            </div>
          );
        })}
        <button className="logout" onClick={async () => { await logout(); navigate("/login"); }}>Sign out</button>
      </aside>
      <div className="main">
        <header className="topbar">
          <button className="btn ghost menu" onClick={() => setOpen((v) => !v)}>Menu</button>
          <h1>{PROJECT_NAME}</h1>
          <p>{PROJECT_SUB}</p>
        </header>
        <div className="content">{children}</div>
      </div>
    </div>
  );
}
