import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { categoriesApi, itemsApi, locationsApi, photoImportApi } from "../api/endpoints";
import { useUnsavedChanges } from "../navigation/UnsavedChangesContext";
import type { Location } from "../types";
import { compressPhoto } from "../utils/image";

type RowStatus = "queued" | "identifying" | "done" | "failed";

interface ReviewRow {
  id: string;
  photoDataUrl: string;
  photoBlob: Blob;
  status: RowStatus;
  failureReason: string | null;
  name: string;
  category: string;
  brand: string;
  locationId: string;
}

// Gemini's free tier allows roughly 10 requests/minute. The backend retries
// briefly, but a per-minute limit needs a longer wait than that.
const RATE_LIMIT_RETRY_DELAYS_MS = [15_000, 30_000];

export default function PhotoImportPage() {
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();

  const [location, setLocation] = useState<Location | null>(null);
  const [locations, setLocations] = useState<Location[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [rows, setRows] = useState<ReviewRow[]>([]);
  const [preparing, setPreparing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queueRef = useRef<ReviewRow[]>([]);
  const processingRef = useRef(false);
  const unmountedRef = useRef(false);
  const { setBlocked, confirmLeave } = useUnsavedChanges();
  const [addingCategoryForRowId, setAddingCategoryForRowId] = useState<string | null>(null);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [savingCategory, setSavingCategory] = useState(false);

  const identifying = rows.some((r) => r.status === "queued" || r.status === "identifying");

  useEffect(() => {
    setBlocked(preparing || rows.length > 0);
  }, [preparing, rows, setBlocked]);

  useEffect(
    () => () => {
      unmountedRef.current = true;
      setBlocked(false);
    },
    [setBlocked]
  );

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

  const updateRow = (id: string, changes: Partial<ReviewRow>) => {
    setRows((prev) => prev.map((r) => (r.id === id ? { ...r, ...changes } : r)));
  };

  const removeRow = (id: string) => {
    queueRef.current = queueRef.current.filter((r) => r.id !== id);
    setRows((prev) => prev.filter((r) => r.id !== id));
  };

  // Identifies queued photos one at a time, so a big batch doesn't trip Gemini's rate limit.
  const processQueue = async (loc: Location) => {
    if (processingRef.current) return;
    processingRef.current = true;
    while (queueRef.current.length > 0 && !unmountedRef.current) {
      const row = queueRef.current.shift()!;
      updateRow(row.id, { status: "identifying" });
      try {
        const candidate = await identifyWithRetry(loc, row.photoBlob);
        updateRow(row.id, {
          status: "done",
          name: candidate.name,
          category: candidate.category,
          brand: candidate.brand ?? "",
        });
      } catch (err) {
        updateRow(row.id, { status: "failed", failureReason: describeError(err) });
      }
    }
    processingRef.current = false;
  };

  const identifyWithRetry = async (loc: Location, photo: Blob) => {
    for (let attempt = 0; ; attempt++) {
      try {
        return await photoImportApi.identify(loc.type, photo);
      } catch (err) {
        if (!isRateLimited(err) || attempt >= RATE_LIMIT_RETRY_DELAYS_MS.length || unmountedRef.current) {
          throw err;
        }
        await new Promise((resolve) => setTimeout(resolve, RATE_LIMIT_RETRY_DELAYS_MS[attempt]));
      }
    }
  };

  const handleFilesChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    if (files.length === 0 || !location) return;
    setPreparing(true);
    setError(null);

    const newRows: ReviewRow[] = [];
    let unreadable = 0;
    for (const file of files) {
      try {
        const photo = await compressPhoto(file);
        newRows.push({
          id: crypto.randomUUID(),
          photoDataUrl: photo.dataUrl,
          photoBlob: photo.blob,
          status: "queued",
          failureReason: null,
          name: "",
          category: categories[0] ?? "",
          brand: "",
          locationId: location.id,
        });
      } catch {
        unreadable++;
      }
    }
    if (unreadable > 0) {
      setError(`${unreadable} bilde(r) kunne ikke leses (prøv JPEG eller PNG).`);
    }
    setRows((prev) => [...prev, ...newRows]);
    queueRef.current.push(...newRows);
    setPreparing(false);
    void processQueue(location);
  };

  const retryRow = (row: ReviewRow) => {
    if (!location) return;
    updateRow(row.id, { status: "queued", failureReason: null });
    queueRef.current.push(row);
    void processQueue(location);
  };

  const handleSaveNewCategory = async (rowId: string) => {
    if (!location || !newCategoryName.trim()) return;
    setSavingCategory(true);
    setError(null);
    try {
      const updated = await categoriesApi.create(location.type, newCategoryName.trim());
      setCategories(updated);
      updateRow(rowId, { category: newCategoryName.trim() });
      setAddingCategoryForRowId(null);
      setNewCategoryName("");
    } catch (err) {
      setError(err instanceof ApiError && err.message ? err.message : "Klarte ikke å legge til kategorien.");
    } finally {
      setSavingCategory(false);
    }
  };

  const handleSaveAll = async () => {
    if (rows.length === 0 || !locationId) return;
    setSaving(true);
    setError(null);
    for (const row of rows) {
      try {
        await itemsApi.create(row.locationId, row.category, row.name.trim(), row.brand.trim() || null, row.photoDataUrl);
        // Drop each row as it's saved, so retrying after a failure doesn't create duplicates.
        setRows((prev) => prev.filter((r) => r.id !== row.id));
      } catch {
        setError("Noe gikk galt underveis - varene som står igjen ble ikke lagt til. Prøv igjen.");
        setSaving(false);
        return;
      }
    }
    setBlocked(false);
    navigate(`/locations/${locationId}`);
  };

  if (!location) {
    return <p className="empty-state">Laster …</p>;
  }

  const missingName = rows.some((r) => !r.name.trim());

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
      <h2 style={{ marginTop: "0.4rem" }}>Importer fra bilder</h2>

      {error && <div className="error-banner">{error}</div>}

      <div className="card stack">
        <p className="card-subtitle">
          Velg ett bilde per plagg - flere bilder om gangen går fint. Bildet lagres på varen. Alt legges til «
          {location.name}» som standard - du kan endre sted eller fjerne varer før du lagrer.
        </p>
        <button className="btn" onClick={() => fileInputRef.current?.click()} disabled={preparing || saving}>
          {preparing ? "Forbereder bilder …" : rows.length > 0 ? "Legg til flere bilder" : "Velg bilder"}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          style={{ display: "none" }}
          onChange={(e) => void handleFilesChange(e)}
        />
      </div>

      {rows.length > 0 && (
        <div className="stack">
          <p className="card-subtitle">
            {identifying
              ? `Gjenkjenner plagg … (${rows.filter((r) => r.status === "done" || r.status === "failed").length}/${rows.length})`
              : `${rows.length} varer klare. Sjekk gjerne over før du legger til.`}
          </p>
          <ul className="checklist">
            {rows.map((row) => (
              <li key={row.id} className="checklist-item" style={{ flexWrap: "wrap", alignItems: "flex-start" }}>
                <img
                  src={row.photoDataUrl}
                  alt=""
                  style={{ width: 64, height: 64, borderRadius: 8, objectFit: "cover", flexShrink: 0 }}
                />
                <div className="stack" style={{ flex: 1, minWidth: "50%" }}>
                  {row.status === "queued" || row.status === "identifying" ? (
                    <p className="card-subtitle">{row.status === "identifying" ? "Gjenkjenner …" : "I kø …"}</p>
                  ) : (
                    <>
                      {row.status === "failed" && (
                        <div className="row">
                          <p className="card-subtitle" style={{ flex: 1 }}>
                            {row.failureReason} Fyll inn selv, eller
                          </p>
                          <button type="button" className="btn secondary" onClick={() => retryRow(row)}>
                            Prøv igjen
                          </button>
                        </div>
                      )}
                      <input
                        type="text"
                        placeholder="Navn"
                        value={row.name}
                        onChange={(e) => updateRow(row.id, { name: e.target.value })}
                      />
                      <input
                        type="text"
                        placeholder="Merke (valgfritt)"
                        value={row.brand}
                        onChange={(e) => updateRow(row.id, { brand: e.target.value })}
                      />
                      <div className="row">
                        <select
                          value={row.category}
                          onChange={(e) => {
                            if (e.target.value === "__new__") {
                              setAddingCategoryForRowId(row.id);
                            } else {
                              updateRow(row.id, { category: e.target.value });
                            }
                          }}
                          style={{ flex: 1 }}
                        >
                          {categories.map((c) => (
                            <option key={c} value={c}>
                              {c}
                            </option>
                          ))}
                          <option value="__new__">+ Ny kategori</option>
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

                      {addingCategoryForRowId === row.id && (
                        <div className="row">
                          <input
                            type="text"
                            placeholder="Navn på ny kategori"
                            value={newCategoryName}
                            onChange={(e) => setNewCategoryName(e.target.value)}
                            style={{ flex: 1 }}
                          />
                          <button
                            type="button"
                            className="btn secondary"
                            onClick={() => void handleSaveNewCategory(row.id)}
                            disabled={savingCategory || !newCategoryName.trim()}
                          >
                            Lagre
                          </button>
                          <button
                            type="button"
                            className="icon-btn"
                            aria-label="Avbryt"
                            onClick={() => {
                              setAddingCategoryForRowId(null);
                              setNewCategoryName("");
                            }}
                          >
                            ✕
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </div>
                <button className="icon-btn" onClick={() => removeRow(row.id)} aria-label="Fjern" disabled={saving}>
                  ✕
                </button>
              </li>
            ))}
          </ul>
          <button
            className="btn"
            onClick={() => void handleSaveAll()}
            disabled={saving || identifying || missingName}
          >
            {saving
              ? "Legger til …"
              : identifying
                ? "Venter på gjenkjenning …"
                : missingName
                  ? "Alle varer trenger et navn"
                  : `Legg til ${rows.length} varer`}
          </button>
        </div>
      )}
    </div>
  );
}

function isRateLimited(err: unknown): boolean {
  return (
    err instanceof ApiError &&
    (err.message.includes("Gemini request failed: 503") || err.message.includes("Gemini request failed: 429"))
  );
}

function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 503) {
      return "Bildeimport er ikke satt opp ennå (mangler GEMINI_API_KEY).";
    }
    if (isRateLimited(err)) {
      return "Gemini er overbelastet akkurat nå.";
    }
  }
  return "Klarte ikke å gjenkjenne plagget.";
}
