import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

function ConsumptionChart({ data }) {
  return (
    <section className="chart-section">
      <div className="chart-heading">
        <div>
          <h3>Son 7 Günlük Tüketim</h3>
          <p>Günlük enerji tüketimi değişimi</p>
        </div>
      </div>

      <div className="chart-container">
        <ResponsiveContainer width="100%" height={260}>
          <LineChart
            data={data}
            margin={{
              top: 10,
              right: 20,
              left: 0,
              bottom: 0,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="day" />
            <YAxis unit=" kWh" />
            <Tooltip formatter={(value) => [`${value} kWh`, "Tüketim"]} />
            <Line
              type="monotone"
              dataKey="consumption"
              stroke="#1769aa"
              strokeWidth={3}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default ConsumptionChart;