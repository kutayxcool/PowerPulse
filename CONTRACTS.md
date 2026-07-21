# PowerPulse — Entegrasyon Sözleşmeleri (Contract-First)

Bu belge, ekip üyelerinin birbirini beklemeden bağımsız geliştirme yapabilmesi için sabitlenmiş arayüzleri tanımlar. Buradaki şemalar/interface imzaları değişmeden önce ekipçe konuşulmalı.

## 1. Kafka Konuları (Topics)

### 1.1 `telemetry` — Sensörden gelen anlık tüketim verisi
**Üreten:** Kutay (Telemetry Sensors) · **Tüketen:** Tarık (Core)

```json
{
  "homeId": "uuid",
  "applianceId": "uuid",
  "applianceName": "Buzdolabı",
  "wattage": 1450.0,
  "timestamp": "2026-07-21T10:15:30Z"
}
```

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

## 2. Java Servis Arayüzü — `EnergyAdvisoryService`
**Sağlayan:** Kutay (AI Advisory Service) · **Çağıran:** Tarık (Core)

```java
public interface EnergyAdvisoryService {
    String generateAdvisory(EnergyAdvisoryContext context);
}

public record EnergyAdvisoryContext(
    String homeId,
    double totalConsumptionKwh,
    double budgetQuotaKwh,
    double currentBillAmount,
    boolean quotaBreached,
    List<ApplianceAnomaly> anomalies
) {}

public record ApplianceAnomaly(String applianceName, int consecutiveBreaches) {}
```

## 3. Kademeli Ceza Tarifesi (Ekip Kararı — spec dışı ek kural)

Orijinal ödev tek seviyeli ("premium penalty rate") bir ceza tanımlıyor. Ekip kararıyla kademeli hale getirdik; bu hesaplama **Core (Tarık)** tarafında yapılacak, sonucu `EnergyAdvisoryContext.quotaBreached` ve fatura tutarına yansıyacak:

| Kota Aşımı | Kademe | Ceza Çarpanı |
|---|---|---|
| %100–119 | 1 | 1.5x |
| %120–139 | 2 | 2x |
| %140–159 | 3 | 2.5x |
| Her ek %20 | +1 kademe | +0.5x |

## 4. REST API — Web App ↔ Core
**Sağlayan:** Tarık (Core) · **Çağıran:** Esra (Web App)

> Not: Kesin endpoint listesi Core geliştirmesi ilerledikçe burada güncellenecek (Swagger ile de belgelenecek — bkz. adım 10). Şimdilik minimum beklenen:
- `POST /api/homes/register` — yeni ev/kullanıcı kaydı (registration topic'ine yayınlanır)
- `GET /api/homes/{homeId}/consumption` — güncel tüketim + kota durumu
- `GET /api/homes/{homeId}/advisory` — AI tavsiyesi (Core, AI Advisory Service'i çağırıp döner)
- `GET /api/homes/{homeId}/bill` — güncel fatura + ceza kademesi

## 5. Ortam Değişkenleri (herkes için ortak)

| Değişken | Açıklama |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` (local) |
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | docker-compose'daki `powerpulse_db` bilgileri |
| `IGNITE_HOST` | `localhost:10800` |
| `GEMINI_API_KEY` | AI Advisory Service için, export edilecek |
