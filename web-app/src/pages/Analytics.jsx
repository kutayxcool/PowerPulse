import HomeComparisonChart from "../components/HomeComparisonChart";
import ApplianceConsumptionChart from "../components/ApplianceConsumptionChart";
import DayNightConsumptionChart from "../components/DayNightConsumptionChart";
import DailyConsumptionChart from "../components/DailyConsumptionChart";
import AIRecommendation from "../components/AIRecommendation";

// Homie, Ana Sayfa'daki gibi burada da ekranin SOL tarafinda, kendi
// dar sutununda oturur - boylece 4 grafik sagda tek bir sutunda
// toplu/yan yana kalir, Homie'nin karti araya girip onlari asagi
// itmez. Ayni Homie, grafiklerin her birinin yanindaki "?" ile
// tiklandiginda o grafigi teker teker aciklar (bkz. her grafik
// bilesenindeki InfoHint).
function Analytics({ homes, homieRef, aiColumnRef, activeHint, onOpenHint }) {
  return (
    <section>
      <div className="page-heading">
        <div>
          <h2>Enerji Analizleri</h2>
          <p>
            Tüketim verilerini ev, cihaz ve zamana göre inceleyin.
          </p>
        </div>
      </div>

      <div className="analytics-top-grid">
        <aside className="analytics-top-side" ref={aiColumnRef}>
          <AIRecommendation
            homes={homes}
            homieRef={homieRef}
            onOpenHint={onOpenHint}
            isPointing={Boolean(activeHint)}
          />
        </aside>

        <div className="analytics-top-content">
          <DailyConsumptionChart
            homes={homes}
            onOpenHint={onOpenHint}
          />

          <div className="analytics-grid">
            <HomeComparisonChart
              homes={homes}
              onOpenHint={onOpenHint}
            />

            <ApplianceConsumptionChart
              homes={homes}
              onOpenHint={onOpenHint}
            />
          </div>

          <DayNightConsumptionChart
            homes={homes}
            onOpenHint={onOpenHint}
          />
        </div>
      </div>
    </section>
  );
}

export default Analytics;