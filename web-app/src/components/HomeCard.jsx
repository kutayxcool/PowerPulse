function HomeCard({ home, onSelectHome, getStatusText }) {
  return (
    <button
      type="button"
      className="card"
      onClick={() => onSelectHome(home)}
    >
      <div className="card-header">
        <div className="home-icon">🏠</div>

        <div>
          <h2>{home.name}</h2>
          <p>Ev No: {home.id.toString().padStart(3, "0")}</p>
        </div>
      </div>

      <div className="metrics">
        <div>
          <span>⚡ Tüketim</span>
          <strong>{home.consumption.toFixed(1)} kWh</strong>
        </div>

        <div>
          <span>💰 Fatura</span>
          <strong>{home.bill.toFixed(2)} TL</strong>
        </div>
      </div>

      <div className="quota-info">
        <span>Kota kullanımı</span>
        <strong>%{home.quotaPercentage}</strong>
      </div>

      <div className="progress">
        <div
          className={`progress-bar ${home.status}`}
          style={{
            width: `${Math.min(home.quotaPercentage, 100)}%`,
          }}
        />
      </div>

      <p className={`status ${home.status}`}>
        {getStatusText(home.status)}
      </p>
    </button>
  );
}

export default HomeCard;