import { api } from "./client";
import type {
  CategoryTemplate,
  InventoryItem,
  Location,
  LocationType,
  PackingListData,
  ProductCandidate,
  Season,
  SearchResult,
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
  create: (locationId: string, category: string, name: string) =>
    api.post<InventoryItem>("/api/items", { locationId, category, name }),
  update: (id: string, category: string, name: string) =>
    api.put<InventoryItem>(`/api/items/${id}`, { category, name }),
  remove: (id: string) => api.delete<void>(`/api/items/${id}`),
  setPhoto: (id: string, sourceImageUrl: string) =>
    api.put<InventoryItem>(`/api/items/${id}/photo`, { sourceImageUrl }),
};

export const productLookupApi = {
  searchByText: (query: string) =>
    api.post<ProductCandidate[]>("/api/product-lookup/by-text", { query }),
  searchByPhoto: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.postForm<ProductCandidate[]>("/api/product-lookup/by-photo", formData);
  },
};

export const categoriesApi = {
  forLocationType: (locationType: LocationType) =>
    api.get<string[]>(`/api/categories?locationType=${locationType}`),
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
