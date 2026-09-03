import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { locationsApi } from "../api/endpoints";
import type { Location, LocationType } from "../types";
import { LOCATION_TYPE_LABELS } from "../types";

export default function LocationsPage() {
  const [locations, setLocations] = useState<Location[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [type, setType] = useState<LocationType>("FLIGHT");
  const [icon, setIcon] = useState("");
  const [saving, setSaving] = useState(false);
  const [editingIconId, setEditingIconId] = useState<string | null>(null);
  const [iconDraft, setIconDraft] = useState("");
  const [savingIcon, setSavingIcon] = useState(false);

  const load = () => {
    locationsApi
      .list()
      .then(setLocations)
      .catch(() => setError("Klarte ikke å laste steder."));
  };

  useEffect(load, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await locationsApi.create(name.trim(), type, icon.trim() || null);
      setName("");
      setIcon("");
      load();
    } catch {
      setError("Klarte ikke å opprette stedet.");
    } finally {
      setSaving(false);
    }
  };

  const startEditIcon = (location: Location) => {
    setEditingIconId(location.id);
    setIconDraft(location.icon ?? "");
  };

  const handleSaveIcon = async (location: Location) => {
    setSavingIcon(true);
    setError(null);
    try {
      await locationsApi.update(location.id, location.name, location.type, iconDraft.trim() || null);
      setEditingIconId(null);
      load();
    } catch {
      setError("Klarte ikke å lagre emojien.");
    } finally {
      setSavingIcon(false);
    }
  };

  const handleDelete = async (id: string) => {
    setError(null);
    try {
      await locationsApi.remove(id);
      load();
    } catch {
      setError("Klarte ikke å slette stedet.");
    }
  };

  return (
    <div>
      {error && <div className="error-banner">{error}</div>}

      <form className="stack card" onSubmit={(e) => void handleCreate(e)}>
        <input
          type="text"
          placeholder="Nytt sted, f.eks. Spania"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <select value={type} onChange={(e) => setType(e.target.value as LocationType)}>
          <option value="FLIGHT">{LOCATION_TYPE_LABELS.FLIGHT}</option>
          <option value="CABIN">{LOCATION_TYPE_LABELS.CABIN}</option>
        </select>
        <input
          type="text"
          placeholder="Emoji (valgfritt), f.eks. 🇪🇸 - brukes bl.a. som ikon på handlelisten"
          value={icon}
          onChange={(e) => setIcon(e.target.value)}
        />
        <button className="btn" type="submit" disabled={saving || !name.trim()}>
          Legg til sted
        </button>
      </form>

      {locations === null && <p className="empty-state">Laster …</p>}
      {locations?.length === 0 && <p className="empty-state">Ingen steder ennå.</p>}

      {locations?.map((location) => (
        <div key={location.id} className="card stack">
          <div className="row-between">
            <Link to={`/locations/${location.id}`} className="card-link">
              <p className="card-title">
                {location.icon ?? "🎯"} {location.name}
              </p>
              <p className="card-subtitle">{LOCATION_TYPE_LABELS[location.type]}</p>
            </Link>
            <div className="row">
              <button className="icon-btn" onClick={() => startEditIcon(location)} aria-label="Endre emoji">
                ✎
              </button>
              <button className="icon-btn" onClick={() => void handleDelete(location.id)} aria-label="Slett">
                ✕
              </button>
            </div>
          </div>

          {editingIconId === location.id && (
            <div className="row">
              <input
                type="text"
                placeholder="Emoji, f.eks. 🇪🇸"
                value={iconDraft}
                onChange={(e) => setIconDraft(e.target.value)}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="btn secondary"
                onClick={() => void handleSaveIcon(location)}
                disabled={savingIcon}
              >
                Lagre
              </button>
              <button type="button" className="icon-btn" aria-label="Avbryt" onClick={() => setEditingIconId(null)}>
                ✕
              </button>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
