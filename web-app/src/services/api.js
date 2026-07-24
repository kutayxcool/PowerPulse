const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// Giris/kayit sonrasi alinan JWT burada saklanir - her istekte
// "Authorization: Bearer <token>" header'i olarak eklenir. Backend
// artik /api/auth/** disindaki her ucta kimlik dogrulama istiyor.
const TOKEN_STORAGE_KEY = "powerpulse-auth-token";

export function getToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

// Backend'in ApiErrorResponse ile ayni sekilde: gercek HTTP durum
// kodunu tasir - App.jsx bir 401 aldiginda oturumun suresinin
// dolmus/gecersiz oldugunu anlayip kullaniciyi giris ekranina
// dondurebilir.
export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function buildHeaders(extraHeaders = {}) {
  const token = getToken();

  return {
    ...extraHeaders,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

// Backend hatalari ApiErrorResponse { status, error, message, path,
// timestamp } seklinde JSON doner - varsa o mesaj kullanilir, yoksa
// (ör. tamamen beklenmedik bir hata) genel bir mesaja duser.
async function extractErrorMessage(response, path) {
  try {
    const data = await response.clone().json();
    if (data && typeof data.message === "string") {
      return data.message;
    }
  } catch {
    // JSON degilse asagida duz metin/varsayilan mesaja duser.
  }

  const fallbackText = await response.text().catch(() => "");

  return (
    fallbackText ||
    `API isteği başarısız oldu: ${response.status} ${path}`
  );
}

export async function apiGet(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: buildHeaders(),
  });

  if (!response.ok) {
    throw new ApiError(
      await extractErrorMessage(response, path),
      response.status
    );
  }

  return response.json();
}

export async function apiPost(path, body) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: buildHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new ApiError(
      await extractErrorMessage(response, path),
      response.status
    );
  }

  return response.json();
}

export async function apiPatch(path, body) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "PATCH",
    headers: buildHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new ApiError(
      await extractErrorMessage(response, path),
      response.status
    );
  }

  return response.json();
}

export async function apiDelete(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "DELETE",
    headers: buildHeaders(),
  });

  if (!response.ok) {
    throw new ApiError(
      await extractErrorMessage(response, path),
      response.status
    );
  }

  // Core 204 No Content dönüyor, gövdede JSON yok.
  return true;
}

export default API_BASE_URL;
