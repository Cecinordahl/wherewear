/**
 * Small curated list of stores for the shopping list's "which store"
 * autocomplete. Purely local/static - typing a name that matches one of
 * these (case-insensitively) auto-attaches its homepage link; anything else
 * is just saved as plain text with no link. This is a deliberate trade-off
 * (a free/instant local list beats a live external lookup - see README) and
 * a short starter set: add more entries here as you need them.
 */
export interface KnownStore {
  name: string;
  url: string;
}

export const KNOWN_STORES: KnownStore[] = [
  { name: "Bikbok", url: "https://bikbok.com/no/" },
  { name: "H&M", url: "https://www2.hm.com/no_no/index.html" },
  { name: "Zara", url: "https://www.zara.com/no/" },
  { name: "Vero Moda", url: "https://www.veromoda.com/no/" },
  { name: "Gina Tricot", url: "https://www.ginatricot.com/no/" },
  { name: "Cubus", url: "https://www.cubus.com/no-no/" },
  { name: "Kappahl", url: "https://www.kappahl.com/no-NO/" },
  { name: "Nelly", url: "https://nelly.com/no/" },
  { name: "Zalando", url: "https://www.zalando.no/" },
  { name: "Boozt", url: "https://www.boozt.com/no/no" },
  { name: "Amazon Spania", url: "https://www.amazon.es/" },
  { name: "IKEA", url: "https://www.ikea.com/no/no/" },
  { name: "Kicks", url: "https://www.kicks.no/" },
  { name: "Vinmonopolet", url: "https://www.vinmonopolet.no/" },
  { name: "Helly Hansen", url: "https://www.hellyhansen.com/no-no/" },
  { name: "Intersport", url: "https://www.intersport.no/" },
  { name: "XXL", url: "https://www.xxl.no/" },
];

export function findKnownStore(name: string): KnownStore | undefined {
  const normalized = name.trim().toLowerCase();
  return KNOWN_STORES.find((s) => s.name.toLowerCase() === normalized);
}
