import { useEffect, useState, type ReactNode } from "react";
import { checkBackendHealth } from "../api/health";

const POLL_INTERVAL_MS = 3000;
const PER_ATTEMPT_TIMEOUT_MS = 8000;
const MAX_WAIT_MS = 90000;

/**
 * Blocks the app behind a friendly waiting screen until the backend
 * actually responds. Render's free tier sleeps after ~15 min idle and can
 * take up to a minute to wake on the next request - without this, a form
 * submit during that window either hangs with no explanation or fails with
 * a generic "couldn't save" error, and (confusingly) can appear to succeed
 * moments later once the backend wakes up mid-retry.
 */
export default function BackendWakeGate({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<"checking" | "ready" | "failed">("checking");
  const [waitedSeconds, setWaitedSeconds] = useState(0);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const start = Date.now();

    const tick = async () => {
      if (cancelled) return;
      const ok = await checkBackendHealth(PER_ATTEMPT_TIMEOUT_MS);
      if (cancelled) return;
      if (ok) {
        setStatus("ready");
        return;
      }
      const elapsed = Date.now() - start;
      setWaitedSeconds(Math.round(elapsed / 1000));
      if (elapsed > MAX_WAIT_MS) {
        setStatus("failed");
        return;
      }
      setTimeout(() => void tick(), POLL_INTERVAL_MS);
    };

    void tick();
    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  if (status === "ready") {
    return <>{children}</>;
  }

  if (status === "failed") {
    return (
      <div className="centered">
        <div className="stack" style={{ alignItems: "center" }}>
          <p>Fikk ikke kontakt med serveren.</p>
          <p className="card-subtitle">
            Den kan være nede, eller bruke uvanlig lang tid på å starte.
          </p>
          <button
            className="btn"
            onClick={() => {
              setStatus("checking");
              setWaitedSeconds(0);
              setRetryKey((k) => k + 1);
            }}
          >
            Prøv igjen
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="centered">
      <div className="stack" style={{ alignItems: "center" }}>
        <p>Vekker serveren …</p>
        <p className="card-subtitle">
          Kan ta opptil ett minutt siden appen ikke har vært i bruk nylig.
          {waitedSeconds > 0 && ` (${waitedSeconds}s)`}
        </p>
      </div>
    </div>
  );
}
