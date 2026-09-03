import { useEffect, useState } from "react";
import { customStoresApi, locationsApi, productLookupApi, shoppingListApi } from "../api/endpoints";
import { HOME_LOCATION_ID, ONLINE_LOCATION_ID } from "../types";
import type { CustomStore, Location, ProductCandidate, ShoppingListItem } from "../types";
import { downloadCalendarReminder } from "../utils/calendar";
import { KNOWN_STORES } from "../stores";
import StoreAutocomplete from "../components/StoreAutocomplete";

const ANYWHERE = "";

export default function ShoppingListPage() {
  const [items, setItems] = useState<ShoppingListItem[] | null>(null);
  const [locations, setLocations] = useState<Location[]>([]);
  const [customStores, setCustomStores] = useState<CustomStore[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [locationId, setLocationId] = useState(ANYWHERE);
  const [dateUnknown, setDateUnknown] = useState(false);
  const [tripDate, setTripDate] = useState("");
  const [leadTimeDays, setLeadTimeDays] = useState(10);
  const [storeName, setStoreName] = useState("");
  const [productUrl, setProductUrl] = useState("");
  const [saving, setSaving] = useState(false);

  const [productCandidates, setProductCandidates] = useState<ProductCandidate[] | null>(null);
  const [searchingProduct, setSearchingProduct] = useState(false);
  const [productSearchError, setProductSearchError] = useState<string | null>(null);

  const isOnline = locationId === ONLINE_LOCATION_ID;

  const load = () => {
    shoppingListApi
      .list()
      .then(setItems)
      .catch(() => setError("Klarte ikke å laste handlelisten."));
  };

  useEffect(() => {
    load();
    locationsApi.list().then(setLocations).catch(() => {});
    customStoresApi.list().then(setCustomStores).catch(() => {});
  }, []);

  const locationName = (id: string | null) => {
    if (id === ONLINE_LOCATION_ID) return "Online";
    if (id === HOME_LOCATION_ID) return "Hjemme";
    if (!id) return "Hvor som helst";
    return locations.find((l) => l.id === id)?.name ?? "Ukjent sted";
  };

  const findStoreUrl = (name: string): string | null => {
    const normalized = name.trim().toLowerCase();
    const known = KNOWN_STORES.find((s) => s.name.toLowerCase() === normalized);
    if (known) return known.url;
    const custom = customStores.find((s) => s.name.toLowerCase() === normalized);
    return custom?.url ?? null;
  };

  const handleAddStore = async (newName: string, newUrl: string | null) => {
    setStoreName(newName);
    setError(null);
    try {
      const created = await customStoresApi.create(newName, newUrl);
      setCustomStores((prev) => [...prev, created]);
    } catch {
      setError("Klarte ikke å lagre den nye butikken.");
    }
  };

  const resetForm = () => {
    setEditingId(null);
    setName("");
    setLocationId(ANYWHERE);
    setDateUnknown(false);
    setTripDate("");
    setLeadTimeDays(10);
    setStoreName("");
    setProductUrl("");
    setProductCandidates(null);
    setProductSearchError(null);
  };

  const startEdit = (item: ShoppingListItem) => {
    setEditingId(item.id);
    setName(item.name);
    setLocationId(item.locationId ?? ANYWHERE);
    setStoreName(item.storeName ?? "");
    setProductUrl(item.productUrl ?? "");
    setProductCandidates(null);
    setProductSearchError(null);
    if (item.locationId === ONLINE_LOCATION_ID) {
      setDateUnknown(!item.tripDate);
      setTripDate(item.tripDate ?? "");
      setLeadTimeDays(item.leadTimeDays ?? 10);
    } else {
      setDateUnknown(false);
      setTripDate("");
      setLeadTimeDays(10);
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleSearchProduct = async () => {
    if (!name.trim() || !storeName.trim()) return;
    setSearchingProduct(true);
    setProductSearchError(null);
    setProductCandidates(null);
    try {
      const results = await productLookupApi.searchByText(name.trim(), storeName.trim(), findStoreUrl(storeName));
      setProductCandidates(results.filter((c) => c.pageUrl));
    } catch {
      setProductSearchError("Søket feilet. Prøv igjen, eller lim inn lenken manuelt.");
    } finally {
      setSearchingProduct(false);
    }
  };

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    setError(null);
    const input = {
      name: name.trim(),
      locationId: locationId || null,
      tripDate: isOnline && !dateUnknown && tripDate ? tripDate : null,
      leadTimeDays: isOnline && !dateUnknown && tripDate ? leadTimeDays : null,
      storeName: storeName.trim() || null,
      storeUrl: storeName.trim() ? findStoreUrl(storeName) : null,
      productUrl: productUrl.trim() || null,
    };
    try {
      if (editingId) {
        await shoppingListApi.update(editingId, input);
      } else {
        await shoppingListApi.create(input);
      }
      resetForm();
      load();
    } catch {
      setError(editingId ? "Klarte ikke å lagre endringene." : "Klarte ikke å legge til.");
    } finally {
      setSaving(false);
    }
  };

  const toggleChecked = async (item: ShoppingListItem) => {
    setError(null);
    try {
      await shoppingListApi.setChecked(item.id, !item.checked);
      load();
    } catch {
      setError("Klarte ikke å oppdatere.");
    }
  };

  const handleDelete = async (id: string) => {
    setError(null);
    try {
      await shoppingListApi.remove(id);
      load();
    } catch {
      setError("Klarte ikke å slette.");
    }
  };

  const handleAddToCalendar = (item: ShoppingListItem) => {
    if (!item.orderByDate) return;
    downloadCalendarReminder(
      `Bestill: ${item.name}`,
      `Wherewear-påminnelse: bestill ${item.name} nå for å rekke det til turen.`,
      item.orderByDate
    );
  };

  if (items === null) {
    return <p className="empty-state">Laster …</p>;
  }

  const dueSoonItems = items.filter((i) => i.dueSoon);
  const onlineItems = items.filter((i) => !i.checked && i.locationId === ONLINE_LOCATION_ID);
  const homeItems = items.filter((i) => !i.checked && i.locationId === HOME_LOCATION_ID);
  const destinationItems = items.filter(
    (i) => !i.checked && i.locationId !== ONLINE_LOCATION_ID && i.locationId !== HOME_LOCATION_ID
  );
  const checkedItems = items.filter((i) => i.checked);

  return (
    <div>
      {error && <div className="error-banner">{error}</div>}

      {dueSoonItems.length > 0 && (
        <div className="card" style={{ borderColor: "var(--danger)" }}>
          <p className="card-title">🔔 Bør bestilles nå</p>
          {dueSoonItems.map((item) => (
            <p key={item.id} className="card-subtitle">
              {item.name} — bestillingsfrist var {item.orderByDate}
            </p>
          ))}
        </div>
      )}

      <form className="stack card" onSubmit={(e) => void handleAdd(e)}>
        <input
          type="text"
          placeholder="Ny ting å kjøpe, f.eks. Mascara"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <div>
          <label className="category-title" style={{ display: "block", marginBottom: "0.3rem" }}>
            Hvor skal det kjøpes?
          </label>
          <select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
            <optgroup label="Bestilling">
              <option value={ONLINE_LOCATION_ID}>Online</option>
            </optgroup>
            <optgroup label="Standard">
              <option value={HOME_LOCATION_ID}>Hjemme</option>
              <option value={ANYWHERE}>Hvor som helst</option>
            </optgroup>
            <optgroup label="Steder">
              {locations.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name}
                </option>
              ))}
            </optgroup>
          </select>
        </div>

        <div>
          <label className="card-subtitle" style={{ display: "block", marginBottom: "0.2rem" }}>
            Butikk (valgfritt)
          </label>
          <StoreAutocomplete
            value={storeName}
            onChange={(value) => {
              setStoreName(value);
              setProductCandidates(null);
            }}
            customStores={customStores}
            onAddStore={(newName, newUrl) => void handleAddStore(newName, newUrl)}
          />
        </div>

        {name.trim() && storeName.trim() && (
          <div>
            <button
              type="button"
              className="btn secondary"
              onClick={() => void handleSearchProduct()}
              disabled={searchingProduct}
            >
              🔍 Søk etter produktlenke hos {storeName.trim()}
            </button>
            {searchingProduct && <p className="card-subtitle">Søker …</p>}
            {productSearchError && <p className="card-subtitle" style={{ color: "var(--danger)" }}>{productSearchError}</p>}
            {productCandidates !== null && productCandidates.length === 0 && !searchingProduct && (
              <p className="card-subtitle">Fant ingen treff. Lim inn lenken manuelt under.</p>
            )}
            {productCandidates && productCandidates.length > 0 && (
              <div className="stack" style={{ marginTop: "0.4rem" }}>
                {productCandidates.map((c, i) => (
                  <button
                    key={i}
                    type="button"
                    className="card"
                    style={{ padding: "0.6rem", textAlign: "left", cursor: "pointer", display: "flex", gap: "0.6rem", alignItems: "center" }}
                    onClick={() => {
                      setProductUrl(c.pageUrl ?? "");
                      setProductCandidates(null);
                    }}
                  >
                    {c.imageUrl ? (
                      <img src={c.imageUrl} alt="" style={{ width: 40, height: 40, objectFit: "cover", borderRadius: 6 }} />
                    ) : (
                      <span style={{ width: 40, height: 40, display: "flex", alignItems: "center", justifyContent: "center", fontSize: "1.2rem" }}>🔗</span>
                    )}
                    <span>
                      <span className="card-title" style={{ fontSize: "0.85rem" }}>{c.title}</span>
                      {c.source && <span className="card-subtitle" style={{ display: "block" }}>{c.source}</span>}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        <div>
          <label className="card-subtitle" style={{ display: "block", marginBottom: "0.2rem" }}>
            Lenke til akkurat denne varen (valgfritt)
          </label>
          <input
            type="text"
            placeholder="https://… (fylles inn automatisk hvis du søker over)"
            value={productUrl}
            onChange={(e) => setProductUrl(e.target.value)}
          />
        </div>

        {isOnline && (
          <div className="stack" style={{ paddingLeft: "1.6rem" }}>
            <label className="row">
              <input type="checkbox" checked={dateUnknown} onChange={(e) => setDateUnknown(e.target.checked)} />
              <span>Vet ikke når jeg reiser ennå</span>
            </label>

            {!dateUnknown && (
              <div className="row">
                <div style={{ flex: 1 }}>
                  <label className="card-subtitle">Neste tur dit</label>
                  <input type="date" value={tripDate} onChange={(e) => setTripDate(e.target.value)} />
                </div>
                <div style={{ width: "6rem" }}>
                  <label className="card-subtitle">Dager før</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={leadTimeDays}
                    onChange={(e) => setLeadTimeDays(Number(e.target.value) || 0)}
                  />
                </div>
              </div>
            )}
          </div>
        )}

        <div className="row">
          <button className="btn" type="submit" disabled={saving || !name.trim()}>
            {editingId ? "Lagre endringer" : "Legg til"}
          </button>
          {editingId && (
            <button type="button" className="btn secondary" onClick={resetForm}>
              Avbryt redigering
            </button>
          )}
        </div>
      </form>

      <ShoppingListSection
        title="Bestilles på nett"
        items={onlineItems}
        locationName={locationName}
        onToggle={toggleChecked}
        onEdit={startEdit}
        onDelete={handleDelete}
        onAddToCalendar={handleAddToCalendar}
      />
      <ShoppingListSection
        title="Kjøpes hjemme"
        items={homeItems}
        locationName={locationName}
        onToggle={toggleChecked}
        onEdit={startEdit}
        onDelete={handleDelete}
        onAddToCalendar={handleAddToCalendar}
      />
      <ShoppingListSection
        title="Kjøpes på destinasjonen"
        items={destinationItems}
        locationName={locationName}
        onToggle={toggleChecked}
        onEdit={startEdit}
        onDelete={handleDelete}
        onAddToCalendar={handleAddToCalendar}
        showLocationTag
      />

      {items.length === 0 && <p className="empty-state">Handlelisten er tom.</p>}

      {checkedItems.length > 0 && (
        <div className="category-section" style={{ marginTop: "1rem" }}>
          <p className="category-title">Kjøpt</p>
          <ul className="checklist">
            {checkedItems.map((item) => (
              <li key={item.id} className="checklist-item checked">
                <input type="checkbox" checked={item.checked} onChange={() => void toggleChecked(item)} />
                <label>{item.name}</label>
                <button className="icon-btn" onClick={() => startEdit(item)} aria-label="Rediger">
                  ✏️
                </button>
                <button className="icon-btn" onClick={() => void handleDelete(item.id)} aria-label="Slett">
                  ✕
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function ShoppingListSection({
  title,
  items,
  locationName,
  onToggle,
  onEdit,
  onDelete,
  onAddToCalendar,
  showLocationTag,
}: {
  title: string;
  items: ShoppingListItem[];
  locationName: (id: string | null) => string;
  onToggle: (item: ShoppingListItem) => void;
  onEdit: (item: ShoppingListItem) => void;
  onDelete: (id: string) => void;
  onAddToCalendar: (item: ShoppingListItem) => void;
  showLocationTag?: boolean;
}) {
  if (items.length === 0) return null;

  return (
    <div className="category-section">
      <p className="category-title">{title}</p>
      <ul className="checklist">
        {items.map((item) => {
          const link = item.productUrl || item.storeUrl;
          return (
            <li key={item.id} className="checklist-item">
              <input type="checkbox" checked={item.checked} onChange={() => onToggle(item)} />
              <label style={{ display: "flex", flexDirection: "column" }}>
                <span>{item.name}</span>
                <span className="card-subtitle">
                  {showLocationTag && locationName(item.locationId)}
                  {item.storeName && `${showLocationTag ? " · " : ""}${item.storeName}`}
                  {item.orderByDate &&
                    `${showLocationTag || item.storeName ? " · " : ""}bestill innen ${item.orderByDate}`}
                  {item.needsDate && (
                    <span style={{ color: "var(--danger)" }}>
                      {showLocationTag || item.storeName ? " · " : ""}⚠️ Mangler dato for påminnelse
                    </span>
                  )}
                </span>
              </label>
              {link && (
                <a href={link} target="_blank" rel="noreferrer" className="icon-btn" title="Åpne lenke">
                  🔗
                </a>
              )}
              {item.orderByDate && (
                <button className="icon-btn" onClick={() => onAddToCalendar(item)} title="Legg til i kalender">
                  📅
                </button>
              )}
              <button className="icon-btn" onClick={() => onEdit(item)} aria-label="Rediger">
                ✏️
              </button>
              <button className="icon-btn" onClick={() => onDelete(item.id)} aria-label="Slett">
                ✕
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
