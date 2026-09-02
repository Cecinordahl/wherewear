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
5. Deploy. Note the resulting URL, e.g. `https://wherewear-backend.onrender.com` — you'll need it in step 7.

**Free-tier limitation to know about:** Render's free web services "sleep" after ~15 minutes of no traffic, and the next request wakes it up — which can take 30-60 seconds. The first packing-list load after a while idle may feel slow; that's this, not a bug.

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
| GET | `/api/categories?locationType=FLIGHT\|CABIN` | fixed category list for that location type |
| GET | `/api/category-templates?locationType=...` | your template item lists |
| PUT | `/api/category-templates?locationType=...&category=...` | replace a template's item list |
| GET | `/api/packing-lists/{locationId}/{season}` | get (or first-time generate) a packing list |
| PUT | `/api/packing-lists/{locationId}/{season}` | save your edited list |
| POST | `/api/packing-lists/{locationId}/{season}/reset` | regenerate from templates + inventory |
| GET | `/api/search?q=...` | search inventory by name |

`season` is one of `VINTER`, `VAR`, `SOMMER`, `HOST` (ASCII names in the API/URLs; the frontend displays them as Vinter/Vår/Sommer/Høst).

---

## GDPR / privacy notes

*Not legal advice — flagging what I see and how the code addresses it, so you can decide if you want a lawyer's opinion before inviting others.*

- **Legal basis**: the data here (clothing/gear item names, tied to a location) is personal data under GDPR but not a special category. For a personal utility tool you use yourself, **legitimate interest** (or, once you invite others and they explicitly opt in, **contract necessity** for providing the service they signed up for) is the applicable basis. Nothing here is sensitive-category data (health, political views, etc.), so the higher bar for special-category processing doesn't apply.
- **Access control (implemented)**: `firestore.rules` enforces `request.auth.uid == resource.data.userId` on every collection, so one user's data is never readable or writable by another, even via a compromised or malicious frontend. This only protects **direct Firestore access** — since all client traffic actually goes through the Spring Boot backend (using the Admin SDK, which bypasses rules by design), the backend's own per-request auth check (`FirebaseAuthFilter` + ownership checks in each service) is the real enforcement point today. Keep the rules deployed anyway as defense-in-depth for if you ever add direct client-to-Firestore reads.
- **Privacy notice**: not needed for solo use. **Before inviting friends**, add a short privacy notice (what's collected, why, that it's not shared between accounts, how to request deletion) — a simple static page is enough at this scale; you don't need a cookie banner since there's no tracking/analytics here.
- **Data deletion**: there's currently no "delete my account and all my data" endpoint. Flag this as a **before-you-invite-others checklist item** — worth adding a `DELETE /api/me` that cascades through locations/items/templates/packingLists for that `userId`, since GDPR gives users a right to erasure once more than one person's data is involved.
- **Data minimization**: the model already only stores what's needed (item names, categories, locations) — no photos, no location coordinates, no tracking. Good as-is.

---

## What's not built yet (by design, per MVP scope)

- Photos of items, AI-based item recognition
- Sharing/inviting friends (data model supports multi-user via `userId`, but there's no invite UI)
- Notifications/reminders
- Account deletion endpoint (see GDPR notes above)
