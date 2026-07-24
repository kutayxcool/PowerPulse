import { apiPost, getToken, setToken, clearToken } from "./api";

const USER_STORAGE_KEY = "powerpulse-auth-user";

function persistSession(authResponse) {
  setToken(authResponse.token);

  localStorage.setItem(
    USER_STORAGE_KEY,
    JSON.stringify({
      id: authResponse.userId,
      email: authResponse.email,
      name: authResponse.name,
    })
  );

  return getCurrentUser();
}

export async function registerAccount(name, email, password) {
  const response = await apiPost("/auth/register", {
    name,
    email,
    password,
  });

  return persistSession(response);
}

export async function login(email, password) {
  const response = await apiPost("/auth/login", { email, password });

  return persistSession(response);
}

export function logout() {
  clearToken();
  localStorage.removeItem(USER_STORAGE_KEY);
}

export function getCurrentUser() {
  const stored = localStorage.getItem(USER_STORAGE_KEY);

  if (!stored) {
    return null;
  }

  try {
    return JSON.parse(stored);
  } catch {
    return null;
  }
}

export function isAuthenticated() {
  return Boolean(getToken()) && Boolean(getCurrentUser());
}
