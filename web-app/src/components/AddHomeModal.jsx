import { useState } from "react";

const applianceCatalog = {
  Buzdolabı: {
    Arçelik: [
      {
        model: "Eco 90",
        wattage: 90,
        safeLimitWatt: 120,
      },
      {
        model: "Fresh 110",
        wattage: 110,
        safeLimitWatt: 140,
      },
    ],
    Bosch: [
      {
        model: "Serie 4",
        wattage: 100,
        safeLimitWatt: 135,
      },
      {
        model: "Serie 6",
        wattage: 115,
        safeLimitWatt: 150,
      },
    ],
  },

  Klima: {
    Samsung: [
      {
        model: "WindFree 1200",
        wattage: 1200,
        safeLimitWatt: 1600,
      },
      {
        model: "WindFree 1800",
        wattage: 1800,
        safeLimitWatt: 2200,
      },
    ],
    Vestel: [
      {
        model: "Flora 1200",
        wattage: 1150,
        safeLimitWatt: 1550,
      },
      {
        model: "Flora 1800",
        wattage: 1750,
        safeLimitWatt: 2150,
      },
    ],
  },

  "Çamaşır Makinesi": {
    Beko: [
      {
        model: "CM 8100",
        wattage: 500,
        safeLimitWatt: 700,
      },
      {
        model: "CM 9100",
        wattage: 650,
        safeLimitWatt: 850,
      },
    ],
    Siemens: [
      {
        model: "iQ300",
        wattage: 600,
        safeLimitWatt: 800,
      },
      {
        model: "iQ500",
        wattage: 750,
        safeLimitWatt: 950,
      },
    ],
  },

  Televizyon: {
    LG: [
      {
        model: "NanoCell 55",
        wattage: 90,
        safeLimitWatt: 130,
      },
      {
        model: "OLED 65",
        wattage: 130,
        safeLimitWatt: 180,
      },
    ],
    Samsung: [
      {
        model: "Crystal UHD 55",
        wattage: 100,
        safeLimitWatt: 140,
      },
      {
        model: "Neo QLED 65",
        wattage: 145,
        safeLimitWatt: 195,
      },
    ],
  },
};

const initialType = Object.keys(applianceCatalog)[0];
const initialBrand = Object.keys(applianceCatalog[initialType])[0];
const initialModel = applianceCatalog[initialType][initialBrand][0].model;

function AddHomeModal({ isOpen, onClose, onAddHome }) {
  const [homeName, setHomeName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [budgetQuotaKwh, setBudgetQuotaKwh] = useState("");

  const [applianceType, setApplianceType] = useState(initialType);
  const [brand, setBrand] = useState(initialBrand);
  const [model, setModel] = useState(initialModel);

  const [appliances, setAppliances] = useState([]);
  const [formError, setFormError] = useState("");

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
  };

  const handleBrandChange = (event) => {
    const selectedBrand = event.target.value;
    const firstModel =
      applianceCatalog[applianceType][selectedBrand][0].model;

    setBrand(selectedBrand);
    setModel(firstModel);
  };

  const handleAddAppliance = () => {
    const newAppliance = {
      id: crypto.randomUUID(),
      name: applianceType,
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

    setAppliances([]);
    setFormError("");
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = (event) => {
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

    const newHome = {
      id: crypto.randomUUID(),
      name: homeName.trim(),
      contactEmail: contactEmail.trim(),
      budgetQuotaKwh: numericQuota,
      consumption: 0,
      bill: 0,
      quotaPercentage: 0,
      status: "normal",
      appliances,
      dailyConsumption: [
        { day: "Pzt", consumption: 0 },
        { day: "Sal", consumption: 0 },
        { day: "Çar", consumption: 0 },
        { day: "Per", consumption: 0 },
        { day: "Cum", consumption: 0 },
        { day: "Cmt", consumption: 0 },
        { day: "Paz", consumption: 0 },
      ],
    };

    onAddHome(newHome);
    resetForm();
    onClose();
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
                      {appliance.name === "Buzdolabı" && "🧊"}
                      {appliance.name === "Klima" && "❄️"}
                      {appliance.name === "Çamaşır Makinesi" && "🫧"}
                      {appliance.name === "Televizyon" && "📺"}
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
            >
              İptal
            </button>

            <button
              type="submit"
              className="add-home-save-button"
            >
              Evi Kaydet
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddHomeModal;