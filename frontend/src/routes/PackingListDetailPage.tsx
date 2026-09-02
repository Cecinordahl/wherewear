import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { packingListsApi } from "../api/endpoints";
import type { PackingListData, Season } from "../types";
import { SEASON_LABELS } from "../types";

export default function PackingListDetailPage() {
  const { locationId, season } = useParams<{ locationId: string; season: Season }>();
  const [list, setList] = useState<PackingListData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [newItemNames, setNewItemNames] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!locationId || !season) return;
    packingListsApi
      .get(locationId, season)
      .then(setList)
      .catch(() => setError("Klarte ikke å laste pakkelisten."));
  }, [locationId, season]);

  const persist = async (updated: PackingListData) => {
    setList(updated);
    if (!locationId || !season) return;
    try {
      await packingListsApi.save(locationId, season, updated.categories);
    } catch {
      setError("Klarte ikke å lagre endringen.");
    }
  };

  const toggleItem = (category: string, itemName: string) => {
    if (!list) return;
    const updated: PackingListData = {
      ...list,
      categories: list.categories.map((c) =>
        c.category !== category
          ? c
          : {
              ...c,
              items: c.items.map((i) => (i.name === itemName ? { ...i, checked: !i.checked } : i)),
            }
      ),
    };
    void persist(updated);
  };

  const removeItem = (category: string, itemName: string) => {
    if (!list) return;
    const updated: PackingListData = {
      ...list,
      categories: list.categories.map((c) =>
        c.category !== category ? c : { ...c, items: c.items.filter((i) => i.name !== itemName) }
      ),
    };
    void persist(updated);
  };

  const addItem = (category: string) => {
    if (!list) return;
    const name = (newItemNames[category] ?? "").trim();
    if (!name) return;
    const updated: PackingListData = {
      ...list,
      categories: list.categories.map((c) =>
        c.category !== category ? c : { ...c, items: [...c.items, { name, checked: false }] }
      ),
    };
    setNewItemNames((prev) => ({ ...prev, [category]: "" }));
    void persist(updated);
  };

  const handleReset = async () => {
    if (!locationId || !season) return;
    if (!confirm("Dette overskriver dine endringer med en ny liste basert på maler + inventar. Fortsette?")) {
      return;
    }
    setError(null);
    try {
      const regenerated = await packingListsApi.reset(locationId, season);
      setList(regenerated);
    } catch {
      setError("Klarte ikke å tilbakestille listen.");
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
      <div className="row-between" style={{ marginTop: "0.4rem", marginBottom: "0.75rem" }}>
        <h2 style={{ margin: 0 }}>{SEASON_LABELS[season]}</h2>
        <button className="btn secondary" onClick={() => void handleReset()}>
          Tilbakestill
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {list === null && <p className="empty-state">Laster …</p>}

      {list?.categories.map((c) => (
        <div key={c.category} className="category-section">
          <p className="category-title">{c.category}</p>
          <ul className="checklist">
            {c.items.map((item) => (
              <li key={item.name} className={`checklist-item ${item.checked ? "checked" : ""}`}>
                <input
                  type="checkbox"
                  checked={item.checked}
                  onChange={() => toggleItem(c.category, item.name)}
                  id={`${c.category}-${item.name}`}
                />
                <label htmlFor={`${c.category}-${item.name}`}>{item.name}</label>
                <button
                  className="icon-btn"
                  onClick={() => removeItem(c.category, item.name)}
                  aria-label="Fjern"
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>
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
        </div>
      ))}
    </div>
  );
}
