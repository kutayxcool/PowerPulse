-- Kullanici hesaplari (kayit/giris) tablosu. Her ev artik bir
-- kullaniciya ait olur - "herkes kayit olabilecek ve kendi evlerini
-- yonetebilecek" bir coklu kullanici sistemine gecis.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- homes tablosuna sahiplik kolonu once NULL edilebilir eklenir; varsa
-- (auth eklenmeden once olusturulmus) mevcut evler otomatik olusturulan
-- bir "eski veriler" hesabina baglanir, boylece NOT NULL kisitlamasi
-- eklenirken mevcut satirlar icin hata olusmaz. Bu hesabin sifresi
-- gecersiz bir bcrypt disi degerdir - kimse bu hesapla giris yapamaz.
ALTER TABLE homes ADD COLUMN owner_id UUID;

INSERT INTO users (id, email, password_hash, display_name)
SELECT '00000000-0000-0000-0000-000000000001', 'legacy@powerpulse.local', '!disabled!', 'Eski Kayitlar'
WHERE EXISTS (SELECT 1 FROM homes WHERE owner_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM users WHERE id = '00000000-0000-0000-0000-000000000001');

UPDATE homes SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;

ALTER TABLE homes ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE homes ADD CONSTRAINT fk_home_owner FOREIGN KEY (owner_id) REFERENCES users (id);
CREATE INDEX idx_homes_owner_id ON homes (owner_id);
