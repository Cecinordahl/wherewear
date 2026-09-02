export type LocationType = "FLIGHT" | "CABIN";

export type Season = "VINTER" | "VAR" | "SOMMER" | "HOST";

export const SEASONS: Season[] = ["VINTER", "VAR", "SOMMER", "HOST"];

export const SEASON_LABELS: Record<Season, string> = {
  VINTER: "Vinter",
  VAR: "Vår",
  SOMMER: "Sommer",
  HOST: "Høst",
};

export const LOCATION_TYPE_LABELS: Record<LocationType, string> = {
  FLIGHT: "Flydestinasjon",
  CABIN: "Hytte",
};

export interface Location {
  id: string;
  name: string;
  type: LocationType;
}

export interface InventoryItem {
  id: string;
  locationId: string;
  category: string;
  name: string;
  photoDataUrl?: string;
}

export interface ProductCandidate {
  title: string;
  source: string | null;
  pageUrl: string | null;
  imageUrl: string | null; // null for organic (site-scoped) web results, which don't carry a thumbnail
}

export interface CustomStore {
  id: string;
  name: string;
  url: string | null;
}

export interface CategoryTemplate {
  category: string;
  items: string[];
}

export interface PackingItem {
  name: string;
  checked: boolean;
}

export interface PackingCategory {
  category: string;
  items: PackingItem[];
}

export interface PackingListData {
  locationId: string;
  season: Season;
  categories: PackingCategory[];
}

export interface SearchResult {
  itemId: string;
  itemName: string;
  category: string;
  locationId: string;
  locationName: string;
}

// Fixed pseudo-locations - not real Locations, just convenience defaults
// (matches backend ShoppingListDtos sentinel constants).
export const HOME_LOCATION_ID = "HOME";
export const ONLINE_LOCATION_ID = "ONLINE";

export interface ShoppingListItem {
  id: string;
  name: string;
  locationId: string | null;
  checked: boolean;
  tripDate: string | null; // yyyy-MM-dd
  leadTimeDays: number | null;
  orderByDate: string | null; // yyyy-MM-dd, computed
  dueSoon: boolean;
  needsDate: boolean; // online, but no tripDate set yet
  storeName: string | null;
  storeUrl: string | null;
  productUrl: string | null;
}
