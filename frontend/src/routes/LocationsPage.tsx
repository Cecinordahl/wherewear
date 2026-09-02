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
  const [saving, setSaving] = useState(false);

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
      await locationsApi.create(name.trim(), type);
      setName("");
      load();
    } catch {
      setError("Klarte ikke å opprette stedet.");
    } finally {
      setSaving(false);
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
        <button className="btn" type="submit" disabled={saving || !name.trim()}>
          Legg til sted
        </button>
      </form>

      {locations === null && <p className="empty-state">Laster …</p>}
      {locations?.length === 0 && <p className="empty-state">Ingen steder ennå.</p>}

      {locations?.map((location) => (
        <div key={location.id} className="card row-between">
          <Link to={`/locations/${location.id}`} className="card-link">
            <p className="card-title">{location.name}</p>
            <p className="card-subtitle">{LOCATION_TYPE_LABELS[location.type]}</p>
          </Link>
          <button className="icon-btn" onClick={() => void handleDelete(location.id)} aria-label="Slett">
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}
