import { useEffect, useRef, useState } from "react";
import {
  applianceCatalog,
  getApplianceIcon,
  initialApplianceType,
  initialApplianceBrand,
  initialApplianceModel,
} from "../data/applianceCatalog";

// Zamanlayici icin sunulan hazir sureler (dakika cinsinden).
const TIMER_DURATION_OPTIONS = [
  { label: "15 dk", minutes: 15 },
  { label: "30 dk", minutes: 30 },
  { label: "1 saat", minutes: 60 },
  { label: "2 saat", minutes: 120 },
];

// Bilesen govdesinin (render/handler) disinda, modul seviyesinde
// tanimlanir - Date.now() gibi "saf olmayan" bir cagriyi dogrudan
// render/handler icinde yapmamak icin (eslint react-hooks/purity).
function currentTimestamp() {
  return Date.now();
}

function EditHomeModal({
  home,
  onClose,
  onAddAppliance,
  onRemoveAppliance,
  onToggleAppliance,
  onDeleteHome,
}) {
  const [isAddFormOpen, setIsAddFormOpen] = useState(false);
  const [applianceType, setApplianceType] = useState(initialApplianceType);
  const [brand, setBrand] = useState(initialApplianceBrand);
  const [model, setModel] = useState(initialApplianceModel);
  const [applianceName, setApplianceName] = useState(initialApplianceType);

  const [pendingApplianceId, setPendingApplianceId] = useState(null);
  const [togglingApplianceId, setTogglingApplianceId] = useState(null);
  const [isAdding, setIsAdding] = useState(false);
  const [isDeletingHome, setIsDeletingHome] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  // Cihaz basina kurulmus zamanlayicilar - applianceId -> { endsAt,
  // label }. Sure dolunca cihaz otomatik olarak durdurulur. Modal
  // kapanirsa/degisirse bekleyen setTimeout'lar temizlenir ki artik
  // ekranda olmayan bir modal icin durdurma cagrisi yapilmasin.
  const [applianceTimers, setApplianceTimers] = useState({});
  const timeoutRefs = useRef({});
  const [, forceTick] = useState(0);

  useEffect(() => {
    const pendingTimeouts = timeoutRefs.current;

    return () => {
      Object.values(pendingTimeouts).forEach((timeoutId) =>
        clearTimeout(timeoutId)
      );
    };
  }, []);

  // Geri sayim yazisinin canli kalmasi icin, en az bir zamanlayici
  // kurulu oldugu surece saniyede bir yeniden render tetiklenir.
  useEffect(() => {
    if (Object.keys(applianceTimers).length === 0) {
      return undefined;
    }

    const intervalId = setInterval(() => forceTick((tick) => tick + 1), 1000);

    return () => clearInterval(intervalId);
  }, [applianceTimers]);

  if (!home) {
    return null;
  }

  const availableBrands = Object.keys(applianceCatalog[applianceType]);
  const availableModels = applianceCatalog[applianceType][brand];

  const selectedAppliance =
    availableModels.find((item) => item.model === model) ||
    availableModels[0];

  const handleTypeChange = (event) => {
    const selectedType = event.target.value;
    const firstBrand = Object.keys(applianceCatalog[selectedType])[0];
    const firstModel = applianceCatalog[selectedType][firstBrand][0].model;

    setApplianceType(selectedType);
    setBrand(firstBrand);
    setModel(firstModel);
    setApplianceName(selectedType);
  };

  const handleBrandChange = (event) => {
    const selectedBrand = event.target.value;
    const firstModel =
      applianceCatalog[applianceType][selectedBrand][0].model;

    setBrand(selectedBrand);
    setModel(firstModel);
  };

  const resetAddForm = () => {
    setApplianceType(initialApplianceType);
    setBrand(initialApplianceBrand);
    setModel(initialApplianceModel);
    setApplianceName(initialApplianceType);
    setIsAddFormOpen(false);
  };

  const handleAddAppliance = async () => {
    setErrorMessage("");

    if (!applianceName.trim()) {
      setErrorMessage("Lütfen cihaza bir isim verin.");
      return;
    }

    setIsAdding(true);

    try {
      await onAddAppliance(home.id, {
        name: applianceName.trim(),
        safeLimitWatt: selectedAppliance.safeLimitWatt,
      });

      resetAddForm();
    } catch (error) {
      console.error("Cihaz eklenemedi:", error);
      setErrorMessage(
        "Cihaz eklenirken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
    } finally {
      setIsAdding(false);
    }
  };

  const handleRemoveAppliance = async (applianceId) => {
    setErrorMessage("");
    setPendingApplianceId(applianceId);

    try {
      await onRemoveAppliance(home.id, applianceId);
    } catch (error) {
      console.error("Cihaz silinemedi:", error);
      setErrorMessage(
        "Cihaz silinirken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
    } finally {
      setPendingApplianceId(null);
    }
  };

  // Bir cihaz icin bekleyen zamanlayiciyi (varsa) iptal eder - hem
  // sure dolup calistirildiginda hem de kullanici manuel olarak
  // Durdur/Baslat'a bastiginda cagrilir ki eski bir zamanlayici daha
  // sonra beklenmedik sekilde devreye girmesin.
  const clearApplianceTimer = (applianceId) => {
    if (timeoutRefs.current[applianceId]) {
      clearTimeout(timeoutRefs.current[applianceId]);
      delete timeoutRefs.current[applianceId];
    }

    setApplianceTimers((current) => {
      if (!current[applianceId]) {
        return current;
      }

      const next = { ...current };
      delete next[applianceId];
      return next;
    });
  };

  const handleToggleAppliance = async (appliance) => {
    setErrorMessage("");
    setTogglingApplianceId(appliance.id);
    clearApplianceTimer(appliance.id);

    const nextActive = appliance.active === false;

    try {
      await onToggleAppliance(home.id, appliance.id, nextActive);
    } catch (error) {
      console.error("Cihaz durumu güncellenemedi:", error);
      setErrorMessage(
        "Cihaz durumu değiştirilirken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
    } finally {
      setTogglingApplianceId(null);
    }
  };

  // Zamanlayici secilince cihaz (henuz calismiyorsa) baslatilir ve
  // secilen sure sonunda otomatik olarak durdurulur. "Zamanlayıcı yok"
  // secilirse bekleyen zamanlayici sadece iptal edilir, cihazin
  // calisma durumu degismez.
  const handleTimerChange = async (appliance, event) => {
    const minutes = Number(event.target.value);

    clearApplianceTimer(appliance.id);

    if (!minutes) {
      return;
    }

    setErrorMessage("");

    try {
      if (appliance.active === false) {
        await onToggleAppliance(home.id, appliance.id, true);
      }

      const durationLabel =
        TIMER_DURATION_OPTIONS.find((option) => option.minutes === minutes)
          ?.label || `${minutes} dk`;
      const endsAt = currentTimestamp() + minutes * 60 * 1000;

      const timeoutId = setTimeout(async () => {
        delete timeoutRefs.current[appliance.id];
        setApplianceTimers((current) => {
          const next = { ...current };
          delete next[appliance.id];
          return next;
        });

        try {
          await onToggleAppliance(home.id, appliance.id, false);
        } catch (error) {
          console.error(
            "Zamanlayıcı sonunda cihaz durdurulamadı:",
            error
          );
        }
      }, minutes * 60 * 1000);

      timeoutRefs.current[appliance.id] = timeoutId;

      setApplianceTimers((current) => ({
        ...current,
        [appliance.id]: { endsAt, label: durationLabel },
      }));
    } catch (error) {
      console.error("Zamanlayıcı kurulamadı:", error);
      setErrorMessage(
        "Zamanlayıcı kurulurken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
    }
  };

  const formatRemaining = (endsAt) => {
    const remainingMs = Math.max(0, endsAt - currentTimestamp());
    const totalSeconds = Math.ceil(remainingMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    if (minutes === 0) {
      return `${seconds} sn`;
    }

    return `${minutes} dk ${seconds.toString().padStart(2, "0")} sn`;
  };

  const handleDeleteHomeClick = async () => {
    const confirmed = window.confirm(
      `${home.name} silinsin mi? Bu işlem geri alınamaz, evin tüm cihazları ve geçmiş verileri de silinecek.`
    );

    if (!confirmed) {
      return;
    }

    setErrorMessage("");
    setIsDeletingHome(true);

    try {
      await onDeleteHome(home.id);
    } catch (error) {
      console.error("Ev silinemedi:", error);
      setErrorMessage(
        "Ev silinirken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
      setIsDeletingHome(false);
    }
  };

  return (
    <div className="add-home-overlay" onClick={onClose}>
      <div
        className="add-home-modal"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="add-home-header">
          <div>
            <h2>Evi Düzenle</h2>
            <p>{home.name} — cihazları güncelleyin.</p>
          </div>

          <button
            type="button"
            className="add-home-close"
            onClick={onClose}
            aria-label="Kapat"
          >
            ×
          </button>
        </div>

        <section className="add-home-section">
          <div className="add-home-list-header">
            <div>
              <h3>Kayıtlı Cihazlar</h3>
              <p>{home.appliances.length} cihaz kayıtlı</p>
            </div>
          </div>

          {home.appliances.length === 0 ? (
            <div className="add-home-empty">
              <strong>Bu evde hiç cihaz yok</strong>
              <span>Aşağıdan yeni bir cihaz ekleyebilirsiniz.</span>
            </div>
          ) : (
            <div className="add-home-device-list">
              {home.appliances.map((appliance) => {
                const isActive = appliance.active !== false;
                const isToggling = togglingApplianceId === appliance.id;
                const timer = applianceTimers[appliance.id];

                return (
                  <div
                    className="add-home-device-item add-home-device-item--edit"
                    key={appliance.id}
                  >
                    <div className="add-home-device-icon">
                      {getApplianceIcon(appliance.name)}
                    </div>

                    <div className="add-home-device-info">
                      <strong>{appliance.name}</strong>
                      <span>
                        Güvenli limit: {appliance.safeLimitWatt} W
                      </span>

                      <span
                        className={`add-home-device-status ${
                          isActive
                            ? "add-home-device-status--on"
                            : "add-home-device-status--off"
                        }`}
                      >
                        {isActive ? "● Çalışıyor" : "● Durduruldu"}
                        {timer
                          ? ` · ${formatRemaining(timer.endsAt)} sonra duracak`
                          : ""}
                      </span>
                    </div>

                    <div className="add-home-device-actions">
                      <button
                        type="button"
                        className={`add-home-toggle-button ${
                          isActive
                            ? "add-home-toggle-button--stop"
                            : "add-home-toggle-button--start"
                        }`}
                        onClick={() => handleToggleAppliance(appliance)}
                        disabled={isToggling}
                      >
                        {isToggling
                          ? "..."
                          : isActive
                          ? "⏸ Durdur"
                          : "▶ Başlat"}
                      </button>

                      <select
                        className="add-home-timer-select"
                        value={timer ? "" : "0"}
                        onChange={(event) =>
                          handleTimerChange(appliance, event)
                        }
                        aria-label={`${appliance.name} için zamanlayıcı`}
                        title="Belirli bir süre çalıştırıp otomatik durdur"
                      >
                        <option value="0">Zamanlayıcı yok</option>
                        {TIMER_DURATION_OPTIONS.map((option) => (
                          <option key={option.minutes} value={option.minutes}>
                            {option.label} çalıştır
                          </option>
                        ))}
                      </select>

                      <button
                        type="button"
                        className="add-home-remove-button"
                        onClick={() => handleRemoveAppliance(appliance.id)}
                        disabled={pendingApplianceId === appliance.id}
                      >
                        {pendingApplianceId === appliance.id
                          ? "Siliniyor..."
                          : "Sil"}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {isAddFormOpen ? (
          <section className="add-home-section add-home-appliance-section">
            <div className="add-home-section-title">
              <div>
                <h3>Yeni Cihaz Ekle</h3>
                <p>
                  Cihaz türünü, markasını ve modelini seçerek eve ekleyin.
                </p>
              </div>
            </div>

            <div className="add-home-device-grid">
              <label className="add-home-field">
                <span>Cihaz türü</span>

                <select value={applianceType} onChange={handleTypeChange}>
                  {Object.keys(applianceCatalog).map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>

              <label className="add-home-field">
                <span>Marka</span>

                <select value={brand} onChange={handleBrandChange}>
                  {availableBrands.map((availableBrand) => (
                    <option key={availableBrand} value={availableBrand}>
                      {availableBrand}
                    </option>
                  ))}
                </select>
              </label>

              <label className="add-home-field">
                <span>Model</span>

                <select
                  value={model}
                  onChange={(event) => setModel(event.target.value)}
                >
                  {availableModels.map((availableModel) => (
                    <option
                      key={availableModel.model}
                      value={availableModel.model}
                    >
                      {availableModel.model}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="add-home-field">
              <span>Cihaz adı</span>

              <input
                type="text"
                value={applianceName}
                onChange={(event) => setApplianceName(event.target.value)}
                placeholder={applianceType}
              />
            </label>

            <div className="add-home-power-card">
              <div>
                <span>Güç tüketimi</span>
                <strong>{selectedAppliance.wattage} W</strong>
              </div>

              <div>
                <span>Güvenli limit</span>
                <strong>{selectedAppliance.safeLimitWatt} W</strong>
              </div>
            </div>

            <div className="add-home-actions">
              <button
                type="button"
                className="add-home-cancel-button"
                onClick={resetAddForm}
                disabled={isAdding}
              >
                Vazgeç
              </button>

              <button
                type="button"
                className="add-home-device-button"
                onClick={handleAddAppliance}
                disabled={isAdding}
              >
                {isAdding ? "Ekleniyor..." : "+ Cihazı Ekle"}
              </button>
            </div>
          </section>
        ) : (
          <button
            type="button"
            className="add-home-open-form-button"
            onClick={() => setIsAddFormOpen(true)}
          >
            + Yeni Cihaz Ekle
          </button>
        )}

        {errorMessage && <p className="add-home-error">{errorMessage}</p>}

        <div className="modal-danger-zone">
          <button
            type="button"
            className="modal-delete-button"
            onClick={handleDeleteHomeClick}
            disabled={isDeletingHome}
          >
            {isDeletingHome ? "Siliniyor..." : "🗑 Evi Sil"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default EditHomeModal;
