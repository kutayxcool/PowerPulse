import { useState } from "react";

function AIRecommendation({ homes }) {
  const [isFlipped, setIsFlipped] = useState(false);
  const [showMessage, setShowMessage] = useState(false);

  if (!homes || homes.length === 0) {
    return null;
  }

  const highestConsumptionHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(secondHome.consumption) - Number(firstHome.consumption)
  )[0];

  const highestQuotaHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(secondHome.quotaPercentage) -
      Number(firstHome.quotaPercentage)
  )[0];

  const highestConsumption = Number(
    highestConsumptionHome.consumption || 0
  );

  const highestQuota = Number(
    highestQuotaHome.quotaPercentage || 0
  );

  const estimatedSaving = Number(
    (highestConsumption * 0.08 * 2.1).toFixed(2)
  );

  const handleGeminiClick = (event) => {
    event.stopPropagation();
    setShowMessage(true);
  };

  const handleCloseMessage = (event) => {
    event.stopPropagation();
    setShowMessage(false);
  };

  return (
    <section className="ai-recommendation-wrapper">
      <button
        type="button"
        className={`ai-flip-card ${
          isFlipped ? "is-flipped" : ""
        }`}
        onClick={() =>
          setIsFlipped((previousValue) => !previousValue)
        }
        aria-label="AI öneri kartını çevir"
      >
        <div className="ai-flip-card-inner">
          <div className="ai-flip-card-face ai-flip-card-front">
            <div className="ai-card-top">
              <div className="ai-card-icon">
                <span aria-hidden="true">🤖</span>
              </div>

              <span className="ai-card-badge">
                <span aria-hidden="true">✨</span>
                AI İçgörüsü
              </span>
            </div>

            <div className="ai-card-content">
              <h3>{highestConsumptionHome.name}</h3>

              <p>
                Şu anda en yüksek enerji tüketimine sahip ev.
              </p>

              <strong>
                {highestConsumption.toFixed(1)} kWh
              </strong>
            </div>

            <div className="ai-card-footer">
              <span>Detaylı öneri için tıkla</span>
              <span
                className="rotate-icon"
                aria-hidden="true"
              >
                ↻
              </span>
            </div>
          </div>

          <div className="ai-flip-card-face ai-flip-card-back">
            <div className="ai-card-top">
              <div className="ai-card-icon">
                <span aria-hidden="true">✨</span>
              </div>

              <span className="ai-card-badge">
                Akıllı Öneri
              </span>
            </div>

            <div className="ai-card-content">
              <h3>Enerji Tasarrufu Önerisi</h3>

              <ul>
                <li>
                  {highestQuotaHome.name} kota kullanımında %
                  {highestQuota.toFixed(0)} seviyesinde.
                </li>

                <li>
                  Yüksek tüketimli cihazları daha uygun
                  saatlerde çalıştırabilirsiniz.
                </li>

                <li>
                  Gece tarifesi aktifse çamaşır makinesi,
                  bulaşık makinesi ve elektrikli araç şarjını
                  gece saatlerine taşıyabilirsiniz.
                </li>
              </ul>

              <strong>
                Tahmini tasarruf: ₺
                {estimatedSaving.toFixed(2)}
              </strong>
            </div>

            <div className="ai-card-footer">
              <span>Ön yüze dön</span>
              <span
                className="rotate-icon"
                aria-hidden="true"
              >
                ↻
              </span>
            </div>
          </div>
        </div>
      </button>

      <button
        type="button"
        className="gemini-button"
        onClick={handleGeminiClick}
      >
        <span aria-hidden="true">💬</span>
        Gemini Asistanına Sor
      </button>

      {showMessage && (
        <div className="gemini-message">
          <span>
            ✨ Gemini entegrasyonu yakında aktif olacak.
          </span>

          <button
            type="button"
            onClick={handleCloseMessage}
            aria-label="Mesajı kapat"
          >
            ×
          </button>
        </div>
      )}
    </section>
  );
}

export default AIRecommendation;