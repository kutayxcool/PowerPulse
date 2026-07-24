import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import InfoHint from "./InfoHint";

const COLORS = [
  "#1769aa",
  "#20b26b",
  "#f5a623",
  "#8b5cf6",
  "#e5484d",
  "#06b6d4",
];

function ApplianceConsumptionChart({ homes, onOpenHint }) {
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
      <InfoHint
        id="chart-appliance"
        text="Bu pasta grafik, toplam enerji tüketiminin cihaz türlerine (klima, buzdolabı, fırın gibi) göre nasıl dağıldığını gösterir. En büyük dilime bakarak en çok tüketen cihaz türünü buradan bulabilirsin."
        onOpen={onOpenHint}
      />

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
              formatter={(value, name) => [
                `${Number(value).toFixed(1)} kWh`,
                name,
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