import { useState } from "react";
import { KNOWN_STORES } from "../stores";
import type { CustomStore } from "../types";

/**
 * Custom searchable dropdown for the store name field. Native <datalist> is
 * unreliable on iOS Safari (poor/no suggestion UI there), so this is a
 * plain text input + filtered suggestion list instead - works consistently
 * across browsers. Merges the small hardcoded starter list (stores.ts) with
 * stores the user has added themselves (persisted via customStoresApi), and
 * offers an inline "add this as a new store" option when nothing matches.
 */
export default function StoreAutocomplete({
  value,
  onChange,
  customStores,
  onAddStore,
}: {
  value: string;
  onChange: (value: string) => void;
  customStores: CustomStore[];
  onAddStore: (name: string, url: string | null) => void;
}) {
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [addingNew, setAddingNew] = useState(false);
  const [newStoreUrl, setNewStoreUrl] = useState("");

  const allStores = [...KNOWN_STORES, ...customStores.map((s) => ({ name: s.name, url: s.url ?? "" }))];

  const query = value.trim().toLowerCase();
  const matches = query ? allStores.filter((s) => s.name.toLowerCase().includes(query)).slice(0, 6) : [];
  const exactMatch = allStores.some((s) => s.name.toLowerCase() === query);

  const closeAll = () => {
    setShowSuggestions(false);
    setAddingNew(false);
    setNewStoreUrl("");
  };

  const handleSaveNewStore = () => {
    onAddStore(value.trim(), newStoreUrl.trim() || null);
    closeAll();
  };

  return (
    <div style={{ position: "relative" }}>
      <input
        type="text"
        placeholder="f.eks. Bikbok"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setShowSuggestions(true);
          setAddingNew(false);
        }}
        onFocus={() => setShowSuggestions(true)}
        // Delay so a click inside the panel registers before it closes.
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
      />
      {showSuggestions && (matches.length > 0 || (query && !exactMatch)) && (
        <div
          className="checklist"
          style={{ position: "absolute", zIndex: 10, left: 0, right: 0, marginTop: "0.25rem" }}
        >
          {matches.map((s) => (
            <button
              key={s.name}
              type="button"
              style={{
                display: "block",
                width: "100%",
                textAlign: "left",
                padding: "0.65rem 0.8rem",
                background: "none",
                border: "none",
                borderBottom: "1px solid var(--border)",
                cursor: "pointer",
              }}
              onClick={() => {
                onChange(s.name);
                closeAll();
              }}
            >
              {s.name}
            </button>
          ))}

          {query && !exactMatch && !addingNew && (
            <button
              type="button"
              style={{
                display: "block",
                width: "100%",
                textAlign: "left",
                padding: "0.65rem 0.8rem",
                background: "none",
                border: "none",
                color: "var(--accent)",
                cursor: "pointer",
              }}
              onClick={() => setAddingNew(true)}
            >
              ➕ Legg til «{value.trim()}» som ny butikk
            </button>
          )}

          {addingNew && (
            <div className="stack" style={{ padding: "0.65rem 0.8rem" }}>
              <label className="card-subtitle">Nettside for {value.trim()} (valgfritt, men gir bedre søk)</label>
              <input
                type="text"
                placeholder="https://…"
                value={newStoreUrl}
                onChange={(e) => setNewStoreUrl(e.target.value)}
              />
              <button type="button" className="btn secondary" onClick={handleSaveNewStore}>
                Lagre butikk
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
