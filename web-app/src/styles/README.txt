POWERPULSE CSS KURULUMU

1) src klasörünün içinde "styles" adında bir klasör oluştur.
2) Bu paketteki CSS dosyalarını src/styles içine kopyala.
3) App.jsx dosyasında şu satırı kaldır:
   import "./App.css";

4) Yerine şunu ekle:
   import "./styles/index.css";

5) Eski App.css dosyasını hemen silme.
   Önce projeyi çalıştır ve tüm sayfaları kontrol et.
   Her şey doğruysa App.css dosyasını silebilirsin.

Bu düzenlemede özellikle şu sorunlar düzeltildi:
- Modal başlığı artık koyu ve okunaklı.
- "Normal", "Yüksek tüketim" ve "Anormal tüketim" yazıları sola hizalı.
- Watt değerleri sağda sabit duruyor.
- Tekrarlanan header-actions, live-indicator ve live-dot stilleri birleştirildi.
- Responsive kurallar tek dosyada toplandı.
