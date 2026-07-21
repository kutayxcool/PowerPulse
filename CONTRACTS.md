# PowerPulse — Entegrasyon Sözleşmeleri (Contract-First)

Bu belge, ekip üyelerinin birbirini beklemeden bağımsız geliştirme yapabilmesi için sabitlenmiş arayüzleri tanımlar. Esra'nın frontend taraflı sözleşme önerisi buraya işlendi (v2). AI Advisory Service'in ayrı mikroservis olarak HTTP sözleşmesi eklendi (v3). Ignite anlık veri cache sözleşmesi eklendi (v4). Registration topic akış yönü orijinal ödev PDF'ine göre düzeltildi (v5). Buradaki şemalar/interface imzaları değişmeden önce ekipçe konuşulmalı.

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

### 1.2 `registration` — Ev/cihaz kayıt ve bütçe bilgisi (v5 — akış yönü düzeltildi)
**Üreten:** Tarık (Core) · **Tüketen:** Kutay (Telemetry Sensors)

> Düzeltme: Önceki versiyonda "Esra'nın Web App'i doğrudan Kafka'ya yayınlıyor" yazıyordu — bu yanlıştı. Orijinal ödev PDF'i (bölüm 5.2.1, "Home Registration Endpoint") şunu açıkça belirtiyor: Esra'nın Web App'i bir **REST** isteği (`POST` — Core'da tanımlanacak) atar, **Core** bu isteği alıp PostgreSQL'e kaydeder ve **kendisi** Kafka'nın `registration` topic'ine yayınlar. Telemetry Sensors (Kutay) bu topic'i dinleyip yeni evi/cihazı simülasyonuna dinamik olarak ekler (bkz. görev #12, Core'un bu endpoint'i bitirmesini bekliyor).

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

### 1.3 Apache Ignite — anlık değer cache'i (v4)
**Yazan:** Kutay (Telemetry Sensors) · **Okuyan:** Tarık (Core, isteğe bağlı hızlı okuma için)

Telemetry Sensors, her cihazın en son ürettiği değeri Kafka'ya göndermenin yanında Ignite'a da yazıyor. Amaç: Core'un "şu an bu cihaz kaç watt çekiyor" gibi anlık sorularda Kafka geçmişini taramasına ya da DB'ye gitmesine gerek kalmadan hızlıca cevap alabilmesi.

- **Cache adı:** `latest_telemetry`
- **Key formatı:** `"{homeId}:{applianceId}"` (örnek: `"11111111-1111-1111-1111-111111111111:21111111-1111-1111-1111-111111111111"`)
- **Value:** `telemetry` topic'iyle aynı JSON şeması (bölüm 1.1), string olarak saklanıyor.
- **Bağlantı:** Ignite thin client, `IGNITE_ADDRESS` env değişkeni (varsayılan `localhost:10800`).
- Ignite'a yazma başarısız olursa (servis kapalıysa vb.) Telemetry Sensors durmaz, sadece loglar — Kafka akışı bu duruma bağımlı değil.

### 1.4 Anomali kuralı — spec'te sabit (v5 — netleştirme)

Bir cihaz güvenli limitini (appliance'ın `safeLimitWatt` değeri) **üst üste 3 telemetry döngüsünde** aşarsa anomali olarak işaretlenmeli ve alarm tetiklenmeli; normale dönünce sayaç sıfırlanmalı. Bu sayı (**3**) ekibin kendi kararı değil, orijinal ödev PDF'inde (bölüm 5.2.1, "Consecutive Breach Counter") açıkça verilmiş bir gereksinim. Bu sayaç mantığı **Core (Tarık)** tarafında Ignite üzerinde tutulacak — Sensors sadece ham wattage verisini üretir, anomali tespiti/saymayı yapmaz.

Ayrıca aynı bölümde kota uyarı eşiği de belirtiliyor: bir ev bütçesinin **%80'ine veya %100'üne** ulaştığında uyarı/alarm hattı tetiklenmeli (bu, Esra'nın dashboard'daki görsel `status` eşiklerinden — %90/%100 — ayrı, Core'un iç alarm/bildirim mantığıdır).

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

### 2.5 `POST /api/homes/register` — Yeni ev/cihaz kaydı (v5 — eklendi)
**Sağlayan:** Tarık (Core) · **Çağıran:** Esra (Web App, kayıt formu)

Ödev PDF'inde tanımlı ("Home Registration Endpoint"). Core bu isteği alınca PostgreSQL'e kaydeder ve `registration` Kafka topic'ine yayınlar (bkz. bölüm 1.2).

```json
{
  "contactEmail": "kullanici@example.com",
  "name": "Kadıköy Evi",
  "budgetQuotaKwh": 300.0,
  "appliances": [
    { "name": "Klima", "safeLimitWatt": 2000.0 }
  ]
}
```
Response: `201 Created` + oluşturulan evin `id` (uuid) dahil tam kaydı.

### 2.6 `GET /api/ai/recommendation` (genel) veya `GET /api/homes/{id}/recommendation` (eve özel)
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

## 3. Java Servis Arayüzü — `EnergyAdvisoryService` (v2)
**Sağlayan:** Kutay (AI Advisory Service) · **Çağıran:** Tarık (Core, `/api/ai/recommendation` içinde sarmalanır)

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

Core, bu `AdvisoryResult`'ı alıp `homeId`, `homeName` ve `generatedAt` (ISO 8601, timezone'lu) ile sarmalayarak REST response'una çevirir. `ApplianceAnomaly.consecutiveBreaches`, bölüm 1.4'teki 3-kere-üst-üste kuralına göre Core'un Ignite'ta tuttuğu sayaçtan gelir.

### 3.1 AI Advisory Service — ayrı mikroservis, internal HTTP sözleşmesi (v3)

Kutay'ın `ai-advisory` servisi **kendi portunda (8081)** ayrı bir Spring Boot uygulaması olarak çalışır, Core buna HTTP üzerinden bağlanır.

**Endpoint:** `POST http://localhost:8081/internal/advisory`
**Request body:** `EnergyAdvisoryContext` · **Response body:** `AdvisoryResult`
**Hata:** `503` + bölüm 6'daki ortak hata formatı.

`AI_ADVISORY_BASE_URL` env değişkeni ile Core'a bu servisin adresi verilecek (local'de `http://localhost:8081`).

## 4. Kademeli Ceza Tarifesi (Ekip Kararı — spec dışı ek kural)

Orijinal ödev tek seviyeli ("premium penalty rate") bir ceza tanımlıyor. Ekip kararıyla kademeli hale getirdik; bu hesaplama **Core (Tarık)** tarafında yapılır ve doğrudan `bill` alanına yansır. `status` (normal/warning/danger) alanı sadece görsel gösterge olarak kalır, ceza kademesi ayrı bir iç hesaptır:

| Kota Aşımı | Kademe | Ceza Çarpanı |
|---|---|---|
| %100–119 | 1 | 1.5x |
| %120–139 | 2 | 2x |
| %140–159 | 3 | 2.5x |
| Her ek %20 | +1 kademe | +0.5x |

## 5. Sayısal Değer Formatı

Backend değerleri birimleriyle birlikte string döndürmez. `269.5` doğru, `"269.5 kWh"` yanlış.

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

Backend, `http://localhost:5173` origin'ine izin vermeli. Merkezi (global) `CorsConfigurationSource` bean'i kullanılacak.

## 8. Canlı Veri / Polling (v1)

Frontend `/api/homes`'u **5 saniyede bir** çağırır.

## 9. Tarih-Saat Formatı

ISO 8601, timezone'lu: `2026-07-21T16:30:00+03:00`.

## 10. Geliştirme Sırası (Öncelik)

1. Veri modelleri ve JSON alan isimleri (bu belge)
2. `GET /api/homes` (ilk etapta mock JSON dönebilir)
3. `GET /api/homes/{id}`
4. CORS ayarı
5. `POST /api/homes/register` (bkz. bölüm 2.5 — Sensors'ın registration dinleyicisi buna bağlı)
6. `GET /api/analytics`
7. Hata response yapısı
8. `GET /api/ai/recommendation` (AI Advisory Service hazır — bkz. bölüm 3.1)
9. Kafka/telemetry gerçek veri entegrasyonu (Telemetry Sensors hazır — bkz. bölüm 1.1, Ignite cache hazır — bkz. bölüm 1.3)

## 11. Ortam Değişkenleri (herkes için ortak)

| Değişken | Açıklama |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` (local) |
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | docker-compose'daki `powerpulse_db` bilgileri (port: **5433**) |
| `IGNITE_ADDRESS` | `localhost:10800` (Ignite thin client adresi) |
| `GEMINI_API_KEY` | AI Advisory Service için, export edilecek |
| `GEMINI_MODEL` | varsayılan: `gemini-3-flash-preview` |
| `AI_ADVISORY_BASE_URL` | Core için, varsayılan: `http://localhost:8081` |
| `VITE_API_BASE_URL` | Frontend `.env`, `http://localhost:8080/api` |
