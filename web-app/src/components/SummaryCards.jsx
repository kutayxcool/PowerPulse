import InfoHint from "./InfoHint";

function SummaryCards({ homes, onOpenHint }) {
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
        <InfoHint
          id="summary-total-homes"
          text="Sisteme kayıtlı toplam ev sayısı."
          onOpen={onOpenHint}
        />
        <div className="summary-icon">🏠</div>

        <div>
          <span>Toplam Ev</span>
          <strong>{totalHomes}</strong>
        </div>
      </div>

      <div className="summary-card">
        <InfoHint
          id="summary-total-consumption"
          text="Tüm evlerinizin bu ayki toplam elektrik tüketimi (kWh)."
          onOpen={onOpenHint}
        />
        <div className="summary-icon">⚡</div>

        <div>
          <span>Toplam Tüketim</span>
          <strong>{totalConsumption.toFixed(1)} kWh</strong>
        </div>
      </div>

      <div className="summary-card">
        <InfoHint
          id="summary-total-bill"
          text="Tüm evlerinizin bu ayki toplam tahmini faturası (TL)."
          onOpen={onOpenHint}
        />
        <div className="summary-icon">💰</div>

        <div>
          <span>Toplam Fatura</span>
          <strong>{totalBill.toFixed(2)} TL</strong>
        </div>
      </div>

      <div className="summary-card">
        <InfoHint
          id="summary-risky-homes"
          text="Aylık kotasının %90'ına yaklaşan (uyarı) veya %100'ünü geçen (kritik) ev sayısı."
          onOpen={onOpenHint}
        />
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