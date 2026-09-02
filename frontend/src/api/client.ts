import { auth } from "../firebase";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const idToken = await auth.currentUser?.getIdToken();
  if (!idToken) {
    throw new ApiError(401, "Not signed in");
  }

  const isFormData = options.body instanceof FormData;

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      // Let the browser set its own multipart Content-Type (with boundary) for FormData bodies.
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      Authorization: `Bearer ${idToken}`,
      ...options.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new ApiError(response.status, extractMessage(text) || `Request to ${path} failed with ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }),
  postForm: <T>(path: string, formData: FormData) => request<T>(path, { method: "POST", body: formData }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PUT", body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};

/** Spring's default error body is JSON with a "message" field; fall back to the raw text if it's not JSON. */
function extractMessage(text: string): string {
  try {
    const parsed = JSON.parse(text) as { message?: string };
    return parsed.message ?? text;
  } catch {
    return text;
  }
}

export { ApiError };
