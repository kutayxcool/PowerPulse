import ConsumptionChart from "./ConsumptionChart";

function HomeModal({ home, onClose }) {
  if (!home) {
    return null;
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        onClick={(event) => event.stopPropagation()}
      >
        <button
          className="modal-close"
          onClick={onClose}
          aria-label="Detay penceresini kapat"
        >
          ×
        </button>

        <div className="modal-header">
          <div className="home-icon">🏠</div>

          <div>
            <h2>{home.name}</h2>
            <p>Ev No: {home.id.toString().padStart(3, "0")}</p>
          </div>
        </div>

        <div className="modal-summary">
          <div>
            <span>Toplam tüketim</span>
            <strong>{home.consumption.toFixed(1)} kWh</strong>
          </div>

          <div>
            <span>Güncel fatura</span>
            <strong>{home.bill.toFixed(2)} TL</strong>
          </div>

          <div>
            <span>Kota kullanımı</span>
            <strong>%{home.quotaPercentage}</strong>
          </div>
        </div>

        <h3>Cihazlar</h3>

        <div className="appliance-list">
        {home.appliances.map((appliance) => (
            <div
            className={`appliance-item ${appliance.status}`}
            key={appliance.id}
            >
            <div>
                <strong>{appliance.name}</strong>

                <span>
                {appliance.status === "danger"
                    ? "Anormal tüketim"
                    : appliance.status === "warning"
                    ? "Yüksek tüketim"
                    : "Normal"}
                </span>
            </div>

            <strong>{appliance.watt} W</strong>
            </div>
        ))}
        </div>

        <ConsumptionChart data={home.dailyConsumption} />
      </div>
    </div>
  );
}

export default HomeModal;
