function LoadingScreen() {
  return (
    <div className="loading-screen" role="status" aria-live="polite">
      <div className="loading-spinner" />

      <h2>PowerPulse verileri yükleniyor</h2>

      <p>Enerji tüketim bilgileri hazırlanıyor...</p>
    </div>
  );
}

export default LoadingScreen;