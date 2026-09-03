import { api } from "./client";
import type {
  CategoryTemplate,
  CustomStore,
  InventoryItem,
  Location,
  LocationType,
  PackingListData,
  ProductCandidate,
  Season,
  SearchResult,
  ShoppingListItem,
} from "../types";

export const locationsApi = {
  list: () => api.get<Location[]>("/api/locations"),
  create: (name: string, type: LocationType) => api.post<Location>("/api/locations", { name, type }),
  update: (id: string, name: string, type: LocationType) =>
    api.put<Location>(`/api/locations/${id}`, { name, type }),
  remove: (id: string) => api.delete<void>(`/api/locations/${id}`),
};

export const itemsApi = {
  listForLocation: (locationId: string) =>
    api.get<InventoryItem[]>(`/api/locations/${locationId}/items`),
  create: (locationId: string, category: string, name: string, brand?: string | null) =>
    api.post<InventoryItem>("/api/items", { locationId, category, name, brand: brand ?? null }),
  update: (id: string, category: string, name: string, brand?: string | null) =>
    api.put<InventoryItem>(`/api/items/${id}`, { category, name, brand: brand ?? null }),
  remove: (id: string) => api.delete<void>(`/api/items/${id}`),
  setPhoto: (id: string, sourceImageUrl: string) =>
    api.put<InventoryItem>(`/api/items/${id}/photo`, { sourceImageUrl }),
};

export const productLookupApi = {
  searchByText: (query: string, storeName?: string | null, storeUrl?: string | null) =>
    api.post<ProductCandidate[]>("/api/product-lookup/by-text", { query, storeName, storeUrl }),
  searchByPhoto: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.postForm<ProductCandidate[]>("/api/product-lookup/by-photo", formData);
  },
};

export const customStoresApi = {
  list: () => api.get<CustomStore[]>("/api/custom-stores"),
  create: (name: string, url: string | null) => api.post<CustomStore>("/api/custom-stores", { name, url }),
  remove: (id: string) => api.delete<void>(`/api/custom-stores/${id}`),
};

export const categoriesApi = {
  forLocationType: (locationType: LocationType) =>
    api.get<string[]>(`/api/categories?locationType=${locationType}`),
  create: (locationType: LocationType, name: string) =>
    api.post<string[]>("/api/categories", { locationType, name }),
};

export const categoryTemplatesApi = {
  forLocationType: (locationType: LocationType) =>
    api.get<CategoryTemplate[]>(`/api/category-templates?locationType=${locationType}`),
  updateItems: (locationType: LocationType, category: string, items: string[]) =>
    api.put<CategoryTemplate>(
      `/api/category-templates?locationType=${locationType}&category=${encodeURIComponent(category)}`,
      { items }
    ),
};

export const packingListsApi = {
  get: (locationId: string, season: Season) =>
    api.get<PackingListData>(`/api/packing-lists/${locationId}/${season}`),
  save: (locationId: string, season: Season, categories: PackingListData["categories"]) =>
    api.put<PackingListData>(`/api/packing-lists/${locationId}/${season}`, { categories }),
  reset: (locationId: string, season: Season) =>
    api.post<PackingListData>(`/api/packing-lists/${locationId}/${season}/reset`),
};

export const searchApi = {
  search: (query: string) => api.get<SearchResult[]>(`/api/search?q=${encodeURIComponent(query)}`),
};

export interface ReceiptItemCandidate {
  name: string;
  category: string;
  brand: string | null;
}

export const receiptImportApi = {
  extract: (locationType: LocationType, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.postForm<ReceiptItemCandidate[]>(
      `/api/receipt-import?locationType=${locationType}`,
      formData
    );
  },
};

export interface ShoppingListItemInput {
  name: string;
  locationId: string | null;
  tripDate: string | null;
  leadTimeDays: number | null;
  storeName: string | null;
  storeUrl: string | null;
  productUrl: string | null;
}

export const shoppingListApi = {
  list: () => api.get<ShoppingListItem[]>("/api/shopping-list"),
  create: (input: ShoppingListItemInput) => api.post<ShoppingListItem>("/api/shopping-list", input),
  update: (id: string, input: ShoppingListItemInput) =>
    api.put<ShoppingListItem>(`/api/shopping-list/${id}`, input),
  setChecked: (id: string, checked: boolean) =>
    api.put<ShoppingListItem>(`/api/shopping-list/${id}/checked`, { checked }),
  remove: (id: string) => api.delete<void>(`/api/shopping-list/${id}`),
};
