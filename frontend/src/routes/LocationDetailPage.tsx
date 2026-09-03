import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../api/client";
import { categoriesApi, itemsApi, locationsApi } from "../api/endpoints";
import type { InventoryItem, Location } from "../types";

export default function LocationDetailPage() {
  const { locationId } = useParams<{ locationId: string }>();
  const [location, setLocation] = useState<Location | null>(null);
  const [categories, setCategories] = useState<string[]>([]);
  const [items, setItems] = useState<InventoryItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [newItemName, setNewItemName] = useState("");
  const [newItemBrand, setNewItemBrand] = useState("");
  const [newItemCategory, setNewItemCategory] = useState("");
  const [saving, setSaving] = useState(false);
  const [addingCategory, setAddingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [savingCategory, setSavingCategory] = useState(false);

  const loadItems = () => {
    if (!locationId) return;
    itemsApi
      .listForLocation(locationId)
      .then(setItems)
      .catch(() => setError("Klarte ikke å laste ting."));
  };

  useEffect(() => {
    if (!locationId) return;
    locationsApi
      .list()
      .then((all) => {
        const found = all.find((l) => l.id === locationId) ?? null;
        setLocation(found);
        if (found) {
          categoriesApi.forLocationType(found.type).then((cats) => {
            setCategories(cats);
            setNewItemCategory((current) => current || cats[0] || "");
          });
        }
      })
      .catch(() => setError("Klarte ikke å laste stedet."));
    loadItems();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [locationId]);

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!locationId || !newItemName.trim() || !newItemCategory) return;
    setSaving(true);
    setError(null);
    try {
      await itemsApi.create(locationId, newItemCategory, newItemName.trim(), newItemBrand.trim() || null);
      setNewItemName("");
      setNewItemBrand("");
      loadItems();
    } catch {
      setError("Klarte ikke å legge til.");
    } finally {
      setSaving(false);
    }
  };

  const handleSaveNewCategory = async () => {
    if (!location || !newCategoryName.trim()) return;
    setSavingCategory(true);
    setError(null);
    try {
      const updated = await categoriesApi.create(location.type, newCategoryName.trim());
      setCategories(updated);
      setNewItemCategory(newCategoryName.trim());
      setAddingCategory(false);
      setNewCategoryName("");
    } catch (err) {
      setError(err instanceof ApiError && err.message ? err.message : "Klarte ikke å legge til kategorien.");
    } finally {
      setSavingCategory(false);
    }
  };

  const handleDeleteItem = async (id: string) => {
    setError(null);
    try {
      await itemsApi.remove(id);
      loadItems();
    } catch {
      setError("Klarte ikke å slette.");
    }
  };

  if (!location) {
    return <p className="empty-state">Laster …</p>;
  }

  const itemsByCategory = categories.map((category) => ({
    category,
    items: (items ?? []).filter((item) => item.category === category),
  }));

  return (
    <div>
      <Link to="/locations" className="card-subtitle">
        ← Steder
      </Link>
      <h2 style={{ marginTop: "0.4rem" }}>{location.name}</h2>

      {error && <div className="error-banner">{error}</div>}

      <form className="stack card" onSubmit={(e) => void handleAddItem(e)}>
        <input
          type="text"
          placeholder="Ny ting, f.eks. Blå ullgenser"
          value={newItemName}
          onChange={(e) => setNewItemName(e.target.value)}
        />
        <input
          type="text"
          placeholder="Merke (valgfritt)"
          value={newItemBrand}
          onChange={(e) => setNewItemBrand(e.target.value)}
        />
        <select
          value={newItemCategory}
          onChange={(e) => {
            if (e.target.value === "__new__") {
              setAddingCategory(true);
            } else {
              setNewItemCategory(e.target.value);
            }
          }}
        >
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
          <option value="__new__">+ Ny kategori</option>
        </select>

        {addingCategory && (
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
              onClick={() => void handleSaveNewCategory()}
              disabled={savingCategory || !newCategoryName.trim()}
            >
              Lagre
            </button>
            <button
              type="button"
              className="icon-btn"
              aria-label="Avbryt"
              onClick={() => {
                setAddingCategory(false);
                setNewCategoryName("");
              }}
            >
              ✕
            </button>
          </div>
        )}

        <button className="btn" type="submit" disabled={saving || !newItemName.trim() || addingCategory}>
          Legg til
        </button>
      </form>

      <Link to={`/locations/${locationId}/import-receipt`} className="btn secondary" style={{ textDecoration: "none", display: "inline-block", marginBottom: "1rem" }}>
        📄 Importer fra kvittering
      </Link>

      {items === null && <p className="empty-state">Laster …</p>}

      {itemsByCategory.map(
        ({ category, items: categoryItems }) =>
          categoryItems.length > 0 && (
            <div key={category} className="category-section">
              <p className="category-title">{category}</p>
              <ul className="checklist">
                {categoryItems.map((item) => (
                  <li key={item.id} className="checklist-item">
                    {item.photoDataUrl ? (
                      <img
                        src={item.photoDataUrl}
                        alt=""
                        style={{ width: 32, height: 32, borderRadius: 6, objectFit: "cover" }}
                      />
                    ) : (
                      <Link
                        to={`/locations/${locationId}/items/${item.id}/find-photo`}
                        className="icon-btn"
                        aria-label="Finn produktbilde"
                        title="Finn produktbilde"
                      >
                        📷
                      </Link>
                    )}
                    <label>
                      {item.name}
                      {item.brand && <span className="card-subtitle"> · {item.brand}</span>}
                    </label>
                    <button
                      className="icon-btn"
                      onClick={() => void handleDeleteItem(item.id)}
                      aria-label="Slett"
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )
      )}

      {items !== null && items.length === 0 && (
        <p className="empty-state">Ingenting registrert her ennå.</p>
      )}
    </div>
  );
}
