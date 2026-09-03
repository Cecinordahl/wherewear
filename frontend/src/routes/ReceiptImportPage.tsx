import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { categoriesApi, itemsApi, locationsApi, receiptImportApi } from "../api/endpoints";
import { useUnsavedChanges } from "../navigation/UnsavedChangesContext";
import type { Location } from "../types";

interface ReviewRow {
  id: string;
  name: string;
  category: string;
  locationId: string;
}

export default function ReceiptImportPage() {
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();

  const [location, setLocation] = useState<Location | null>(null);
  const [locations, setLocations] = useState<Location[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [rows, setRows] = useState<ReviewRow[] | null>(null);
  const [extracting, setExtracting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { setBlocked, confirmLeave } = useUnsavedChanges();

  useEffect(() => {
    setBlocked(extracting || (rows !== null && rows.length > 0));
  }, [extracting, rows, setBlocked]);

  useEffect(() => () => setBlocked(false), [setBlocked]);

  useEffect(() => {
    if (!locationId) return;
    locationsApi
      .list()
      .then((all) => {
        setLocations(all);
        const found = all.find((l) => l.id === locationId) ?? null;
        setLocation(found);
        if (found) {
          categoriesApi.forLocationType(found.type).then(setCategories);
        }
      })
      .catch(() => setError("Klarte ikke å laste stedet."));
  }, [locationId]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !location) return;
    setExtracting(true);
    setError(null);
    setRows(null);
    try {
      const candidates = await receiptImportApi.extract(location.type, file);
      setRows(
        candidates.map((c, i) => ({
          id: `${Date.now()}-${i}`,
          name: c.name,
          category: c.category,
          locationId: location.id,
        }))
      );
    } catch (err) {
      setError(describeError(err));
    } finally {
      setExtracting(false);
    }
  };

  const updateRow = (id: string, changes: Partial<ReviewRow>) => {
    setRows((prev) => prev?.map((r) => (r.id === id ? { ...r, ...changes } : r)) ?? null);
  };

  const removeRow = (id: string) => {
    setRows((prev) => prev?.filter((r) => r.id !== id) ?? null);
  };

  const handleSaveAll = async () => {
    if (!rows || rows.length === 0 || !locationId) return;
    setSaving(true);
    setError(null);
    try {
      for (const row of rows) {
        await itemsApi.create(row.locationId, row.category, row.name);
      }
      setBlocked(false);
      navigate(`/locations/${locationId}`);
    } catch {
      setError("Noe gikk galt underveis - sjekk hva som ble lagt til før du prøver igjen.");
      setSaving(false);
    }
  };

  if (!location) {
    return <p className="empty-state">Laster …</p>;
  }

  return (
    <div>
      <Link
        to={`/locations/${locationId}`}
        className="card-subtitle"
        onClick={(e) => {
          if (!confirmLeave()) e.preventDefault();
        }}
      >
        ← {location.name}
      </Link>
      <h2 style={{ marginTop: "0.4rem" }}>Importer fra kvittering</h2>

      {error && <div className="error-banner">{error}</div>}

      {rows === null && (
        <div className="card stack">
          <p className="card-subtitle">
            Ta bilde av kvitteringen (eller last opp et eksisterende bilde). Alt legges til «{location.name}» som
            standard - du kan endre sted eller fjerne varer i neste steg.
          </p>
          <button className="btn" onClick={() => fileInputRef.current?.click()} disabled={extracting}>
            {extracting ? "Leser kvittering …" : "Velg bilde av kvittering"}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ display: "none" }}
            onChange={(e) => void handleFileChange(e)}
          />
        </div>
      )}

      {rows !== null && rows.length === 0 && (
        <p className="empty-state">Fant ingen varer på kvitteringen. Prøv et tydeligere bilde.</p>
      )}

      {rows !== null && rows.length > 0 && (
        <div className="stack">
          <p className="card-subtitle">{rows.length} varer funnet. Sjekk gjerne over før du legger til.</p>
          <ul className="checklist">
            {rows.map((row) => (
              <li key={row.id} className="checklist-item" style={{ flexWrap: "wrap" }}>
                <div className="stack" style={{ flex: 1, minWidth: "60%" }}>
                  <input
                    type="text"
                    value={row.name}
                    onChange={(e) => updateRow(row.id, { name: e.target.value })}
                  />
                  <div className="row">
                    <select
                      value={row.category}
                      onChange={(e) => updateRow(row.id, { category: e.target.value })}
                      style={{ flex: 1 }}
                    >
                      {categories.map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                    <select
                      value={row.locationId}
                      onChange={(e) => updateRow(row.id, { locationId: e.target.value })}
                      style={{ flex: 1 }}
                    >
                      {locations.map((l) => (
                        <option key={l.id} value={l.id}>
                          {l.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <button className="icon-btn" onClick={() => removeRow(row.id)} aria-label="Fjern">
                  ✕
                </button>
              </li>
            ))}
          </ul>
          <button className="btn" onClick={() => void handleSaveAll()} disabled={saving}>
            {saving ? "Legger til …" : `Legg til ${rows.length} varer`}
          </button>
        </div>
      )}
    </div>
  );
}

function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 503) {
      return "Kvitteringsimport er ikke satt opp ennå (mangler GEMINI_API_KEY).";
    }
    if (err.message.includes("Gemini request failed: 503") || err.message.includes("Gemini request failed: 429")) {
      return "Gemini er overbelastet akkurat nå - prøv igjen om litt.";
    }
    if (err.message) {
      return err.message;
    }
  }
  return "Klarte ikke å lese kvitteringen. Prøv igjen.";
}
