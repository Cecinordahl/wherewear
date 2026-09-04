import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { locationsApi } from "../api/endpoints";
import type { Location } from "../types";
import { SEASON_LABELS, SEASONS } from "../types";

export default function PackingListsPage() {
  const [locations, setLocations] = useState<Location[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    locationsApi
      .list()
      .then(setLocations)
      .catch(() => setError("Klarte ikke å laste steder."));
  }, []);

  if (error) {
    return <div className="error-banner">{error}</div>;
  }

  if (locations === null) {
    return <p className="empty-state">Laster …</p>;
  }

  if (locations.length === 0) {
    return <p className="empty-state">Legg til et sted først under «Steder».</p>;
  }

  return (
    <div className="stack">
      <div>
        <div className="kicker">Planlegging</div>
        <h2>Pakkelister</h2>
      </div>

      {locations.map((location) => (
        <div key={location.id} className="card">
          <p className="card-title">{location.name}</p>
          <div className="row" style={{ flexWrap: "wrap" }}>
            {SEASONS.map((season) => (
              <Link
                key={season}
                to={`/packing-lists/${location.id}/${season}`}
                className="btn secondary"
                style={{ textDecoration: "none" }}
              >
                {SEASON_LABELS[season]}
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
