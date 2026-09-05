import { useRef, useState } from "react";
import { searchApi } from "../api/endpoints";
import type { SearchResult } from "../types";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Guards against out-of-order responses: typing fast fires a request per
  // keystroke with no cancellation, so a broader earlier query (e.g. "sk",
  // which matches nearly everything) can resolve after the narrower "sko"
  // one and silently overwrite its correct results with stale, wider ones.
  const latestQuery = useRef("");

  const runSearch = async (q: string) => {
    setQuery(q);
    latestQuery.current = q;
    if (!q.trim()) {
      setResults(null);
      return;
    }
    setError(null);
    try {
      const found = await searchApi.search(q.trim());
      if (latestQuery.current === q) {
        setResults(found);
      }
    } catch {
      if (latestQuery.current === q) {
        setError("Søket feilet.");
      }
    }
  };

  return (
    <div>
      <div className="kicker">Finn fram</div>
      <h2>Søk</h2>

      <input
        type="text"
        placeholder="Hvor er skistavene mine?"
        value={query}
        onChange={(e) => void runSearch(e.target.value)}
        autoFocus
      />

      {error && <div className="error-banner">{error}</div>}

      <div className="stack" style={{ marginTop: "0.75rem" }}>
        {results?.map((r) => (
          <div key={r.itemId} className="card">
            <p className="card-title">{r.itemName}</p>
            <p className="card-subtitle">
              {r.locationName} · {r.category}
            </p>
          </div>
        ))}
        {results?.length === 0 && <p className="empty-state">Fant ingenting.</p>}
      </div>
    </div>
  );
}
