import { useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "./auth";
import Shell from "./layout/Shell";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Attendance from "./pages/Attendance";
import Biometric from "./pages/Biometric";
import Documents from "./pages/Documents";
import Analysis from "./pages/Analysis";
import Approvals from "./pages/Approvals";
import Teaching from "./pages/Teaching";
import Reports from "./pages/Reports";
import Notifications from "./pages/Notifications";
import Users from "./pages/Users";
import Storage from "./pages/Storage";
import SecurityPage from "./pages/SecurityPage";
import Profile from "./pages/Profile";

function Gate({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <Shell open={open} setOpen={setOpen}>{children}</Shell>;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/" element={<Gate><Dashboard /></Gate>} />
      <Route path="/attendance" element={<Gate><Attendance /></Gate>} />
      <Route path="/biometric" element={<Gate><Biometric /></Gate>} />
      <Route path="/documents" element={<Gate><Documents /></Gate>} />
      <Route path="/analysis" element={<Gate><Analysis /></Gate>} />
      <Route path="/approvals" element={<Gate roles={["ADMIN", "HEAD_OF_STUDIES"]}><Approvals /></Gate>} />
      <Route path="/teaching" element={<Gate><Teaching /></Gate>} />
      <Route path="/reports" element={<Gate roles={["ADMIN", "HEAD_OF_STUDIES"]}><Reports /></Gate>} />
      <Route path="/notifications" element={<Gate><Notifications /></Gate>} />
      <Route path="/users" element={<Gate roles={["ADMIN"]}><Users /></Gate>} />
      <Route path="/storage" element={<Gate roles={["ADMIN"]}><Storage /></Gate>} />
      <Route path="/security" element={<Gate roles={["ADMIN"]}><SecurityPage /></Gate>} />
      <Route path="/profile" element={<Gate><Profile /></Gate>} />
      <Route path="/profile/:id" element={<Gate><Profile /></Gate>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
