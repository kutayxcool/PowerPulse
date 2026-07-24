-- Cihazlarin manuel olarak durdurulup baslatilabilmesi ("Durdur"/
-- "Baslat" butonu + zamanlayici ozelligi) icin bir "aktif mi" bayragi
-- eklenir. Varsayilan deger TRUE'dur - yani mevcut/yeni tum cihazlar
-- eklendiginde/goc sirasinda calisiyor durumda kabul edilir.
ALTER TABLE appliances
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
