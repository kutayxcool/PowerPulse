# PowerPulse — Entegrasyon Sözleşmeleri (Contract-First)

Bu belge, ekip üyelerinin birbirini beklemeden bağımsız geliştirme yapabilmesi için sabitlenmiş arayüzleri tanımlar. v2: Esra'nın REST sözleşmesi. v3: AI Advisory Service HTTP sözleşmesi. v4: Ignite cache sözleşmesi. v5: registration akış yönü düzeltmesi. v6: hoca ses kaydı dinlenip polling/pagination/email/mimari düzeltmeleri yapıldı. Buradaki şemalar/interface imzaları değişmeden önce ekipçe konuşulmalı.

## 0. Kimlik (ID) Kararı — Ekip Kararı

Ev ve cihaz `id` alanları **UUID (string)** olacak, sayısal (Long/Integer) değil. Kafka telemetry/registration akışında zaten UUID kullanıyoruz, ayrı bir sayısal-ID eşleme katmanı eklemek gereksiz karmaşıklık yaratır. Tarık'ın Core'da DB primary key'i UUID olarak tanımlaması gerekiyor.

## 0.1 Mimari — TEK Spring Boot uygulaması (v6 — önemli düzeltme)

Ödev, hocanın sözlü açıklamasında **açıkça mikroservis mimarisini yasaklıyor**: *"gidip hani mikroservis mimari yapmayın... Modül modül değil, bir Java Spring Boot uygulaması."* Core, paketlerle bölünmüş (`com.example.postgres`, `com.example.kafka`, `com.example.ai` gibi) **tek bir** Spring Boot uygulaması olmalı.

Bu yüzden Kutay'ın `ai-advisory` servisi **kalıcı bir mimari parça değil** — şu an bağımsız geliştirme/test kolaylığı için ayrı port (8081) üzerinde çalıştırılıyor. **Teslimden önce** bu servisteki sınıflar (`EnergyAdvisoryService`, `EnergyAdvisoryContext`, `AdvisoryResult`, `GeminiEnergyAdvisoryService`) olduğu gibi Tarık'ın Core reposuna bir Java paketi olarak taşınacak, ayrı `AdvisoryController`/port/HTTP çağrısı kaldırılacak; Core, AI çağrısını doğrudan Java metodu olarak (in-process) yapacak. Bölüm 3.1'deki HTTP sözleşmesi sadece **geçiş dönemi** içindir.

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

Demo veriler için sabit UUID'ler kullanılıyor (Kadıköy Evi, Beşiktaş Evi — bkz. `sensors/` kodu).

### 1.2 `registration` — Ev/cihaz kayıt ve bütçe bilgisi
**Üreten:** Tarık (Core) · **Tüketen:** Kutay (Telemetry Sensors)

Akış: Esra'nın Web App'i Core'a **REST** isteği atar (bkz. 2.5) → Core PostgreSQL'e kaydeder → Core Kafka'nın `registration` topic'ine yayınlar → Telemetry Sensors dinleyip yeni evi/cihazı simülasyonuna dinamik ekler (bkz. görev #12, Core'un register endpoint'ini bekliyor).

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

### 1.3 Apache Ignite — anlık değer cache'i
**Yazan:** Kutay (Telemetry Sensors) · **Okuyan:** Tarık (Core)

- **Cache adı:** `latest_telemetry` · **Key:** `"{homeId}:{applianceId}"` · **Value:** telemetry JSON'ı (string).
- **Bağlantı:** `IGNITE_ADDRESS` (varsayılan `localhost:10800`).
- Ignite sadece **anlık/günlük** veri için — Ignite düşerse veya sıfırlansa bile finansal/geçmiş veri kaybolmamalı, o yüzden Core'un düzenli aralıklarla (hoca "15 dakika gibi kısa olmayan bir aralık" öneriyor) Ignite'taki durumu Postgres'e snapshot olarak yazması gerekiyor. Ignite kaynak-doğruluk (source of truth) değildir, Postgres'tir.
- Ignite'a yazma başarısız olursa Telemetry Sensors durmaz, sadece loglar.

### 1.4 Anomali kuralı — spec'te sabit
Bir cihaz güvenli limitini **üst üste 3 telemetry döngüsünde** aşarsa anomali sayılır, alarm tetiklenir, normale dönünce sayaç sıfırlanır. **3 sayısı sabit** (spec), ama "döngü"nün ne kadar sürdüğü (ör. her telemetry tick'i mi, yoksa 1 dakikalık pencere mi) **ekibin kararına bırakılmış** — henüz karar vermedik, Tarık ile konuşulmalı. Sayaç mantığı Core'da Ignite üzerinde tutulur.

Ayrıca kota uyarı eşiği: bir ev bütçesinin **%80'ine veya %100'üne** ulaştığında uyarı tetiklenmeli (BTK'nın SMS kuralına benzer — bu, Esra'nın dashboard `status` eşiklerinden — %90/%100 — ayrı, Core'un iç alarm/bildirim mantığı).

### 1.5 Bildirim/e-posta gönderimi (v6 — yeni, daha önce hiç konuşulmamıştı)
Core, %80/%100 kota aşımı, anomali tespiti veya ceza tarifesi aktivasyonu olduğunda sadece ekranda göstermekle kalmayıp, **evin kayıtlı `contactEmail` adresine gerçek bir e-posta göndermeli** (AI'ın ürettiği tavsiye metniyle). Bu öneriler ve bildirimler **ayrı iki Postgres tablosunda** (`recommendation`, `notification` gibi) loglanmalı. Bu tamamen Core'un (Tarık'ın) sorumluluğu ama ekipçe hiç konuşulmadığı için not düşüldü — e-posta gönderimi için (ör. Spring Mail/SMTP) bir karar gerekiyor.

## 2. REST API — Web App ↔ Core
**Sağlayan:** Tarık (Core) · **Çağıran:** Esra (Web App)

**Base URL:** `http://localhost:8080`, frontend `.env`: `VITE_API_BASE_URL=http://localhost:8080/api`

### 2.1 `GET /api/homes` — Dashboard ev listesi (Ignite'tan okunur — anlık veri)
```json
[
  { "id": "uuid", "name": "Kadıköy Evi", "consumption": 269.5, "bill": 565.95, "quotaPercentage": 99, "status": "warning" }
]
```
`status`: `<90` → `normal`, `90–100` → `warning`, `>100` → `danger`.

### 2.2 `GET /api/homes/{id}` — Ev detayı (appliances + son 7 gün tüketim, **sayfalanabilir**)
```json
{
  "id": "uuid", "name": "Kadıköy Evi", "consumption": 269.5, "bill": 565.95,
  "quotaPercentage": 99, "status": "warning",
  "appliances": [ { "id": "uuid", "name": "Klima", "watt": 1200, "status": "warning" } ],
  "dailyConsumption": [ { "day": "Pzt", "consumption": 30 } ]
}
```
> Not (v6): `dailyConsumption` gibi geçmişe dönük veriler Postgres'ten geliyor (Ignite sadece bugünü/anlık veriyi tutuyor). Hoca'nın uyarısı: geçmiş kayıtlar binlerce/milyonlarca satır olabilir, bu yüzden **pagination zorunlu** — `?page=0&size=20` gibi query parametreleri Core tarafından desteklenmeli, tüm geçmişi tek seferde dönmemeli.

### 2.3 `GET /api/analytics` (**pagination gerekli**, bkz. yukarıdaki not)
```json
{
  "dailyTotalConsumption": [ { "day": "Pzt", "consumption": 120.5 } ],
  "homeComparison": [ { "homeId": "uuid", "homeName": "Kadıköy Evi", "consumption": 269.5, "bill": 565.95, "quotaPercentage": 99 } ]
}
```

### 2.4 `GET /api/dashboard/summary` (opsiyonel, backend hesaplar)
```json
{ "totalHomes": 4, "totalConsumption": 1050.4, "totalBill": 2205.84, "quotaExceededHomes": 2 }
```

### 2.5 `POST /api/homes/register` — Yeni ev/cihaz kaydı (Swagger'dan erişilebilir olmalı)
```json
{
  "contactEmail": "kullanici@example.com",
  "name": "Kadıköy Evi",
  "budgetQuotaKwh": 300.0,
  "appliances": [ { "name": "Klima", "safeLimitWatt": 2000.0 } ]
}
```
Response: `201 Created` + oluşturulan evin `id` (uuid) dahil tam kaydı. Core bunu Postgres'e yazar ve `registration` topic'ine yayınlar (bkz. 1.2).

### 2.6 `GET /api/ai/recommendation` (genel) veya `GET /api/homes/{id}/recommendation` (eve özel)
```json
{
  "title": "Enerji Tasarrufu Önerisi", "homeId": "uuid", "homeName": "Kadıköy Evi",
  "recommendations": ["Klima kullanım süresini azaltabilirsiniz.", "Çamaşır makinesini düşük tarife saatlerinde çalıştırabilirsiniz."],
  "estimatedSavingPercentage": 14, "estimatedSavingAmount": 79.23,
  "generatedAt": "2026-07-21T16:30:00+03:00"
}
```
AI hata verirse yalnızca bu endpoint 503 döner, dashboard'un geri kalanı etkilenmez.

## 3. AI Advisory — Java arayüzü (v6: geçiş dönemi notuyla)
**Sağlayan:** Kutay (şu an `ai-advisory/` ayrı servis, teslimden önce Core'a modül olarak taşınacak — bkz. bölüm 0.1)

```java
public interface EnergyAdvisoryService {
    AdvisoryResult generateAdvisory(EnergyAdvisoryContext context);
}

public record EnergyAdvisoryContext(
    String homeId, String homeName, double totalConsumptionKwh, double budgetQuotaKwh,
    double currentBillAmount, boolean quotaBreached, List<ApplianceAnomaly> anomalies
) {}

public record ApplianceAnomaly(String applianceName, int consecutiveBreaches) {}

public record AdvisoryResult(
    String title, List<String> recommendations,
    double estimatedSavingPercentage, double estimatedSavingAmount
) {}
```

### 3.1 Geçiş dönemi HTTP sözleşmesi (final mimaride kaldırılacak)
**Endpoint:** `POST http://localhost:8081/internal/advisory` · **Request:** `EnergyAdvisoryContext` · **Response:** `AdvisoryResult` · **Hata:** 503.
`AI_ADVISORY_BASE_URL` env değişkeni (local: `http://localhost:8081`) — bu geçici; final teslimde Core, bu sınıfları kendi içine alıp doğrudan Java çağrısı yapacak.

## 4. Kademeli Ceza Tarifesi (Ekip kararı, hocanın da önerdiği yaklaşım)

Orijinal ödev tek seviyeli ("premium penalty rate") ceza tanımlıyor; hoca soru üzerine "ben kademeli yaparım" diyerek bunu doğruladı, biz de kademeli yaptık:

| Kota Aşımı | Kademe | Ceza Çarpanı |
|---|---|---|
| %100–119 | 1 | 1.5x |
| %120–139 | 2 | 2x |
| %140–159 | 3 | 2.5x |
| Her ek %20 | +1 kademe | +0.5x |

`status` (normal/warning/danger) görsel gösterge olarak kalır, ceza kademesi ayrı bir iç hesaptır (Core, `bill` alanına yansıtır).

## 5. Sayısal Değer Formatı
Backend değerleri birimsiz döndürür: `269.5` doğru, `"269.5 kWh"` yanlış.

## 6. Hata Response Formatı
```json
{ "status": 404, "error": "Not Found", "message": "İstenen ev bulunamadı.", "path": "/api/homes/15", "timestamp": "2026-07-21T16:30:00+03:00" }
```
200 GET · 201 kayıt · 400 geçersiz istek · 404 bulunamadı · 500 sunucu hatası · 503 AI/telemetry kullanılamıyor.

## 7. CORS
`http://localhost:5173` origin'ine izin, merkezi `CorsConfigurationSource` bean'i (tek tek `@CrossOrigin` değil).

## 8. Canlı Veri / Polling (v6 — DÜZELTİLDİ)
~~5 saniye~~ → **En fazla 1-2 saniye.** Hoca net belirtti: "1 veya 2 saniye, en fazla 2 saniye olacak şekilde UI'ı tekrar komple baştan çizmeden polling ederek belirli dataları update etmeniz gerekecek." Frontend `/api/homes`'u bu sıklıkla çağıracak, backend bunu sorunsuz karşılayabilmeli (bu yüzden Ignite'tan okumak kritik, Postgres'ten değil).

## 9. Tarih-Saat Formatı
ISO 8601, timezone'lu: `2026-07-21T16:30:00+03:00`.

## 10. Geliştirme Sırası (Öncelik)
1. Veri modelleri ve JSON alan isimleri (bu belge)
2. `GET /api/homes` (mock JSON ile başlanabilir)
3. `GET /api/homes/{id}` (+ pagination)
4. CORS ayarı
5. `POST /api/homes/register` (Swagger'dan erişilebilir)
6. `GET /api/analytics` (+ pagination)
7. Hata response yapısı
8. `GET /api/ai/recommendation` (şimdilik ai-advisory'ye HTTP ile bağlanır, sonra Core'a modül olarak taşınır)
9. Kafka/telemetry gerçek veri entegrasyonu
10. E-posta bildirim gönderimi + recommendation/notification tabloları
11. Ignite → Postgres periyodik snapshot (cron/scheduler)

## 11. Ortam Değişkenleri

| Değişken | Açıklama |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `POSTGRES_URL` / `USER` / `PASSWORD` | `powerpulse_db`, port **5433** |
| `IGNITE_ADDRESS` | `localhost:10800` |
| `GEMINI_API_KEY` / `GEMINI_MODEL` | AI için (varsayılan model: `gemini-3-flash-preview`) |
| `AI_ADVISORY_BASE_URL` | Geçiş dönemi, varsayılan `http://localhost:8081` |
| `VITE_API_BASE_URL` | Frontend `.env`, `http://localhost:8080/api` |
