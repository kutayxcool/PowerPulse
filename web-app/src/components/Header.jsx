function Header({ activePage, onPageChange,lastUpdated }) {
  return (
    <header className="header">
      <div>
        <span className="subtitle">Akıllı Enerji Yönetimi</span>
        <h1>⚡ PowerPulse</h1>
        <p>Evlerin enerji kullanımını canlı olarak takip edin.</p>
      </div>

      <div className="header-actions">
        <nav className="navigation">
          <button
            type="button"
            className={activePage === "dashboard" ? "active" : ""}
            onClick={() => onPageChange("dashboard")}
          >
            Dashboard
          </button>

          <button
            type="button"
            className={activePage === "analytics" ? "active" : ""}
            onClick={() => onPageChange("analytics")}
          >
            Analizler
          </button>
        </nav>

        <div className="live-status">
            <div className="live-indicator">
                <span className="live-dot"></span>
                Live
            </div>

            <span className="last-update">
                {lastUpdated.toLocaleTimeString("tr-TR")}
            </span>
        </div>
      </div>
    </header>
  );
}

export default Header;