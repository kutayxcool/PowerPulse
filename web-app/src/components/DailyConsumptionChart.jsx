import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import InfoHint from "./InfoHint";

function DailyConsumptionChart({ homes, onOpenHint }) {
  const dailyTotals = homes.reduce((totals, home) => {
    home.dailyConsumption?.forEach((dailyData) => {
      const existingDay = totals.find(
        (item) => item.day === dailyData.day
      );

      if (existingDay) {
        existingDay.consumption += dailyData.consumption;
      } else {
        totals.push({
          day: dailyData.day,
          consumption: dailyData.consumption,
        });
      }
    });

    return totals;
  }, []);

  const chartData = dailyTotals.map((item) => ({
    ...item,
    consumption: Number(item.consumption.toFixed(1)),
  }));

  return (
    <section className="analytics-card daily-chart">
      <InfoHint
        id="chart-daily"
        text="Bu grafik, tüm evlerinizin son 7 güne ait toplam günlük enerji tüketimini gösterir. Çizginin yükselip alçalmasından tüketiminizin gün gün nasıl değiştiğini, hangi günlerde daha çok harcadığınızı takip edebilirsin."
        onOpen={onOpenHint}
      />

      <div className="analytics-card-header">
        <h3>Günlük Toplam Tüketim</h3>
        <p>Tüm evlerin son 7 günlük enerji tüketim trendi</p>
      </div>

      <div className="analytics-chart-container">
        <ResponsiveContainer width="100%" height={320}>
          <LineChart
            data={chartData}
            margin={{
              top: 10,
              right: 25,
              left: 0,
              bottom: 5,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />

            <XAxis dataKey="day" />

            <YAxis unit=" kWh" />

            <Tooltip
              formatter={(value) => [
                `${Number(value).toFixed(1)} kWh`,
                "Tüketim",
              ]}
            />

            <Line
              type="monotone"
              dataKey="consumption"
              stroke="#1769aa"
              strokeWidth={3}
              dot={{
                r: 5,
                fill: "#1769aa",
              }}
              activeDot={{
                r: 7,
              }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default DailyConsumptionChart;