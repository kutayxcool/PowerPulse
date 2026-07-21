import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

const COLORS = [
  "#1769aa",
  "#20b26b",
  "#f5a623",
  "#8b5cf6",
  "#e5484d",
  "#06b6d4",
];

function ApplianceConsumptionChart({ homes }) {
  const applianceTotals = homes
    .flatMap((home) => home.appliances)
    .reduce((totals, appliance) => {
      const currentTotal = totals[appliance.name] || 0;

      return {
        ...totals,
        [appliance.name]: currentTotal + (appliance.consumption || 0),
      };
    }, {});

  const chartData = Object.entries(applianceTotals).map(
    ([name, consumption]) => ({
      name,
      consumption: Number(consumption.toFixed(1)),
    })
  );

  return (
    <section className="analytics-card">
      <div className="analytics-card-header">
        <h3>Cihaz Bazlı Tüketim</h3>
        <p>Enerji tüketiminin cihaz türlerine göre dağılımı</p>
      </div>

      <div className="analytics-chart-container">
        <ResponsiveContainer width="100%" height={320}>
          <PieChart>
            <Pie
              data={chartData}
              dataKey="consumption"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={65}
              outerRadius={105}
              paddingAngle={3}
            >
              {chartData.map((item, index) => (
                <Cell
                  key={item.name}
                  fill={COLORS[index % COLORS.length]}
                />
              ))}
            </Pie>

            <Tooltip
              formatter={(value) => [
                `${Number(value).toFixed(1)} kWh`,
                "Tüketim",
              ]}
            />

            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default ApplianceConsumptionChart;