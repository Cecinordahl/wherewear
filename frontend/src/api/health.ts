import { auth } from "../firebase";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

/**
 * Standalone (not using the shared `request()` helper) so it can enforce
 * its own short timeout via AbortController - needed because during a
 * Render cold start, a plain fetch can otherwise hang far longer than we'd
 * want a single poll attempt to wait.
 */
export async function checkBackendHealth(timeoutMs: number): Promise<boolean> {
  const idToken = await auth.currentUser?.getIdToken().catch(() => undefined);
  if (!idToken) return false;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(`${BASE_URL}/api/health`, {
      headers: { Authorization: `Bearer ${idToken}` },
      signal: controller.signal,
    });
    return response.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(timeout);
  }
}
