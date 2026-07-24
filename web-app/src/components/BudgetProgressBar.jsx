import {
  calculateMonthlyBudgetTl,
  calculateOverageTl,
  countTrackedDays,
  getDaysInCurrentMonth,
  formatTl,
} from "../utils/budgetMath";
import InfoHint from "./InfoHint";

function getBudgetStatus(percentage) {
  if (percentage > 100) {
    return "danger";
  }

  if (percentage >= 90) {
    return "warning";
  }

  return "normal";
}

function BudgetProgressBar({ homes, onOpenHint }) {
  if (!homes || homes.length === 0) {
    return null;
  }

  const averagePercentage =
    homes.reduce(
      (sum, home) => sum + Number(home.quotaPercentage || 0),
      0
    ) / homes.length;

  const status = getBudgetStatus(averagePercentage);
  const barWidth = Math.min(100, averagePercentage);

  const statusNote = {
    normal: "Evlerinizin genel bütçe kullanımı sağlıklı seviyede.",
    warning: "Bazı evler aylık kotasının sınırına yaklaşıyor.",
    danger: "Evlerinizden biri veya birkaçı kotasını aştı.",
  }[status];

  const totalBillTl = homes.reduce(
    (sum, home) => sum + Number(home.bill || 0),
    0
  );

  // Artik tahmin degil - her evin gercek kota (kWh) ve taban birim
  // fiyati (TL/kWh) alanlarindan hesaplaniyor, bu yuzden tuketim
  // arttikca oynamaz, ay boyunca SABIT kalir.
  const totalBudgetTl = homes.reduce(
    (sum, home) => sum + calculateMonthlyBudgetTl(home),
    0
  );

  // Kota asimi yuzunden simdiye kadar odenen fazladan tutar (TL).
  // Hicbir ev kotasini asmadiysa bu tam olarak 0 cikar.
  const totalOverageTl = homes.reduce(
    (sum, home) => sum + calculateOverageTl(home),
    0
  );

  const daysInMonth = getDaysInCurrentMonth();

  // "Su ana kadarki gunluk ortalama" ve "kalan gun" hesaplari, ayin
  // kacinci gunu oldugumuza (ornegin 23 Temmuz) DEGIL, projenin/evlerin
  // GERCEKTEN kac gundur veri topladigina gore yapilir - yoksa proje
  // yeni baslamis olsa bile sanki cok gun gecmis gibi davranip
  // hesaplamalari carpitirdi.
  const trackedDays = Math.max(1, countTrackedDays(homes));
  const dailyAverageSoFar = totalBillTl / trackedDays;
  const daysRemaining = Math.max(1, daysInMonth - trackedDays);

  const remainingBudgetTl = totalBudgetTl - totalBillTl;

  const recommendedDailyLimit =
    totalBudgetTl > 0 && remainingBudgetTl > 0
      ? remainingBudgetTl / daysRemaining
      : 0;

  return (
    <section className="budget-progress-card">
      <InfoHint
        id="budget-progress"
        text="Evlerinizin aylık kotalarına göre ortalama bütçe kullanım yüzdesi. Alttaki detaylar: sabit aylık bütçeniz, şu ana kadarki günlük ortalamanız, kotayı aşmamak için önerilen günlük sınır ve kota aşımı yüzünden şu ana kadar ödediğiniz ekstra tutar."
        onOpen={onOpenHint}
      />

      <div className="budget-progress-header">
        <h3>Genel Bütçe Kullanımı</h3>
        <strong>%{averagePercentage.toFixed(0)}</strong>
      </div>

      <div className="budget-progress-track">
        <div
          className={`budget-progress-bar ${status}`}
          style={{ width: `${barWidth}%` }}
        />
      </div>

      <p className="budget-progress-note">{statusNote}</p>

      {totalBudgetTl > 0 && (
        <div className="budget-progress-stats">
          <div>
            <span>Aylık Bütçe (Kotaya Göre)</span>
            <strong>{formatTl(totalBudgetTl)}</strong>
          </div>

          <div>
            <span>Şu Ana Kadarki Günlük Ortalamanız</span>
            <strong>{formatTl(dailyAverageSoFar)}</strong>
          </div>

          <div>
            <span>Kotayı Aşmamak İçin Önerilen Günlük Sınır</span>
            <strong
              className={
                remainingBudgetTl <= 0 ? "budget-stat-danger" : ""
              }
            >
              {remainingBudgetTl > 0
                ? formatTl(recommendedDailyLimit)
                : "Bütçe aşıldı"}
            </strong>
          </div>

          <div>
            <span>Kota Aşım Cezası</span>
            <strong
              className={
                totalOverageTl > 0 ? "budget-stat-danger" : ""
              }
            >
              {formatTl(totalOverageTl)}
            </strong>
          </div>
        </div>
      )}
    </section>
  );
}

export default BudgetProgressBar;
