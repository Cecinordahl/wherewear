import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { locationsApi, packingListsApi } from "../api/endpoints";
import type { PackingTrip, PackingTrips, Season } from "../types";
import { SEASON_LABELS } from "../types";

function formatTripDate(tripDate: string | null): string {
  if (!tripDate) return "Uten dato";
  return new Date(`${tripDate}T00:00`).toLocaleDateString("nb-NO", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function todayIso(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function PackingListDetailPage() {
  const { locationId, season } = useParams<{ locationId: string; season: Season }>();
  const [locationName, setLocationName] = useState<string | null>(null);
  const [trips, setTrips] = useState<PackingTrips | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [newItemNames, setNewItemNames] = useState<Record<string, string>>({});
  const [newCategoryName, setNewCategoryName] = useState("");
  const [creating, setCreating] = useState(false);
  const [newTripDate, setNewTripDate] = useState("");

  const applyTrips = useCallback((loaded: PackingTrips, selectId?: string) => {
    setTrips(loaded);
    setSelectedId((current) => {
      const wanted = selectId ?? current;
      const all = [...loaded.active, ...loaded.past];
      return all.some((t) => t.id === wanted) ? wanted! : loaded.active[0]?.id ?? null;
    });
  }, []);

  const loadTrips = async (selectId?: string) => {
    if (!locationId || !season) return;
    try {
      applyTrips(await packingListsApi.listTrips(locationId, season), selectId);
    } catch {
      setError("Klarte ikke å laste pakkelisten.");
    }
  };

  useEffect(() => {
    if (!locationId || !season) return;
    packingListsApi
      .listTrips(locationId, season)
      .then((loaded) => applyTrips(loaded))
      .catch(() => setError("Klarte ikke å laste pakkelisten."));
  }, [locationId, season, applyTrips]);

  useEffect(() => {
    if (!locationId) return;
    locationsApi
      .list()
      .then((locations) => setLocationName(locations.find((l) => l.id === locationId)?.name ?? null))
      .catch(() => {});
  }, [locationId]);

  const trip = trips ? [...trips.active, ...trips.past].find((t) => t.id === selectedId) ?? null : null;
  const readOnly = trip?.past ?? false;

  const replaceTrip = (updated: PackingTrip) => {
    setTrips((prev) =>
      prev && {
        active: prev.active.map((t) => (t.id === updated.id ? updated : t)),
        past: prev.past.map((t) => (t.id === updated.id ? updated : t)),
      }
    );
  };

  const persist = async (updated: PackingTrip) => {
    replaceTrip(updated);
    try {
      await packingListsApi.saveTrip(updated.id, updated.tripDate, updated.categories);
    } catch {
      setError("Klarte ikke å lagre endringen.");
    }
  };

  const updateCategories = (fn: (categories: PackingTrip["categories"]) => PackingTrip["categories"]) => {
    if (!trip) return;
    void persist({ ...trip, categories: fn(trip.categories) });
  };

  const toggleItem = (category: string, itemName: string) =>
    updateCategories((categories) =>
      categories.map((c) =>
        c.category !== category
          ? c
          : { ...c, items: c.items.map((i) => (i.name === itemName ? { ...i, checked: !i.checked } : i)) }
      )
    );

  const removeItem = (category: string, itemName: string) =>
    updateCategories((categories) =>
      categories.map((c) => (c.category !== category ? c : { ...c, items: c.items.filter((i) => i.name !== itemName) }))
    );

  const addItem = (category: string) => {
    const name = (newItemNames[category] ?? "").trim();
    const existing = trip?.categories.find((c) => c.category === category);
    if (!name || existing?.items.some((i) => i.name.toLowerCase() === name.toLowerCase())) return;
    setNewItemNames((prev) => ({ ...prev, [category]: "" }));
    updateCategories((categories) =>
      categories.map((c) => (c.category !== category ? c : { ...c, items: [...c.items, { name, checked: false }] }))
    );
  };

  const addCategory = () => {
    const name = newCategoryName.trim();
    if (!name || trip?.categories.some((c) => c.category.toLowerCase() === name.toLowerCase())) return;
    setNewCategoryName("");
    updateCategories((categories) => [...categories, { category: name, items: [] }]);
  };

  const removeCategory = (category: string) => {
    if (!confirm(`Fjerne «${category}» fra denne listen?`)) return;
    updateCategories((categories) => categories.filter((c) => c.category !== category));
  };

  const changeDate = async (tripDate: string) => {
    if (!trip) return;
    const date = tripDate || null;
    if (date && date < todayIso() && !confirm("Datoen er passert, så listen flyttes til «Tidligere turer». Fortsette?")) {
      return;
    }
    await persist({ ...trip, tripDate: date });
    // Re-sort, and if the date is now past, move it to history (a fresh list is created if needed).
    await loadTrips(trip.id);
  };

  const handleCreate = async () => {
    if (!locationId || !season) return;
    setError(null);
    try {
      const created = await packingListsApi.createTrip(locationId, season, newTripDate || null);
      setCreating(false);
      setNewTripDate("");
      await loadTrips(created.id);
    } catch {
      setError("Klarte ikke å opprette listen.");
    }
  };

  const handleReset = async () => {
    if (!trip) return;
    if (!confirm("Dette overskriver dine endringer med standardlisten + inventar. Fortsette?")) {
      return;
    }
    setError(null);
    try {
      replaceTrip(await packingListsApi.resetTrip(trip.id));
    } catch {
      setError("Klarte ikke å tilbakestille listen.");
    }
  };

  const handleDelete = async () => {
    if (!trip) return;
    if (!confirm(`Slette pakkelisten (${formatTripDate(trip.tripDate)})?`)) return;
    setError(null);
    try {
      await packingListsApi.deleteTrip(trip.id);
      setSelectedId(null);
      await loadTrips();
    } catch {
      setError("Klarte ikke å slette listen.");
    }
  };

  if (!season) {
    return null;
  }

  return (
    <div>
      <Link to="/packing-lists" className="card-subtitle">
        ← Pakkelister
      </Link>
      <h2 style={{ marginTop: "0.4rem" }}>
        {locationName ? `${locationName} · ` : ""}
        {SEASON_LABELS[season]}
      </h2>

      {error && <div className="error-banner">{error}</div>}

      {trips === null && <p className="empty-state">Laster …</p>}

      {trips && (
        <div className="row" style={{ flexWrap: "wrap", marginBottom: "0.75rem" }}>
          {trips.active.map((t) => (
            <button
              key={t.id}
              className={`btn ${t.id === selectedId ? "" : "secondary"}`}
              onClick={() => setSelectedId(t.id)}
            >
              {formatTripDate(t.tripDate)}
            </button>
          ))}
          {!creating && (
            <button className="btn secondary" onClick={() => setCreating(true)}>
              + Ny liste
            </button>
          )}
        </div>
      )}

      {creating && (
        <form
          className="card"
          onSubmit={(e) => {
            e.preventDefault();
            void handleCreate();
          }}
        >
          <p className="card-title">Ny pakkeliste</p>
          <p className="card-subtitle">Starter fra standardlisten for {SEASON_LABELS[season].toLowerCase()}.</p>
          <div className="row" style={{ marginTop: "0.5rem" }}>
            <input type="date" value={newTripDate} onChange={(e) => setNewTripDate(e.target.value)} />
            <button className="btn" type="submit">
              Opprett
            </button>
            <button className="btn secondary" type="button" onClick={() => setCreating(false)}>
              Avbryt
            </button>
          </div>
        </form>
      )}

      {trip && (
        <>
          <div className="row-between" style={{ margin: "0.75rem 0", flexWrap: "wrap", gap: "0.5rem" }}>
            {readOnly ? (
              <p className="card-subtitle" style={{ margin: 0 }}>
                Tidligere tur · {formatTripDate(trip.tripDate)}
              </p>
            ) : (
              <label className="row" style={{ alignItems: "center" }}>
                <span className="card-subtitle">Dato</span>
                <input
                  type="date"
                  value={trip.tripDate ?? ""}
                  onChange={(e) => void changeDate(e.target.value)}
                />
              </label>
            )}
            <div className="row">
              {!readOnly && (
                <button className="btn secondary" onClick={() => void handleReset()}>
                  Tilbakestill
                </button>
              )}
              <button className="btn danger" onClick={() => void handleDelete()}>
                Slett
              </button>
            </div>
          </div>

          {trip.categories.map((c) => (
            <div key={c.category} className="category-section">
              <div className="row-between">
                <p className="category-title">{c.category}</p>
                {!readOnly && (
                  <button
                    className="icon-btn"
                    onClick={() => removeCategory(c.category)}
                    aria-label={`Fjern kategorien ${c.category}`}
                  >
                    ✕
                  </button>
                )}
              </div>
              {c.items.length > 0 && (
                <ul className="checklist">
                  {c.items.map((item) => (
                    <li key={item.name} className={`checklist-item ${item.checked ? "checked" : ""}`}>
                      <input
                        type="checkbox"
                        checked={item.checked}
                        disabled={readOnly}
                        onChange={() => toggleItem(c.category, item.name)}
                        id={`${c.category}-${item.name}`}
                      />
                      <label htmlFor={`${c.category}-${item.name}`}>{item.name}</label>
                      {!readOnly && (
                        <button
                          className="icon-btn"
                          onClick={() => removeItem(c.category, item.name)}
                          aria-label="Fjern"
                        >
                          ✕
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              {!readOnly && (
                <form
                  className="row"
                  style={{ marginTop: "0.4rem" }}
                  onSubmit={(e) => {
                    e.preventDefault();
                    addItem(c.category);
                  }}
                >
                  <input
                    type="text"
                    placeholder="Legg til …"
                    value={newItemNames[c.category] ?? ""}
                    onChange={(e) => setNewItemNames((prev) => ({ ...prev, [c.category]: e.target.value }))}
                  />
                  <button className="btn secondary" type="submit">
                    +
                  </button>
                </form>
              )}
            </div>
          ))}

          {!readOnly && (
            <form
              className="category-section"
              onSubmit={(e) => {
                e.preventDefault();
                addCategory();
              }}
            >
              <p className="category-title">Ny kategori</p>
              <div className="row">
                <input
                  type="text"
                  placeholder="F.eks. Kveldsantrekk"
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                />
                <button className="btn secondary" type="submit">
                  +
                </button>
              </div>
            </form>
          )}
        </>
      )}

      {trips && trips.past.length > 0 && (
        <div className="category-section" style={{ marginTop: "2rem" }}>
          <p className="category-title">Tidligere turer</p>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {trips.past.map((t) => (
              <button
                key={t.id}
                className={`btn ${t.id === selectedId ? "" : "secondary"}`}
                onClick={() => setSelectedId(t.id)}
              >
                {formatTripDate(t.tripDate)}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
