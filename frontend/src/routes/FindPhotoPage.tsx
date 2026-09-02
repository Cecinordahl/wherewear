import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { itemsApi, productLookupApi } from "../api/endpoints";
import type { ProductCandidate } from "../types";

const DEBOUNCE_MS = 500;

// This page always shows a photo per candidate, so only keep ones that have
// one (in practice always true here - the underlying searches always
// require an image).
type PhotoCandidate = ProductCandidate & { imageUrl: string };
function hasImage(c: ProductCandidate): c is PhotoCandidate {
  return c.imageUrl !== null;
}

export default function FindPhotoPage() {
  const { locationId, itemId } = useParams<{ locationId: string; itemId: string }>();
  const navigate = useNavigate();

  const [query, setQuery] = useState("");
  const [candidates, setCandidates] = useState<PhotoCandidate[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const runSearch = async (searcher: () => Promise<ProductCandidate[]>) => {
    setLoading(true);
    setError(null);
    try {
      setCandidates((await searcher()).filter(hasImage));
    } catch (err) {
      setError(describeError(err));
      setCandidates(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!query.trim()) {
      setCandidates(null);
      return;
    }
    debounceRef.current = setTimeout(() => {
      void runSearch(() => productLookupApi.searchByText(query.trim()));
    }, DEBOUNCE_MS);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setQuery("");
    void runSearch(() => productLookupApi.searchByPhoto(file));
  };

  const handlePick = async (candidate: PhotoCandidate) => {
    if (!itemId) return;
    setSaving(true);
    setError(null);
    try {
      await itemsApi.setPhoto(itemId, candidate.imageUrl);
      navigate(`/locations/${locationId}`);
    } catch (err) {
      setError(describeError(err));
      setSaving(false);
    }
  };

  return (
    <div>
      <Link to={`/locations/${locationId}`} className="card-subtitle">
        ← Tilbake
      </Link>
      <h2 style={{ marginTop: "0.4rem" }}>Finn produktbilde</h2>

      {error && <div className="error-banner">{error}</div>}

      <div className="stack card">
        <input
          type="text"
          placeholder="Lim inn eller skriv produktnavn, f.eks. H&M ribbestrikket genser"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <div className="row-between">
          <span className="card-subtitle">eller</span>
          <button className="btn secondary" onClick={() => fileInputRef.current?.click()}>
            Last opp bilde
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ display: "none" }}
            onChange={handleFileChange}
          />
        </div>
      </div>

      {loading && <p className="empty-state">Søker …</p>}
      {saving && <p className="empty-state">Lagrer bilde …</p>}

      {candidates !== null && !loading && candidates.length === 0 && (
        <p className="empty-state">Fant ingen treff. Prøv et annet søk.</p>
      )}

      {candidates && candidates.length > 0 && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(2, 1fr)",
            gap: "0.6rem",
            marginTop: "0.75rem",
          }}
        >
          {candidates.map((c, i) => (
            <button
              key={i}
              className="card"
              style={{ padding: "0.5rem", textAlign: "left", cursor: "pointer" }}
              onClick={() => void handlePick(c)}
              disabled={saving}
            >
              <img
                src={c.imageUrl}
                alt={c.title}
                style={{ width: "100%", aspectRatio: "1", objectFit: "cover", borderRadius: "8px" }}
              />
              <p className="card-title" style={{ fontSize: "0.8rem", marginTop: "0.4rem" }}>
                {c.title}
              </p>
              {c.source && <p className="card-subtitle">{c.source}</p>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 503) {
      return "Produktsøk er ikke satt opp ennå (mangler SerpAPI-nøkkel).";
    }
    if (err.message) {
      return err.message;
    }
  }
  return "Søket feilet. Prøv igjen.";
}
