// Bir evin dailyConsumption dizisindeki benzersiz GERCEK gun sayisini
// dondurur - projenin/evin kac gundur canli veri topladigini gosterir.
// Ayin kacinci gunu oldugumuzu (takvim tarihi) DEGIL, bunu kullanmak
// gerekiyor; aksi halde proje yeni baslamis olsa bile sanki cok gun
// gecmis gibi davranip hesaplamalari yanlis cikarir.
export function countTrackedDays(dailyConsumption) {
  const uniqueDays = new Set();

  dailyConsumption?.forEach((entry) => {
    if (entry?.day) {
      uniqueDays.add(entry.day);
    }
  });

  return uniqueDays.size;
}

export function getDaysInCurrentMonth() {
  const today = new Date();

  return new Date(
    today.getFullYear(),
    today.getMonth() + 1,
    0
  ).getDate();
}

// Bir evin SABIT aylik butcesini (TL) hesaplar: kota (kWh) * taban
// birim fiyat (TL/kWh). Backend'den gelen gercek alanlara dayanir
// (budgetQuotaKwh, baseRatePerKwh) - tuketime/faturaya gore tahmin
// yurutulmez, bu yuzden ay boyunca SABIT kalir.
export function calculateMonthlyBudgetTl(home) {
  const budgetQuotaKwh = Number(home.budgetQuotaKwh || 0);
  const baseRatePerKwh = Number(home.baseRatePerKwh || 0);

  return budgetQuotaKwh * baseRatePerKwh;
}

// Kota asimi nedeniyle simdiye kadar ODENEN fazladan tutar (TL).
// Gercek fatura ile, ayni tuketimin tamamen taban (tier-0) fiyattan
// hesaplansaydi ne tutacagi arasindaki farktir. Kota asilmadiysa 0'dir.
export function calculateOverageTl(home) {
  const consumption = Number(home.consumption || 0);
  const bill = Number(home.bill || 0);
  const baseRatePerKwh = Number(home.baseRatePerKwh || 0);

  const linearCostAtBaseRate = consumption * baseRatePerKwh;
  const overage = bill - linearCostAtBaseRate;

  return overage > 0 ? overage : 0;
}

// Kotayi asmamak icin kalan gunlere yayilmasi gereken onerilen gunluk
// harcama siniri (TL). "Kalan gun" hesabi da GERCEK takip edilen gun
// sayisina gore yapilir (bkz. countTrackedDays), ayin kacinci gunu
// oldugumuza gore degil.
export function calculateRecommendedDailyLimit(home, daysInMonth) {
  const monthlyBudgetTl = calculateMonthlyBudgetTl(home);
  const bill = Number(home.bill || 0);
  const remainingBudgetTl = monthlyBudgetTl - bill;

  if (monthlyBudgetTl <= 0 || remainingBudgetTl <= 0) {
    return 0;
  }

  const trackedDays = Math.max(
    1,
    countTrackedDays(home.dailyConsumption)
  );
  const daysRemaining = Math.max(1, daysInMonth - trackedDays);

  return remainingBudgetTl / daysRemaining;
}

// Ayni gunluk sinir mantigini kWh cinsinden verir (TL yerine) - kotaya
// gore kalan kWh'yi kalan gunlere bolerek, kotayi asmamak icin
// bugunden itibaren gunluk ortalama ne kadar tuketilebilecegini
// hesaplar.
export function calculateRecommendedDailyLimitKwh(home, daysInMonth) {
  const budgetQuotaKwh = Number(home.budgetQuotaKwh || 0);
  const consumption = Number(home.consumption || 0);
  const remainingKwh = budgetQuotaKwh - consumption;

  if (budgetQuotaKwh <= 0 || remainingKwh <= 0) {
    return 0;
  }

  const trackedDays = Math.max(
    1,
    countTrackedDays(home.dailyConsumption)
  );
  const daysRemaining = Math.max(1, daysInMonth - trackedDays);

  return remainingKwh / daysRemaining;
}

export function formatTl(value) {
  return `₺${Number(value || 0).toLocaleString("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
