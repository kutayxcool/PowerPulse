import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

function HomeComparisonChart({ homes }) {
  const chartData = homes.map((home) => ({
    name: home.name.replace(" Evi", ""),
    consumption: home.consumption,
  }));

  return (
    <section className="comparison-chart">
      <div className="comparison-chart-header">
        <div>
          <h3>Ev Bazlı Tüketim Karşılaştırması</h3>
          <p>Toplam enerji tüketiminin evlere göre dağılımı</p>
        </div>
      </div>

      <div className="comparison-chart-container">
        <ResponsiveContainer width="100%" height={320}>
          <BarChart
            data={chartData}
            margin={{
              top: 10,
              right: 20,
              left: 0,
              bottom: 10,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis unit=" kWh" />
            <Tooltip
              formatter={(value) => [
                `${Number(value).toFixed(1)} kWh`,
                "Tüketim",
              ]}
            />
            <Bar
              dataKey="consumption"
              fill="#1769aa"
              radius={[8, 8, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default HomeComparisonChart;