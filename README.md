# Wherewear

Track what you have at each of your places (Spania, Bergen, Fjellet), and generate editable packing lists per location + season.

Monorepo:
- `backend/` — Java 21 + Spring Boot REST API (Maven)
- `frontend/` — React + TypeScript (Vite), deployed to `wherewear.vercel.app`
- `firestore.rules`, `firestore.indexes.json`, `firebase.json` — Firestore config, deployed via the Firebase CLI

This README is written assuming you're solid on Java/Spring and React/TS but newer to the deploy/scripting side, so every command below is meant to be copy-pasteable.

---

## 1. Create your Firebase project (do this first)

Everything else needs this. Firebase gives you both the Firestore database and the Auth (login) system, on the free "Spark" plan.

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and sign in with your Google account.
2. Click **Add project** → name it `wherewear` (or anything you like) → you can disable Google Analytics for this project (not needed) → **Create project**.
3. In the left sidebar, click **Build → Firestore Database** → **Create database** → choose a location close to you (e.g. `eur3 (europe-west)`) → start in **production mode** (our security rules, below, handle access control) → **Enable**.
4. In the left sidebar, click **Build → Authentication** → **Get started** → under **Sign-in method**, enable **Google** as a sign-in provider → **Save**.
5. Click the gear icon next to "Project Overview" → **Project settings**. Under **Your apps**, click the **</>** (web) icon → nickname it `wherewear-web` → **Register app**. You'll see a `firebaseConfig` object — you'll need these values for the frontend `.env` in step 3.
6. Still in Project settings, go to the **Service accounts** tab → **Generate new private key** → confirm. This downloads a JSON file. **Never commit this file** — it's already covered by `.gitignore`. You'll use its contents for the backend in step 2.

---

## 2. Run the backend locally

**Prerequisites:** Java 21 and Maven. Check with:
```bash
java -version
mvn -version
```
If you don't have them, install via [SDKMAN](https://sdkman.io/) (`sdk install java 21-tem && sdk install maven`) or Homebrew (`brew install openjdk@21 maven`).

1. Save the service-account JSON file you downloaded in step 1.6 somewhere outside the repo, e.g. `~/secrets/wherewear-firebase.json`.
2. From the `backend/` directory, run with that file's path as an env var:
   ```bash
   cd backend
   FIREBASE_SERVICE_ACCOUNT_PATH=~/secrets/wherewear-firebase.json mvn spring-boot:run
   ```
3. The API is now running at `http://localhost:8080`. Try `curl http://localhost:8080/api/locations` — you should get a `401` (expected, since you haven't sent an auth token yet — that confirms the server is up and the filter is working).

To just compile without running: `mvn -DskipTests package`. To run the unit tests: `mvn test`.

---

## 3. Run the frontend locally

**Prerequisites:** Node.js (18+) and npm.

1. From the `frontend/` directory, copy the env template and fill it in with the `firebaseConfig` values from step 1.5:
   ```bash
   cd frontend
   cp .env.example .env
   ```
   Edit `.env` and paste in `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, etc. Leave `VITE_API_BASE_URL=http://localhost:8080` as-is for local dev.
2. Install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```
3. Open the printed local URL (usually `http://localhost:5173`). Sign in with Google — this creates your `users/{uid}` document and seeds your starter category templates automatically on first sign-in.

---

## 4. Opening in IntelliJ

Open the repo root (`wherewear/`) as the IntelliJ project. IntelliJ should detect `backend/pom.xml` and offer to import it as a Maven module — accept that. The `frontend/` folder can stay as a plain directory (IntelliJ's JS support will pick up `package.json` for basic editing support); you'll run `npm run dev` from a terminal rather than through IntelliJ's run configurations.

---

## 5. Deploying Firestore security rules

The rules in `firestore.rules` restrict every read/write to `request.auth.uid == resource.data.userId` — this is the concrete access-control mitigation mentioned in the GDPR notes below. Deploy them once (and again any time you edit `firestore.rules`):

```bash
npm install -g firebase-tools   # one-time, global CLI install
firebase login                  # opens a browser to sign in
firebase use --add              # pick your wherewear Firebase project, alias it "default"
firebase deploy --only firestore
```

---

## 6. Deploying the backend to Render (free tier)

1. Push this repo to GitHub (if you haven't already).
2. Go to [dashboard.render.com](https://dashboard.render.com) → **New +** → **Web Service** → connect your GitHub repo.
3. Render should detect `render.yaml` at the repo root and offer to create the service from it (a "Blueprint"). If it doesn't, create the service manually with:
   - **Root Directory**: leave blank (repo root)
   - **Environment**: Docker
   - **Dockerfile Path**: `backend/Dockerfile`
   - **Docker Build Context Directory**: `backend`
   - **Plan**: Free
4. Under the service's **Environment** tab, add:
   - `FIREBASE_SERVICE_ACCOUNT_JSON` — paste the **entire contents** of the service-account JSON file from step 1.6, as a single value.
   - `WHEREWEAR_ALLOWED_ORIGINS` — `https://wherewear.vercel.app` (already set if using the Blueprint).
   - `SERPAPI_API_KEY` — optional, only needed for the product photo lookup feature (see step 8).
   - `GEMINI_API_KEY` — optional, only needed for the receipt import feature (see step 10).
5. Deploy. Note the resulting URL, e.g. `https://wherewear-backend.onrender.com` — you'll need it in step 7.

**Free-tier limitation to know about:** Render's free web services "sleep" after ~15 minutes of no traffic, and the next request wakes it up — which can take 30-60 seconds (occasionally longer). The frontend handles this explicitly: right after sign-in, it polls `GET /api/health` (`frontend/src/components/BackendWakeGate.tsx`) and blocks the whole app behind a "Vekker serveren …" screen until the backend actually responds, so you can't submit a form into a sleeping backend and get a confusing failure. If it's still not responding after 90 seconds, it shows a retry button instead of waiting forever.

---

## 7. Deploying the frontend to Vercel (free tier)

1. Go to [vercel.com/new](https://vercel.com/new) and import the same GitHub repo.
2. Set **Root Directory** to `frontend`.
3. Set the **Project Name** to `wherewear` — this is what makes the deployed URL `wherewear.vercel.app`.
4. Under **Environment Variables**, add all the `VITE_FIREBASE_*` values from your `.env`, plus:
   - `VITE_API_BASE_URL` = your Render URL from step 6.5 (e.g. `https://wherewear-backend.onrender.com`)
5. Deploy.
6. Back in the Firebase console → **Authentication → Settings → Authorized domains**, add `wherewear.vercel.app` (Google sign-in only works from authorized domains).

---

## API overview

Base URL: backend root. All `/api/**` routes require `Authorization: Bearer <Firebase ID token>`.

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/locations` | list / create locations |
| PUT/DELETE | `/api/locations/{id}` | update / delete a location |
| GET | `/api/locations/{locationId}/items` | inventory at a location |
| POST | `/api/items` | add an inventory item |
| PUT/DELETE | `/api/items/{id}` | update / delete an inventory item |
| PUT | `/api/items/{id}/photo` | attach a found product photo to an item |
| POST | `/api/product-lookup/by-text` | search for a product by name/description |
| POST | `/api/product-lookup/by-photo` | search for a product by uploaded photo (multipart) |
| GET | `/api/categories?locationType=FLIGHT\|CABIN` | fixed category list for that location type |
| GET | `/api/category-templates?locationType=...` | your template item lists |
| PUT | `/api/category-templates?locationType=...&category=...` | replace a template's item list |
| GET | `/api/packing-lists/{locationId}/{season}` | get (or first-time generate) a packing list |
| PUT | `/api/packing-lists/{locationId}/{season}` | save your edited list |
| POST | `/api/packing-lists/{locationId}/{season}/reset` | regenerate from templates + inventory |
| GET | `/api/search?q=...` | search inventory by name |
| POST | `/api/receipt-import?locationType=...` | extract candidate items from a receipt photo (multipart) |
| GET/POST | `/api/shopping-list` | list / add shopping list items |
| PUT/DELETE | `/api/shopping-list/{id}` | update / delete a shopping list item |
| PUT | `/api/shopping-list/{id}/checked` | mark bought / not bought |
| GET/POST | `/api/custom-stores` | list / add your own stores (beyond the hardcoded starter list) |
| DELETE | `/api/custom-stores/{id}` | remove a store you added |

`season` is one of `VINTER`, `VAR`, `SOMMER`, `HOST` (ASCII names in the API/URLs; the frontend displays them as Vinter/Vår/Sommer/Høst).

---

## 8. Product photo lookup (optional)

Lets you find a real product photo for an item — by pasting/typing its name, or uploading a photo of it (e.g. an Instagram screenshot) — instead of taking your own picture. Tap the 📷 next to any inventory item that doesn't have a photo yet.

**How it works:** the backend calls [SerpAPI](https://serpapi.com) (Google Shopping for text search, Google Lens for photo search), you pick the right match from the results, and the backend fetches + compresses that photo and stores it directly on the item (as a small base64 JPEG in Firestore — not Firebase Storage, so this stays on the free Spark plan; storing images inline like this is a deliberate MVP shortcut, fine at the scale of one wardrobe).

For the photo-search path specifically, since SerpAPI's Google Lens engine needs a public URL (not a raw upload), your photo is briefly relayed through [0x0.st](https://0x0.st), a free anonymous file host, just long enough for SerpAPI to fetch it. It's a small third-party service (not Google-grade reliability), and the photo is technically public at an unguessable URL for a few seconds — worth knowing since it's your photo, however briefly public.

**Setup:**
1. Go to [serpapi.com](https://serpapi.com) → sign up (free) → **Your Account** → copy your **API Key**.
2. Locally: add it to your backend run command, e.g.
   ```bash
   FIREBASE_SERVICE_ACCOUNT_PATH=~/secrets/wherewear-firebase.json SERPAPI_API_KEY=your-key-here mvn spring-boot:run
   ```
3. On Render: add `SERPAPI_API_KEY` as an environment variable (see step 6.4 above).

**Free-tier limit:** 100 searches/month total (text + photo searches share the same pool). Without a key configured, the feature just shows a friendly "not set up yet" message — everything else in the app works fine regardless.

---

## 9. Shopping list ("Handleliste")

A simple "need to buy" list, separate from inventory. The **"Hvor skal det kjøpes?"** dropdown answers both "where" and "how", in three visually separate groups: **Bestilling** (Online), **Standard** (Hjemme, Hvor som helst), and **Steder** (your real locations) — Online and Hjemme are fixed convenience defaults, not real entries in Steder. The list groups into three sections in this order: **Bestilles på nett**, **Kjøpes hjemme**, **Kjøpes på destinasjonen**.

For **Online** items (e.g. a steamer from Amazon Spain that needs lead time to arrive), you either set a **trip date** + **lead time in days** (the app computes an "order by" date, and a banner appears at the top once that date arrives), or check **"Vet ikke når jeg reiser ennå"** if you don't know yet — the item stays flagged with "⚠️ Mangler dato for påminnelse" until you come back and add one. Tap **📅** on any item with a date set to download a calendar reminder (`.ics`) with a built-in alarm — opening the downloaded file adds it straight to Calendar (Apple/Google/Outlook all support this) and fires a real notification on the order-by date. There's no public API to add directly to Apple's Reminders app from a website, so this targets Calendar instead — same practical result (a notification at the right time).

Any item can also optionally have a **Butikk** (store) and a direct **product link**. The store field is a custom searchable dropdown (`frontend/src/components/StoreAutocomplete.tsx`) — native `<datalist>` was tried first but is unreliable on iOS Safari, hence the custom component. It searches a small hardcoded starter list (`frontend/src/stores.ts`) merged with stores **you've** added, persisted server-side via `/api/custom-stores` (so the list grows the more you use it, and syncs across devices). Typing a name that matches nothing offers **"➕ Legg til «name» som ny butikk"**, with an optional homepage URL field — adding the URL is worth doing, since it's what makes the product search below actually reliable for that store. Picking a known/saved store auto-attaches its homepage as a 🔗 link on the item.

Once you've entered both an item name and a store, a **"🔍 Søk etter produktlenke"** button appears. When the store's URL is known, this searches Google (not Shopping) scoped to that store's site (`site:domain.com`) — Google Shopping only covers retailers in Google's own Shopping Graph, so it can come back empty for smaller/regional stores (e.g. Clas Ohlson) even when the product is right there on their site; a site-scoped regular search is far more reliable. Without a known store URL, it falls back to a generic Shopping search of "item + store name" (best-effort). Pick a result to fill in the product link automatically, or paste the exact URL into "Lenke til akkurat denne varen" if you already have it (that field always takes priority when set). This reuses the same SerpAPI setup as the [product photo lookup](#8-product-photo-lookup-optional) and shares its 100/month free-tier quota — see step 8.

No setup needed for the store-name autocomplete, adding your own stores, or manual link paste. The "search for product link" button needs the same `SERPAPI_API_KEY` as step 8.

---

## 10. Receipt import (optional)

Lets you bulk-add inventory items from a photo of a receipt instead of typing each one in by hand. From a location's page, tap **"📄 Importer fra kvittering"** → take/upload a photo → the backend sends it to Gemini (Google's vision-capable LLM) asking it to list every purchased item with a clean name and the best-fitting category from that location's actual category list → you get an editable review screen. Every row defaults to the location you started from; you can rename an item, change its category, move it to a **different** location, or remove it entirely before tapping **"Legg til N varer"** to bulk-save.

**Why Gemini and not Claude/GPT-4V:** Google AI Studio's Gemini API has a genuinely free tier, keeping this consistent with the rest of the app's free-tier integrations — a Claude/OpenAI vision call would work just as well technically, but costs real money per receipt.

**Setup:**
1. Go to [aistudio.google.com](https://aistudio.google.com) → sign in → **Get API key** → create one (free).
2. Locally: add it to your backend run command, e.g.
   ```bash
   FIREBASE_SERVICE_ACCOUNT_PATH=~/secrets/wherewear-firebase.json GEMINI_API_KEY=your-key-here mvn spring-boot:run
   ```
3. On Render: add `GEMINI_API_KEY` as an environment variable (see step 6.4 above).

**Notes:** extraction quality depends on photo clarity and receipt layout - expect to correct a name or category occasionally, same "best-effort, you confirm" spirit as the other AI-assisted features. The model id is configurable via `GEMINI_MODEL` (defaults to `gemini-2.0-flash`) in case Google retires that model name later. Without a key configured, the feature shows a friendly "not set up yet" message — everything else in the app works fine regardless.

---

## GDPR / privacy notes

*Not legal advice — flagging what I see and how the code addresses it, so you can decide if you want a lawyer's opinion before inviting others.*

- **Legal basis**: the data here (clothing/gear item names, tied to a location) is personal data under GDPR but not a special category. For a personal utility tool you use yourself, **legitimate interest** (or, once you invite others and they explicitly opt in, **contract necessity** for providing the service they signed up for) is the applicable basis. Nothing here is sensitive-category data (health, political views, etc.), so the higher bar for special-category processing doesn't apply.
- **Access control (implemented)**: `firestore.rules` enforces `request.auth.uid == resource.data.userId` on every collection, so one user's data is never readable or writable by another, even via a compromised or malicious frontend. This only protects **direct Firestore access** — since all client traffic actually goes through the Spring Boot backend (using the Admin SDK, which bypasses rules by design), the backend's own per-request auth check (`FirebaseAuthFilter` + ownership checks in each service) is the real enforcement point today. Keep the rules deployed anyway as defense-in-depth for if you ever add direct client-to-Firestore reads.
- **Privacy notice**: not needed for solo use. **Before inviting friends**, add a short privacy notice (what's collected, why, that it's not shared between accounts, how to request deletion) — a simple static page is enough at this scale; you don't need a cookie banner since there's no tracking/analytics here.
- **Data deletion**: there's currently no "delete my account and all my data" endpoint. Flag this as a **before-you-invite-others checklist item** — worth adding a `DELETE /api/me` that cascades through locations/items/templates/packingLists for that `userId`, since GDPR gives users a right to erasure once more than one person's data is involved.
- **Data minimization**: the model stores what's needed (item names, categories, locations, and now optionally a product photo) — no location coordinates, no tracking.
- **Third-party data flow (product photo lookup)**: using the optional photo-search feature sends the photo to SerpAPI (and briefly, to the anonymous file host 0x0.st) — both outside Google's ecosystem. This is opt-in per item (only triggered when you tap 📷) and only relevant once this feature is in use; worth knowing if you ever write a privacy notice for other users.

---

## What's not built yet (by design, per MVP scope)

- Taking/uploading your own photos of items (only found-product photos, via the lookup feature, are supported)
- Sharing/inviting friends (data model supports multi-user via `userId`, but there's no invite UI)
- In-app or push notifications (reminders work via exporting a Calendar event instead — see "Shopping list" section)
- Account deletion endpoint (see GDPR notes above)
