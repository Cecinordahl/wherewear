import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { signIn } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const handleSignIn = async () => {
    setError(null);
    try {
      await signIn();
    } catch {
      setError("Innlogging feilet. Prøv igjen.");
    }
  };

  return (
    <div className="centered">
      <div className="stack" style={{ alignItems: "center" }}>
        <h1>Wherewear</h1>
        <p style={{ color: "var(--text-muted)" }}>
          Hold oversikt over hva du har hvor, og lag pakkelister.
        </p>
        {error && <div className="error-banner">{error}</div>}
        <button className="btn" onClick={() => void handleSignIn()}>
          Logg inn med Google
        </button>
      </div>
    </div>
  );
}
