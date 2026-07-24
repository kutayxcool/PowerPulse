# PowerPulse

Gerçek zamanlı IoT enerji tüketim izleme ve bütçe/ceza denetim platformu.

## Ekip

| Kişi | Sorumluluk |
|---|---|
| Esra | Web App (frontend) |
| Tarık | Core (backend: kota, kademeli ceza, fatura) |
| Kutay | Telemetry Sensors (simülasyon), AI Advisory Service, altyapı (Kafka/Ignite/Postgres) |

## Klasör Yapısı (önerilen)

```
powerpulse/
├── docker-compose.yml
├── README.md
├── sensors/          # Kutay - telemetry simülatörü
├── ai-advisory/       # Kutay - EnergyAdvisoryService (Gemini entegrasyonu)
├── core/              # Tarık - Core backend
└── web-app/           # Esra - React frontend
```

## Kurulum

1. Bu repoyu klonla.
2. Altyapıyı ayağa kaldır:
   ```
   docker-compose up -d
   ```
   Bu, Kafka (+ Zookeeper + Kafka UI: http://localhost:8090), PostgreSQL (5432) ve Apache Ignite (10800) container'larını başlatır.
3. Gemini API key'ini environment variable olarak ver:
   ```
   export GEMINI_API_KEY=<senin-keyin>
   ```
4. Her ekip üyesi kendi klasöründe (`sensors/`, `ai-advisory/`, `core/`, `web-app/`) bağımsız geliştirme yapar; ortak sözleşme `CONTRACTS.md` dosyasındadır.

## Kademeli Ceza Tarifesi (Ekip Kararı)

Orijinal ödevde tek seviyeli ("premium penalty rate") bir ceza tanımlı. Ekip kararıyla bunu kademeli hale getirdik:

- %100–119 kota aşımı → 1. kademe (1.5x)
- %120–139 → 2. kademe (2x)
- %140–159 → 3. kademe (2.5x)
- Her ek %20 aşımda bir kademe daha eklenir.

> Not: Bu kural ödev PDF'inde yok, ekibin kendi eklediği bir geliştirme.

## sensors modülünü çalıştırırken (not)

`sensors` modülü Apache Ignite 2.x thin client kullanıyor. Bu kütüphane, JDK 11+'ta varsayılan olarak kapalı olan bazı düşük seviye JVM erişimlerine ihtiyaç duyuyor. Çalıştırırken `InaccessibleObjectException` alırsan, IntelliJ'de Run Configuration'ın **VM options** alanına şunu ekle:

```
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.management/sun.management=ALL-UNNAMED --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED
```

Bu, Apache Ignite'ın kendi JDK 11+ uyumluluk dokümantasyonunda önerdiği standart bayraklardır.
