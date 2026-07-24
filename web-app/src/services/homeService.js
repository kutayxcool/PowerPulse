import { apiGet, apiPost, apiPatch, apiDelete } from "./api";

export async function getHomes() {
  return apiGet("/homes");
}

export async function getHomeById(id) {
  return apiGet(`/homes/${id}`);
}

// Analytics/AI kartları appliances + dailyConsumption gibi detay alanlara
// ihtiyaç duyuyor, bunlar sadece /homes/{id} (detay) cevabında var,
// /homes (özet liste) cevabında yok. Bu yuzden liste + her ev icin detay
// cagrisini birlikte yapip birlestiriyoruz.
export async function getHomesWithDetails() {
  const summaries = await getHomes();
  const details = await Promise.all(
    summaries.map((home) => getHomeById(home.id))
  );
  return details;
}

export async function registerHome(payload) {
  return apiPost("/homes/register", payload);
}

export async function deleteHome(id) {
  return apiDelete(`/homes/${id}`);
}

export async function addAppliance(homeId, payload) {
  return apiPost(`/homes/${homeId}/appliances`, payload);
}

export async function removeAppliance(homeId, applianceId) {
  return apiDelete(`/homes/${homeId}/appliances/${applianceId}`);
}

// Cihazi "Durdur"/"Baslat" ile ya da bir zamanlayicinin suresi
// dolunca acip kapatmak icin kullanilir - sadece PostgreSQL'deki
// bayragi gunceller, fiili durma etkisi backend'de
// TelemetryProcessingService icinde uygulanir.
export async function updateApplianceStatus(homeId, applianceId, active) {
  return apiPatch(`/homes/${homeId}/appliances/${applianceId}/status`, {
    active,
  });
}