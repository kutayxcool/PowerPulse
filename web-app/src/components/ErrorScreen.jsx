function ErrorScreen({ message, onRetry }) {
  return (
    <div className="error-screen" role="alert">
      <div className="error-icon" aria-hidden="true">
        ⚠️
      </div>

      <h2>Veriler alınamadı</h2>

      <p>
        {message ||
          "PowerPulse verilerine şu anda ulaşılamıyor. Lütfen tekrar deneyin."}
      </p>

      <button type="button" className="retry-button" onClick={onRetry}>
        Tekrar Dene
      </button>
    </div>
  );
}

export default ErrorScreen;