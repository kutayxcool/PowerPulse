# PowerPulse - Canlıya Alma (Deployment) Rehberi

Bu rehber, PowerPulse'u gerçek internete açmak için izlenecek adımları anlatır.
Mimari şu şekilde bölünüyor:

- **Frontend (web-app/)** → **Vercel** (statik/Vite hosting, ücretsiz plan yeterli)
- **Backend (core + sensors + ai-advisory + Postgres + Kafka + Ignite)** → **tek bir VPS**,
  hepsi Docker Compose ile aynı sunucuda çalışır

Hesap açma, ödeme, DNS ayarları gibi adımlar senin tarafından yapılmalı (bunlar
başkasının yerine yapılamayan işlemler) - ben her adımda hangi komutu/değeri
gireceğini söylüyorum.

---

## 1. VPS satın al

Önerilen: **Hetzner Cloud CX22** (2 vCPU, 4 GB RAM, ~4-5€/ay) ya da dengi bir
**DigitalOcean Droplet** (en az 4 GB RAM önerilir - Kafka + Ignite + 3 Java
servisi aynı anda çalışacak, 1-2 GB'lık en ucuz planlar yetersiz kalabilir).

Sunucu işletim sistemi: **Ubuntu 22.04 LTS**.

Sunucu oluşturulunca sana bir IP adresi verilir (ör. `123.45.67.89`) - bunu not al.

## 2. Bir domain al (yoksa)

Namecheap, GoDaddy, Cloudflare Registrar gibi bir yerden bir alan adı al (ör.
`powerpulse-app.com`). Sonra domain sağlayıcının DNS ayarlarından bir **A kaydı**
ekle:

```
Tip: A
Host: api  (yani api.powerpulse-app.com)
Değer: <VPS'in IP adresi>
```

DNS değişikliklerinin yayılması birkaç dakika-birkaç saat sürebilir.

## 3. VPS'e bağlan ve Docker kur

```bash
ssh root@<VPS_IP>

curl -fsSL https://get.docker.com | sh
```

## 4. Projeyi sunucuya getir

```bash
git clone <senin-repo-adresin> powerpulse
cd powerpulse
```

(Repo private ise GitHub'da bir deploy key/personal access token ile clone
etmen gerekebilir - GitHub bunu nasıl yapacağını anlatan bir ekran gösterir.)

## 5. `.env` dosyasını oluştur

```bash
cp .env.example .env
nano .env
```

`.env.example` içindeki her `*** DEĞİŞTİR ***` yazan satırı doldur. Rastgele,
güvenli değerler üretmek için:

```bash
openssl rand -base64 48   # JWT_SECRET icin
openssl rand -base64 32   # INTERNAL_API_KEY icin
```

`DOMAIN` alanına 2. adımda ayarladığın adresi yaz (ör. `api.powerpulse-app.com`).
`FRONTEND_URL` alanını şimdilik bir yer tutucu bırakabilirsin (ör.
`https://placeholder.vercel.app`) - Vercel deploy'undan gerçek adresi aldıktan
sonra (adım 7) buraya geri döneceğiz.

## 6. Her şeyi ayağa kaldır

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

İlk çalıştırma birkaç dakika sürer (Maven build + imaj indirme). Durumu izlemek
için:

```bash
docker compose -f docker-compose.prod.yml logs -f core
```

`Started CoreApplication` gibi bir satır görünce Core ayakta demektir. Test et:

```bash
curl https://api.powerpulse-app.com/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"yanlis"}'
```

`401` ve bir JSON hata mesajı dönüyorsa backend çalışıyor demektir (kimlik bilgisi
yanlış olduğu için 401 bekleniyor, önemli olan bağlantının kurulması).

## 7. Frontend'i Vercel'e deploy et

1. [vercel.com](https://vercel.com) üzerinden GitHub hesabınla giriş yap.
2. "New Project" → PowerPulse reposunu seç.
3. **Root Directory** olarak `web-app` seç (Vercel'e bu klasörün frontend
   olduğunu söylüyoruz).
4. Framework otomatik "Vite" olarak algılanmalı (algılamazsa `web-app/vercel.json`
   zaten build komutunu/çıktı klasörünü belirtiyor).
5. **Environment Variables** kısmına ekle:
   - `VITE_API_BASE_URL` = `https://api.powerpulse-app.com/api`
6. Deploy'a bas. Bitince Vercel sana gerçek adresi verir (ör.
   `https://powerpulse.vercel.app`).

## 8. Backend'e gerçek frontend adresini bildir

Adım 5'te `FRONTEND_URL` için geçici bir değer koymuştuk - şimdi gerçek Vercel
adresiyle güncelle:

```bash
nano .env   # FRONTEND_URL=https://powerpulse.vercel.app yap
docker compose -f docker-compose.prod.yml up -d core
```

(Sadece `core`'u yeniden başlatmak yeterli, diğer servisler etkilenmez.)

## 9. Uçtan uca test

Vercel adresine (`https://powerpulse.vercel.app`) tarayıcıdan git, kayıt ol,
giriş yap, bir ev ekle, cihaz ekle - hepsi gerçek internetten çalışıyor
olmalı.

---

## Güvenlik kontrol listesi (prod'a çıkmadan önce)

Aşağıdakilerin hepsi `.env` dosyasında **gerçek/rastgele** değerlerle
değiştirilmiş olmalı - kod içindeki varsayılanlar (`powerpulse-local-dev-...`
gibi) sadece yerel geliştirme içindir, prod'da kullanılırsa güvenlik açığı
olur:

- `JWT_SECRET` - rastgele, en az 32 karakter
- `INTERNAL_API_KEY` - rastgele, en az 32 karakter
- `POSTGRES_PASSWORD` - güçlü bir şifre
- `FRONTEND_URL` - sadece gerçek Vercel adresin, başka hiçbir origin değil
- `.env` dosyasının git'e commit EDİLMEDİĞİNDEN emin ol (`.gitignore`'da zaten
  var, ama VPS'te de dosya izinlerini kontrol et: `chmod 600 .env`)
- Postgres/Kafka/Ignite'ın host'a hiçbir port açmadığından emin ol
  (`docker-compose.prod.yml`'de zaten böyle ayarlandı - `docker ps` ile
  kontrol edebilirsin, bu üç servisin `PORTS` sütununda hiçbir şey
  görünmemeli)
- SSH'a şifre yerine key-based giriş kullan, root girişini kapat (VPS
  sağlayıcının kendi rehberi genelde bunu anlatır)

## Sonradan güncelleme yapmak istersen

```bash
cd powerpulse
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Frontend tarafında ise Vercel, GitHub'a her push yaptığında otomatik olarak
yeniden deploy eder - elle bir şey yapmana gerek yok.
