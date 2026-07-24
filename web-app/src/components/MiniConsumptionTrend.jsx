import { useEffect, useRef, useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import InfoHint from "./InfoHint";

// Dashboard'daki tum evlerin CANLI saatlik tuketimini gosterir.
// Backend'de saat bazli gecmis veri tutulmuyor (sadece gunluk toplam
// kaydediliyor - bkz. DailyConsumptionSnapshot), bu yuzden bu grafik
// panel acikken gelen canli telemetri farklarini (iki anlik olcum
// arasindaki fark) o anki saate ekleyerek olusturur. Yani "su ana kadar
// canli izledigimiz gercek tuketim" - sahte/rastgele deger uretilmez.
// Panel her acildiginda gunun saatleri sifirdan dolmaya baslar.
function MiniConsumptionTrend({ homes, onOpenHint }) {
  const [hourlyBuckets, setHourlyBuckets] = useState({});
  const previousTotalRef = useRef(null);
  const currentDayRef = useRef(new Date().toDateString());

  useEffect(() => {
    if (!homes || homes.length === 0) {
      return;
    }

    const today = new Date().toDateString();

    if (today !== currentDayRef.current) {
      currentDayRef.current = today;
      previousTotalRef.current = null;
      setHourlyBuckets({});
    }

    const totalConsumption = homes.reduce(
      (sum, home) => sum + Number(home.consumption || 0),
      0
    );

    if (previousTotalRef.current !== null) {
      const delta = totalConsumption - previousTotalRef.current;

      if (delta > 0) {
        const currentHour = new Date().getHours();

        setHourlyBuckets((prev) => ({
          ...prev,
          [currentHour]: Number(((prev[currentHour] || 0) + delta).toFixed(2)),
        }));
      }
    }

    previousTotalRef.current = totalConsumption;
  }, [homes]);

  if (!homes || homes.length === 0) {
    return null;
  }

  const currentHour = new Date().getHours();

  const chartData = Array.from({ length: currentHour + 1 }, (_, hour) => ({
    hour,
    label: `${String(hour).padStart(2, "0")}:00`,
    consumption: hourlyBuckets[hour] || 0,
  }));

  const tickInterval = chartData.length > 8 ? Math.floor(chartData.length / 6) : 0;

  return (
    <section className="analytics-card mini-trend-card">
      <InfoHint
        id="mini-trend"
        text="Panel açık kaldıkça, tüm evlerin saat başına gerçek zamanlı tüketimini gösterir. Geçmiş saatlerin verisi tutulmadığı için grafik panel her açıldığında sıfırdan dolmaya başlar."
        onOpen={onOpenHint}
      />

      <div className="analytics-card-header">
        <h3>Canlı Saatlik Tüketim</h3>
        <p>Panel açıkken tüm evlerin saat başına gerçek zamanlı tüketimi</p>
      </div>

      <div className="mini-chart-container">
        <ResponsiveContainer width="100%" height={140}>
          <BarChart
            data={chartData}
            margin={{ top: 5, right: 10, left: 0, bottom: 0 }}
          >
            <XAxis
              dataKey="label"
              tick={{ fontSize: 11, fill: "#667085" }}
              axisLine={false}
              tickLine={false}
              interval={tickInterval}
            />

            <YAxis hide domain={[0, "dataMax + 0.5"]} />

            <Tooltip
              formatter={(value) => [
                `${Number(value).toFixed(2)} kWh`,
                "Saatlik Tüketim",
              ]}
            />

            <Bar dataKey="consumption" fill="#1769aa" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}

export default MiniConsumptionTrend;
