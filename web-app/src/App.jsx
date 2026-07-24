import { useEffect, useRef, useState } from "react";
import "./styles/index.css";
import {
  getHomesWithDetails,
  registerHome,
  deleteHome,
  addAppliance,
  removeAppliance,
  updateApplianceStatus,
} from "./services/homeService";
import { ApiError } from "./services/api";
import { getCurrentUser, logout } from "./services/authService";

import Header from "./components/Header";
import HomeModal from "./components/HomeModal";
import EditHomeModal from "./components/EditHomeModal";
import Dashboard from "./pages/Dashboard";
import Analytics from "./pages/Analytics";
import Login from "./pages/Login";
import Register from "./pages/Register";
import LoadingScreen from "./components/LoadingScreen";
import ErrorScreen from "./components/ErrorScreen";
import AddHomeModal from "./components/AddHomeModal";
import HomiePointerOverlay from "./components/HomiePointerOverlay";

function App() {
  // Giris yapmis kullanici - sayfa yenilendiginde de oturumun
  // korunmasi icin localStorage'dan (bkz. authService) baslangic
  // degeri olarak okunur. null ise Giris/Kayit ekranlari gosterilir,
  // dashboard verisi hic cekilmeye calisilmaz.
  const [authUser, setAuthUser] = useState(() => getCurrentUser());
  const [authView, setAuthView] = useState("login");

  const [liveHomes, setLiveHomes] = useState([]);
  const [selectedHomeId, setSelectedHomeId] = useState(null);
  const [editingHomeId, setEditingHomeId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [activePage, setActivePage] = useState("dashboard");
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isAddHomeModalOpen, setIsAddHomeModalOpen] = useState(false);

  // Homie'nin "?" ipuclarina isaret etmesi icin gereken paylasilan
  // durum - Header (navigasyon), Dashboard (kartlar) VE Analytics
  // (grafikler) ayni Homie'ye ve ayni aktif ipucuna erisebilsin diye
  // App seviyesinde tutuluyor. Homie hem Ana Sayfa'da hem Analizler
  // sayfasinda kendi sol "aside" sutununda oturuyor - aiColumnRef, o
  // sutunun sagindaki bos koridoru (kolun gecebilecegi yolu) olcmek
  // icin kullanilir (bkz. HomiePointerOverlay).
  const homieRef = useRef(null);
  const aiColumnRef = useRef(null);
  const [activeHint, setActiveHint] = useState(null);

  const handleOpenHint = (id, text, rect) => {
    setActiveHint((current) =>
      current && current.id === id ? null : { id, text, rect }
    );
  };

  const handleCloseHint = () => setActiveHint(null);

  // "beige" = ana (acik) tema, "copper" ve "forest" = koyu tema secenekleri.
  const [theme, setTheme] = useState(
    () => localStorage.getItem("powerpulse-theme") || "beige"
  );

  useEffect(() => {
    document.body.setAttribute("data-theme", theme);
    localStorage.setItem("powerpulse-theme", theme);
  }, [theme]);

  const getStatusByQuota = (quotaPercentage) => {
    if (quotaPercentage > 100) {
      return "danger";
    }

    if (quotaPercentage >= 90) {
      return "warning";
    }

    return "normal";
  };

  const getStatusText = (status) => {
    switch (status) {
      case "normal":
        return "Normal";

      case "warning":
        return "Sınıra Yaklaşıldı";

      case "danger":
        return "Kota Aşıldı";

      default:
        return "Normal";
    }
  };

  const normalizeHomes = (data) => {
    if (!Array.isArray(data)) {
      throw new Error("Ev verileri geçerli formatta değil.");
    }

    return data.map((home) => ({
      ...home,
      status: home.status || getStatusByQuota(home.quotaPercentage),
    }));
  };

  // Oturumun suresi dolmus/token gecersizlesmisse (401), kullanici
  // otomatik olarak Giris ekranina donulur - baska bir kullanicinin
  // verisiyle karisik/bos bir dashboard gormesin.
  const handleAuthSuccess = (user) => {
    setAuthUser(user);
  };

  const handleLogout = () => {
    logout();
    setAuthUser(null);
    setLiveHomes([]);
    setError(null);
    setAuthView("login");
  };

  const loadDashboardData = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const data = await getHomesWithDetails();

      setLiveHomes(normalizeHomes(data));
      setLastUpdated(new Date());
    } catch (loadError) {
      console.error("Dashboard verileri yüklenemedi:", loadError);

      if (loadError instanceof ApiError && loadError.status === 401) {
        handleLogout();
        return;
      }

      setError(
        "Enerji verileri yüklenirken bir sorun oluştu. Lütfen bağlantınızı kontrol edip tekrar deneyin."
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Sayfayi kilitlemeden arka planda backend'den taze veri ceker.
  // Gecici bir ag hatasi kullaniciyi hata ekranina atmasin diye
  // sessizce loglanir, ekrandaki mevcut veri korunur.
  const refreshDashboardData = async () => {
    try {
      const data = await getHomesWithDetails();

      setLiveHomes(normalizeHomes(data));
      setLastUpdated(new Date());
    } catch (refreshError) {
      console.error("Canlı veri yenilenemedi:", refreshError);

      if (refreshError instanceof ApiError && refreshError.status === 401) {
        handleLogout();
      }
    }
  };

  useEffect(() => {
    if (!authUser) {
      return;
    }

    loadDashboardData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authUser]);

  useEffect(() => {
    if (error || !authUser) {
      return undefined;
    }

    // CONTRACTS.md'ye gore canli veri en fazla 1-2 saniyede bir
    // yenilenmeli; burada gercek backend'den poll ediyoruz.
    const intervalId = setInterval(refreshDashboardData, 2000);

    return () => clearInterval(intervalId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error, authUser]);

  const handleRetry = () => {
    loadDashboardData();
  };

  const handleAddHome = async (newHomePayload) => {
    await registerHome(newHomePayload);
    await refreshDashboardData();
    setActivePage("dashboard");
  };

  const handleDeleteHome = async (homeId) => {
    await deleteHome(homeId);
    setSelectedHomeId(null);
    setEditingHomeId(null);
    await refreshDashboardData();
  };

  const handleAddAppliance = async (homeId, payload) => {
    await addAppliance(homeId, payload);
    await refreshDashboardData();
  };

  const handleRemoveAppliance = async (homeId, applianceId) => {
    await removeAppliance(homeId, applianceId);
    await refreshDashboardData();
  };

  const handleToggleAppliance = async (homeId, applianceId, active) => {
    await updateApplianceStatus(homeId, applianceId, active);
    await refreshDashboardData();
  };

  const filteredHomes = liveHomes.filter((home) => {
    const homeName = home.name || "";

    const matchesSearch = homeName
      .toLocaleLowerCase("tr-TR")
      .includes(searchTerm.toLocaleLowerCase("tr-TR"));

    const matchesStatus =
      selectedStatus === "all" ||
      home.status === selectedStatus;

    return matchesSearch && matchesStatus;
  });

  const selectedHome = liveHomes.find(
    (home) => home.id === selectedHomeId
  );

  const editingHome = liveHomes.find(
    (home) => home.id === editingHomeId
  );

  if (!authUser) {
    return authView === "login" ? (
      <Login
        onSuccess={handleAuthSuccess}
        onSwitchToRegister={() => setAuthView("register")}
      />
    ) : (
      <Register
        onSuccess={handleAuthSuccess}
        onSwitchToLogin={() => setAuthView("login")}
      />
    );
  }

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (error) {
    return (
      <ErrorScreen
        message={error}
        onRetry={handleRetry}
      />
    );
  }

  return (
    <div className="app">
      <Header
        activePage={activePage}
        onPageChange={setActivePage}
        lastUpdated={lastUpdated}
        onAddHome={() => setIsAddHomeModalOpen(true)}
        theme={theme}
        onThemeChange={setTheme}
        homes={liveHomes}
        onOpenHint={handleOpenHint}
        currentUser={authUser}
        onLogout={handleLogout}
      />

      <main>
        {activePage === "dashboard" && (
          <Dashboard
            homes={liveHomes}
            filteredHomes={filteredHomes}
            searchTerm={searchTerm}
            setSearchTerm={setSearchTerm}
            selectedStatus={selectedStatus}
            setSelectedStatus={setSelectedStatus}
            setSelectedHomeId={setSelectedHomeId}
            setEditingHomeId={setEditingHomeId}
            getStatusText={getStatusText}
            homieRef={homieRef}
            aiColumnRef={aiColumnRef}
            activeHint={activeHint}
            onOpenHint={handleOpenHint}
          />
        )}

        {activePage === "analytics" && (
          <Analytics
            homes={liveHomes}
            homieRef={homieRef}
            aiColumnRef={aiColumnRef}
            activeHint={activeHint}
            onOpenHint={handleOpenHint}
          />
        )}
      </main>

      <HomiePointerOverlay
        activeHint={activeHint}
        homieRef={homieRef}
        aiColumnRef={aiColumnRef}
        onClose={handleCloseHint}
      />

      <HomeModal
        home={selectedHome}
        onClose={() => setSelectedHomeId(null)}
      />
      <EditHomeModal
        home={editingHome}
        onClose={() => setEditingHomeId(null)}
        onAddAppliance={handleAddAppliance}
        onRemoveAppliance={handleRemoveAppliance}
        onToggleAppliance={handleToggleAppliance}
        onDeleteHome={handleDeleteHome}
      />
      <AddHomeModal
        isOpen={isAddHomeModalOpen}
        onClose={() => setIsAddHomeModalOpen(false)}
        onAddHome={handleAddHome}
      />
    </div>
  );
}

export default App;