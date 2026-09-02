import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import LoginPage from "./routes/LoginPage";
import LocationsPage from "./routes/LocationsPage";
import LocationDetailPage from "./routes/LocationDetailPage";
import PackingListsPage from "./routes/PackingListsPage";
import PackingListDetailPage from "./routes/PackingListDetailPage";
import SearchPage from "./routes/SearchPage";

function AppShell() {
  const { user, loading, signOut } = useAuth();

  if (loading) {
    return <div className="centered">Laster …</div>;
  }

  if (!user) {
    return <LoginPage />;
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Wherewear</h1>
        <button className="icon-btn" onClick={() => void signOut()}>
          Logg ut
        </button>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/locations" replace />} />
          <Route path="/locations" element={<LocationsPage />} />
          <Route path="/locations/:locationId" element={<LocationDetailPage />} />
          <Route path="/packing-lists" element={<PackingListsPage />} />
          <Route path="/packing-lists/:locationId/:season" element={<PackingListDetailPage />} />
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </main>

      <nav className="app-nav">
        <NavLink to="/locations" className={({ isActive }) => (isActive ? "active" : "")}>
          Steder
        </NavLink>
        <NavLink to="/packing-lists" className={({ isActive }) => (isActive ? "active" : "")}>
          Pakkelister
        </NavLink>
        <NavLink to="/search" className={({ isActive }) => (isActive ? "active" : "")}>
          Søk
        </NavLink>
      </nav>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}
