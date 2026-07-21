import HomeComparisonChart from "../components/HomeComparisonChart";
import ApplianceConsumptionChart from "../components/ApplianceConsumptionChart";
import DayNightConsumptionChart from "../components/DayNightConsumptionChart";
import DailyConsumptionChart from "../components/DailyConsumptionChart";

function Analytics({ homes }) {
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

      <DailyConsumptionChart homes={homes} />

      <div className="analytics-grid">
        <HomeComparisonChart homes={homes} />

        <ApplianceConsumptionChart homes={homes} />
      </div>

      <DayNightConsumptionChart homes={homes} />
    </section>
  );
}

export default Analytics;