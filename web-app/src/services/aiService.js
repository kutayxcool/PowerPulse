import { apiGet, apiPost } from "./api";

export async function getRecommendation(homeId) {
  return apiGet(`/homes/${homeId}/recommendation`);
}

export async function askAssistant(homeId, question) {
  return apiPost(`/homes/${homeId}/ask`, { question });
}
