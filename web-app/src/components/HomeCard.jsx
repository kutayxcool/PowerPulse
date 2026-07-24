import {
  calculateRecommendedDailyLimit,
  calculateRecommendedDailyLimitKwh,
  getDaysInCurrentMonth,
  formatTl,
} from "../utils/budgetMath";

function HomeCard({ home, onSelectHome, onEditHome, getStatusText }) {
  const daysInMonth = getDaysInCurrentMonth();
  const dailyLimit = calculateRecommendedDailyLimit(home, daysInMonth);
  const dailyLimitKwh = calculateRecommendedDailyLimitKwh(
    home,
    daysInMonth
  );
  const handleCardKeyDown = (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelectHome(home);
    }
  };

  const handleEditClick = (event) => {
    event.stopPropagation();
    onEditHome(home);
  };

  return (
    <div
      className="card"
      role="button"
      tabIndex={0}
      onClick={() => onSelectHome(home)}
      onKeyDown={handleCardKeyDown}
    >
      <button
        type="button"
        className="card-edit-button"
        onClick={handleEditClick}
        aria-label={`${home.name} evini düzenle`}
        title="Evi düzenle"
      >
        ✏️
      </button>

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
          {home.budgetQuotaKwh != null && (
            <small className="metric-sublabel">
              Kotayı aşmamak için uymanız gereken günlük sınır:{" "}
              {dailyLimitKwh.toFixed(1)} kWh
            </small>
          )}
        </div>

        <div>
          <span>💰 Fatura</span>
          <strong>{home.bill.toFixed(2)} TL</strong>
          {home.budgetQuotaKwh != null && (
            <small className="metric-sublabel">
              Kotayı aşmamak için uymanız gereken günlük sınır:{" "}
              {formatTl(dailyLimit)}
            </small>
          )}
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
    </div>
  );
}

export default HomeCard;
