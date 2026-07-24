import { useState } from "react";
import {
  applianceCatalog,
  getApplianceIcon,
  initialApplianceType as initialType,
  initialApplianceBrand as initialBrand,
  initialApplianceModel as initialModel,
} from "../data/applianceCatalog";

function AddHomeModal({ isOpen, onClose, onAddHome }) {
  const [homeName, setHomeName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [budgetQuotaKwh, setBudgetQuotaKwh] = useState("");

  const [applianceType, setApplianceType] = useState(initialType);
  const [brand, setBrand] = useState(initialBrand);
  const [model, setModel] = useState(initialModel);
  const [applianceName, setApplianceName] = useState(initialType);

  const [appliances, setAppliances] = useState([]);
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) {
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

  const handleAddAppliance = () => {
    if (!applianceName.trim()) {
      setFormError("Lütfen cihaza bir isim verin.");
      return;
    }

    const newAppliance = {
      id: crypto.randomUUID(),
      name: applianceName.trim(),
      brand,
      model: selectedAppliance.model,
      wattage: selectedAppliance.wattage,
      safeLimitWatt: selectedAppliance.safeLimitWatt,
      status: "normal",
    };

    setAppliances((previousAppliances) => [
      ...previousAppliances,
      newAppliance,
    ]);

    setApplianceName(applianceType);
    setFormError("");
  };

  const handleRemoveAppliance = (applianceId) => {
    setAppliances((previousAppliances) =>
      previousAppliances.filter(
        (appliance) => appliance.id !== applianceId
      )
    );
  };

  const resetForm = () => {
    setHomeName("");
    setContactEmail("");
    setBudgetQuotaKwh("");

    setApplianceType(initialType);
    setBrand(initialBrand);
    setModel(initialModel);
    setApplianceName(initialType);

    setAppliances([]);
    setFormError("");
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const numericQuota = Number(budgetQuotaKwh);

    if (!homeName.trim()) {
      setFormError("Lütfen ev adını girin.");
      return;
    }

    if (!contactEmail.trim()) {
      setFormError("Lütfen iletişim e-postasını girin.");
      return;
    }

    if (!numericQuota || numericQuota <= 0) {
      setFormError("Lütfen geçerli bir aylık kota girin.");
      return;
    }

    if (appliances.length === 0) {
      setFormError("En az bir cihaz eklemelisiniz.");
      return;
    }

    // Core'un POST /api/homes/register sozlesmesi sadece isim ve
    // safeLimitWatt bekliyor; id, marka/model gibi alanlar sadece
    // bu formda gosterim amacli, backend'e gonderilmiyor. Gercek
    // id'ler kayittan sonra Core tarafindan uretiliyor.
    const registrationPayload = {
      name: homeName.trim(),
      contactEmail: contactEmail.trim(),
      budgetQuotaKwh: numericQuota,
      appliances: appliances.map((appliance) => ({
        name: appliance.name,
        safeLimitWatt: appliance.safeLimitWatt,
      })),
    };

    setFormError("");
    setIsSubmitting(true);

    try {
      await onAddHome(registrationPayload);
      resetForm();
      onClose();
    } catch (submitError) {
      console.error("Ev kaydedilemedi:", submitError);
      setFormError(
        "Ev kaydedilirken bir sorun oluştu. Backend'in çalıştığından emin olup tekrar deneyin."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="add-home-overlay" onClick={handleClose}>
      <div
        className="add-home-modal"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="add-home-header">
          <div>
            <h2>Yeni Ev Ekle</h2>
            <p>Yeni bir ev oluşturun ve cihazlarını listeye ekleyin.</p>
          </div>

          <button
            type="button"
            className="add-home-close"
            onClick={handleClose}
            aria-label="Kapat"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <section className="add-home-section">
            <h3>Ev Bilgileri</h3>

            <div className="add-home-grid">
              <label className="add-home-field">
                <span>Ev adı</span>

                <input
                  type="text"
                  value={homeName}
                  onChange={(event) => setHomeName(event.target.value)}
                  placeholder="Örneğin Kadıköy Evi"
                />
              </label>

              <label className="add-home-field">
                <span>İletişim e-postası</span>

                <input
                  type="email"
                  value={contactEmail}
                  onChange={(event) =>
                    setContactEmail(event.target.value)
                  }
                  placeholder="kullanici@example.com"
                />
              </label>

              <label className="add-home-field add-home-field--small">
                <span>Aylık kota</span>

                <div className="add-home-unit-input">
                  <input
                    type="number"
                    min="1"
                    step="0.1"
                    value={budgetQuotaKwh}
                    onChange={(event) =>
                      setBudgetQuotaKwh(event.target.value)
                    }
                    placeholder="300"
                  />

                  <strong>kWh</strong>
                </div>
              </label>
            </div>
          </section>

          <section className="add-home-section add-home-appliance-section">
            <div className="add-home-section-title">
              <div>
                <h3>Cihaz Ekle</h3>
                <p>
                  Cihaz türünü, markasını ve modelini seçerek listeye
                  ekleyin.
                </p>
              </div>
            </div>

            <div className="add-home-device-grid">
              <label className="add-home-field">
                <span>Cihaz türü</span>

                <select
                  value={applianceType}
                  onChange={handleTypeChange}
                >
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
                    <option
                      key={availableBrand}
                      value={availableBrand}
                    >
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

            <button
              type="button"
              className="add-home-device-button"
              onClick={handleAddAppliance}
            >
              + Listeye Ekle
            </button>
          </section>

          <section className="add-home-section">
            <div className="add-home-list-header">
              <div>
                <h3>Eklenen Cihazlar</h3>
                <p>{appliances.length} cihaz eklendi</p>
              </div>
            </div>

            {appliances.length === 0 ? (
              <div className="add-home-empty">
                <strong>Henüz cihaz eklenmedi</strong>
                <span>
                  Yukarıdan bir cihaz seçip “Listeye Ekle” butonuna
                  basın.
                </span>
              </div>
            ) : (
              <div className="add-home-device-list">
                {appliances.map((appliance) => (
                  <div
                    className="add-home-device-item"
                    key={appliance.id}
                  >
                    <div className="add-home-device-icon">
                      {getApplianceIcon(appliance.name)}
                    </div>

                    <div className="add-home-device-info">
                      <strong>{appliance.name}</strong>
                      <span>
                        {appliance.brand} {appliance.model}
                      </span>
                    </div>

                    <div className="add-home-device-power">
                      <strong>{appliance.wattage} W</strong>
                      <span>
                        Limit: {appliance.safeLimitWatt} W
                      </span>
                    </div>

                    <button
                      type="button"
                      className="add-home-remove-button"
                      onClick={() =>
                        handleRemoveAppliance(appliance.id)
                      }
                    >
                      Sil
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>

          {formError && (
            <p className="add-home-error">{formError}</p>
          )}

          <div className="add-home-actions">
            <button
              type="button"
              className="add-home-cancel-button"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              İptal
            </button>

            <button
              type="submit"
              className="add-home-save-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Kaydediliyor..." : "Evi Kaydet"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddHomeModal;