# PowerPulse — Entegrasyon Sözleşmeleri (Contract-First)

Bu belge, ekip üyelerinin birbirini beklemeden bağımsız geliştirme yapabilmesi için sabitlenmiş arayüzleri tanımlar. Esra'nın frontend taraflı sözleşme önerisi buraya işlendi (v2). AI Advisory Service'in ayrı mikroservis olarak HTTP sözleşmesi eklendi (v3). Buradaki şemalar/interface imzaları değişmeden önce ekipçe konuşulmalı.

## 0. Kimlik (ID) Kararı — Ekip Kararı

Ev ve cihaz `id` alanları **UUID (string)** olacak, sayısal (Long/Integer) değil. Esra'nın örnek JSON'larındaki `1`, `2` gibi sayılar sadece örnekti; frontend tarafında `id` opak bir string olarak ele alınmalı (karşılaştırma/eşitlik dışında sayı gibi işlem yapılmayacak). Bunun nedeni: Kafka telemetry/registration akışında zaten UUID kullanıyoruz, ayrı bir sayısal-ID eşleme katmanı eklemek gereksiz karmaşıklık yaratır. Tarık'ın Core'da DB primary key'i UUID olarak tanımlaması gerekiyor.

## 1. Kafka Konuları (Topics)

### 1.1 `telemetry` — Sensörden gelen anlık tüketim verisi
**Üreten:** Kutay (Telemetry Sensors) · **Tüketen:** Tarık (Core)

```json
{
  "homeId": "uuid",
  "applianceId": "uuid",
  "applianceName": "Buzdolabı",
  "wattage": 1450.0,
  "timestamp": "2026-07-21T10:15:30+03:00"
}
```

Not: Telemetry Sensors simülatörü çalışıyor (bkz. `sensors/`), demo veriler için sabit UUID'ler kullanılıyor:
- Kadıköy Evi: `11111111-1111-1111-1111-111111111111` (Klima, Buzdolabı, Çamaşır Makinesi)
- Beşiktaş Evi: `11111111-1111-1111-1111-111111111112` (Klima, Fırın, Televizyon)

### 1.2 `registration` — Ev/cihaz kayıt ve bütçe bilgisi
**Üreten:** Esra (Web App, kullanıcı kayıt formu üzerinden) · **Tüketen:** Tarık (Core)

```json
{
  "homeId": "uuid",
  "contactEmail": "kullanici@example.com",
  "budgetQuotaKwh": 300.0,
  "appliances": [
    { "applianceId": "uuid", "name": "Klima", "safeLimitWatt": 2000.0 }
  ]
}
```

## 2. REST API — Web App ↔ Core
**Sağlayan:** Tarık (Core) · **Çağıran:** Esra (Web App)

**Base URL:** `http://localhost:8080` (frontend `.env`'de `VITE_API_BASE_URL=http://localhost:8080/api` olarak kullanacak — kod içine gömülmeyecek)

### 2.1 `GET /api/homes` — Dashboard ev listesi
```json
[
  {
    "id": "uuid",
    "name": "Kadıköy Evi",
    "consumption": 269.5,
    "bill": 565.95,
    "quotaPercentage": 99,
    "status": "warning"
  }
]
```
`status` mantığı (görsel gösterge, ceza kademesinden bağımsız):
- `quotaPercentage < 90` → `normal`
- `90–100` → `warning`
- `> 100` → `danger`

### 2.2 `GET /api/homes/{id}` — Ev detayı (appliances + son 7 gün tüketim)
```json
{
  "id": "uuid",
  "name": "Kadıköy Evi",
  "consumption": 269.5,
  "bill": 565.95,
  "quotaPercentage": 99,
  "status": "warning",
  "appliances": [
    { "id": "uuid", "name": "Klima", "watt": 1200, "status": "warning" }
  ],
  "dailyConsumption": [
    { "day": "Pzt", "consumption": 30 }
  ]
}
```

### 2.3 `GET /api/analytics`
```json
{
  "dailyTotalConsumption": [
    { "day": "Pzt", "consumption": 120.5 }
  ],
  "homeComparison": [
    { "homeId": "uuid", "homeName": "Kadıköy Evi", "consumption": 269.5, "bill": 565.95, "quotaPercentage": 99 }
  ]
}
```

### 2.4 `GET /api/dashboard/summary` (opsiyonel — backend hesaplarsa)
```json
{
  "totalHomes": 4,
  "totalConsumption": 1050.4,
  "totalBill": 2205.84,
  "quotaExceededHomes": 2
}
```
Karar: Backend hesaplayıp döndürecek (fatura/kota kuralları zaten backend sorumluluğunda, frontend'in tekrar hesaplamasına gerek yok).

### 2.5 `GET /api/ai/recommendation` (genel) veya `GET /api/homes/{id}/recommendation` (eve özel)
```json
{
  "title": "Enerji Tasarrufu Önerisi",
  "homeId": "uuid",
  "homeName": "Kadıköy Evi",
  "recommendations": [
    "Klima kullanım süresini azaltabilirsiniz.",
    "Çamaşır makinesini düşük tarife saatlerinde çalıştırabilirsiniz."
  ],
  "estimatedSavingPercentage": 14,
  "estimatedSavingAmount": 79.23,
  "generatedAt": "2026-07-21T16:30:00+03:00"
}
```
AI servisi hata verirse yalnızca bu endpoint hata döner, dashboard'un geri kalanı etkilenmez (bkz. 503 aşağıda). Core, bu response'u üretmek için dahili olarak AI Advisory Service'i çağırır (bkz. bölüm 3.1).

## 3. Java Servis Arayüzü — `EnergyAdvisoryService` (v2 — güncellendi)
**Sağlayan:** Kutay (AI Advisory Service) · **Çağıran:** Tarık (Core, `/api/ai/recommendation` içinde sarmalanır)

> Değişiklik: Önceki versiyonda tek bir `String` dönüyordu. Esra'nın istediği yapılandırılmış response için artık bir kayıt (record) dönüyor.

```java
public interface EnergyAdvisoryService {
    AdvisoryResult generateAdvisory(EnergyAdvisoryContext context);
}

public record EnergyAdvisoryContext(
    String homeId,
    String homeName,
    double totalConsumptionKwh,
    double budgetQuotaKwh,
    double currentBillAmount,
    boolean quotaBreached,
    List<ApplianceAnomaly> anomalies
) {}

public record ApplianceAnomaly(String applianceName, int consecutiveBreaches) {}

public record AdvisoryResult(
    String title,
    List<String> recommendations,
    double estimatedSavingPercentage,
    double estimatedSavingAmount
) {}
```

Core, bu `AdvisoryResult`'ı alıp `homeId`, `homeName` ve `generatedAt` (ISO 8601, timezone'lu) ile sarmalayarak REST response'una çevirir.

### 3.1 AI Advisory Service — ayrı mikroservis, internal HTTP sözleşmesi (v3 — yeni)

Mimari diyagramda AI Advisory Service, Core'dan ayrı bir kutu olarak gösterildi. Bu yüzden `EnergyAdvisoryService` Java arayüzü aynı JVM içinde import edilmiyor; Kutay'ın `ai-advisory` servisi **kendi portunda (8081)** ayrı bir Spring Boot uygulaması olarak çalışır (`ai-advisory/` klasörü), Core buna HTTP üzerinden bağlanır.

**Endpoint:** `POST http://localhost:8081/internal/advisory`
**Request body:** `EnergyAdvisoryContext` (yukarıdaki JSON şeması)
**Response body:** `AdvisoryResult` (yukarıdaki JSON şeması)
**Hata:** AI çağrısı başarısız olursa (Gemini API hatası, `GEMINI_API_KEY` eksik vb.) `503` + bölüm 6'daki ortak hata formatı döner.

Core, `/api/ai/recommendation` isteğini aldığında arka planda bu internal endpoint'i çağırıp sonucu sarmalar. `AI_ADVISORY_BASE_URL` env değişkeni ile Core'a bu servisin adresi verilecek (local'de `http://localhost:8081`).

Gemini'ye gönderilen prompt, modelin **sadece** `AdvisoryResult` şemasına uygun JSON döndürmesini ister (markdown/açıklama olmadan); `ai-advisory` servisi olası ```` ```json ```` code fence'lerini otomatik temizleyip parse eder.

## 4. Kademeli Ceza Tarifesi (Ekip Kararı — spec dışı ek kural)

Orijinal ödev tek seviyeli ("premium penalty rate") bir ceza tanımlıyor. Ekip kararıyla kademeli hale getirdik; bu hesaplama **Core (Tarık)** tarafında yapılır ve doğrudan `bill` alanına yansır. `status` (normal/warning/danger) alanı sadece görsel gösterge olarak kalır, ceza kademesi ayrı bir iç hesaptır:

| Kota Aşımı | Kademe | Ceza Çarpanı |
|---|---|---|
| %100–119 | 1 | 1.5x |
| %120–139 | 2 | 2x |
| %140–159 | 3 | 2.5x |
| Her ek %20 | +1 kademe | +0.5x |

## 5. Sayısal Değer Formatı

Backend değerleri birimleriyle birlikte string döndürmez. `269.5` doğru, `"269.5 kWh"` yanlış. Birim eklemek (kWh, TL, %, W) frontend'in işi.

## 6. Hata Response Formatı (tüm endpoint'lerde ortak)
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "İstenen ev bulunamadı.",
  "path": "/api/homes/15",
  "timestamp": "2026-07-21T16:30:00+03:00"
}
```

| Kod | Anlamı |
|---|---|
| 200 | Başarılı GET |
| 201 | Başarılı kayıt oluşturma |
| 400 | Geçersiz istek |
| 404 | Ev veya cihaz bulunamadı |
| 500 | Sunucu hatası |
| 503 | AI veya telemetry servisi kullanılamıyor |

## 7. CORS

Backend, `http://localhost:5173` origin'ine izin vermeli. Spring Boot'ta tek tek controller'da `@CrossOrigin` yerine merkezi (global) `CorsConfigurationSource` bean'i kullanılacak (CryptoScope'ta yaptığımız gibi).

## 8. Canlı Veri / Polling (v1)

İlk sürüm için normal REST polling yeterli: Frontend `/api/homes`'u **5 saniyede bir** çağırır, backend en son Kafka/telemetry kaydını yansıtacak şekilde bu sıklığı sorunsuz karşılayabilmeli. WebSocket/SSE ileride değerlendirilecek, v1 kapsamında değil.

## 9. Tarih-Saat Formatı

ISO 8601, timezone'lu: `2026-07-21T16:30:00+03:00`.

## 10. Geliştirme Sırası (Öncelik)

1. Veri modelleri ve JSON alan isimleri (bu belge)
2. `GET /api/homes` (ilk etapta mock JSON dönebilir)
3. `GET /api/homes/{id}`
4. CORS ayarı
5. `GET /api/analytics`
6. Hata response yapısı
7. `GET /api/ai/recommendation` (AI Advisory Service hazır — bkz. bölüm 3.1)
8. Kafka/telemetry gerçek veri entegrasyonu (Telemetry Sensors hazır — bkz. bölüm 1.1)

## 11. Ortam Değişkenleri (herkes için ortak)

| Değişken | Açıklama |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` (local) |
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | docker-compose'daki `powerpulse_db` bilgileri (port: **5433**, host'ta 5432 çakışması nedeniyle değiştirildi) |
| `IGNITE_HOST` | `localhost:10800` |
| `GEMINI_API_KEY` | AI Advisory Service için, export edilecek |
| `GEMINI_MODEL` | varsayılan: `gemini-3-flash-preview` |
| `AI_ADVISORY_BASE_URL` | Core için, varsayılan: `http://localhost:8081` |
| `VITE_API_BASE_URL` | Frontend `.env`, `http://localhost:8080/api` |
