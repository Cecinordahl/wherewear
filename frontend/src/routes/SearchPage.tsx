import { useState } from "react";
import { searchApi } from "../api/endpoints";
import type { SearchResult } from "../types";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const runSearch = async (q: string) => {
    setQuery(q);
    if (!q.trim()) {
      setResults(null);
      return;
    }
    setError(null);
    try {
      setResults(await searchApi.search(q.trim()));
    } catch {
      setError("Søket feilet.");
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
