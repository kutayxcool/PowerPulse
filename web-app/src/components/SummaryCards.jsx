function SummaryCards({ homes }) {
  const totalHomes = homes.length;

  const totalConsumption = homes.reduce(
    (sum, home) => sum + home.consumption,
    0
  );

  const totalBill = homes.reduce(
    (sum, home) => sum + home.bill,
    0
  );

  const riskyHomes = homes.filter(
    (home) =>
      home.status === "warning" ||
      home.status === "danger"
  ).length;

  const warningHomes = homes.filter(
    (home) => home.status === "warning"
  ).length;

  const dangerHomes = homes.filter(
    (home) => home.status === "danger"
  ).length;

  return (
    <section className="summary-grid">

      <div className="summary-card">
        <div className="summary-icon">🏠</div>

        <div>
          <span>Toplam Ev</span>
          <strong>{totalHomes}</strong>
        </div>
      </div>

      <div className="summary-card">
        <div className="summary-icon">⚡</div>

        <div>
          <span>Toplam Tüketim</span>
          <strong>{totalConsumption} kWh</strong>
        </div>
      </div>

      <div className="summary-card">
        <div className="summary-icon">💰</div>

        <div>
          <span>Toplam Fatura</span>
          <strong>{totalBill.toFixed(2)} TL</strong>
        </div>
      </div>

      <div className="summary-card">
        <div className="summary-icon">⚠️</div>

        <div>
          <span>Riskli Ev</span>
          <strong>{riskyHomes}</strong>

          <small className="risk-details">
            {warningHomes} uyarı · {dangerHomes} kritik
          </small>
        </div>
      </div>

    </section>
  );
}

export default SummaryCards;