import { exportHomesAsCsv } from "../utils/exportCsv";
import HomieMascot from "./HomieMascot";
import InfoHint from "./InfoHint";

function Header({
  activePage,
  onPageChange,
  lastUpdated,
  onAddHome,
  theme,
  onThemeChange,
  homes,
  onOpenHint,
  currentUser,
  onLogout,
}) {
  const handleExportClick = () => {
    if (homes && homes.length > 0) {
      exportHomesAsCsv(homes);
    }
  };

  const handleToggleTheme = () => {
    onThemeChange(theme === "copper" ? "beige" : "copper");
  };

  return (
    <header className="header">
      <div className="header-top-row">
        <div className="brand">
          <span className="brand-logo" aria-hidden="true">
            <HomieMascot size={46} />
          </span>

          <h1>
            <span className="brand-letter-box">
              <span>P</span>
            </span>
            ower
            <span className="brand-letter-box">
              <span>P</span>
            </span>
            ulse
          </h1>
        </div>

        <div className="header-actions">
          <nav className="navigation">
            <InfoHint
              id="navigation"
              text="Ana Sayfa: canlı tüketim panosu. Analizler: evler arası karşılaştırma ve grafikler. + Yeni Ev Ekle: sisteme yeni bir ev ve cihazlarını kaydet."
              onOpen={onOpenHint}
            />

            <button
              type="button"
              className={activePage === "dashboard" ? "active" : ""}
              onClick={() => onPageChange("dashboard")}
            >
              Ana Sayfa
            </button>

            <button
              type="button"
              className={activePage === "analytics" ? "active" : ""}
              onClick={() => onPageChange("analytics")}
            >
              Analizler
            </button>

            <button
              type="button"
              className="header-add-home-button"
              onClick={onAddHome}
            >
              + Yeni Ev Ekle
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

          <button
            type="button"
            className="header-icon-button"
            onClick={handleExportClick}
            aria-label="Raporu CSV olarak indir"
            title="Raporu CSV olarak indir"
          >
            <span aria-hidden="true">⬇️</span>
          </button>

          <button
            type="button"
            className="theme-toggle-button"
            onClick={handleToggleTheme}
            aria-label="Temayı değiştir"
            title="Temayı değiştir"
          >
            <span aria-hidden="true">
              {theme === "copper" ? "☀️" : "🌙"}
            </span>
          </button>

          {currentUser && (
            <div className="header-account">
              <span
                className="header-account-name"
                title={currentUser.email}
              >
                {currentUser.name}
              </span>

              <button
                type="button"
                className="header-icon-button"
                onClick={onLogout}
                aria-label="Çıkış yap"
                title="Çıkış yap"
              >
                <span aria-hidden="true">🚪</span>
              </button>
            </div>
          )}
        </div>
      </div>

      <p className="header-tagline">
        <span className="header-tagline-badge">
          Akıllı Enerji Yönetimi
        </span>
        Evlerinizin enerji tüketimini canlı takip edin, bütçenizi
        kontrol altında tutun.
      </p>
    </header>
  );
}

export default Header;
