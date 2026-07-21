import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

function DayNightConsumptionChart({ homes }) {
  const chartData = homes.map((home) => ({
    name: home.name.replace(" Evi", ""),
    day: home.dayConsumption || 0,
    night: home.nightConsumption || 0,
  }));

  return (
    <section className="analytics-card">
      <div className="analytics-card-header">
        <h3>Gündüz ve Gece Tüketimi</h3>
        <p>Evlerin tarife zamanlarına göre tüketim karşılaştırması</p>
      </div>

      <div className="analytics-chart-container">
        <ResponsiveContainer width="100%" height={320}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />

            <XAxis dataKey="name" />

            <YAxis unit=" kWh" />

            <Tooltip
              formatter={(value, name) => [
                `${Number(value).toFixed(1)} kWh`,
                name === "day" ? "Gündüz" : "Gece",
              ]}
            />

            <Legend
              formatter={(value) =>
                value === "day" ? "Gündüz" : "Gece"
              }
            />

            <Bar
              dataKey="day"
              fill="#f5a623"
              radius={[7, 7, 0, 0]}
            />

            <Bar
              dataKey="night"
              fill="#1769aa"
              radius={[7, 7, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default DayNightConsumptionChart;