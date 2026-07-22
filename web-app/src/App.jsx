import { useEffect, useState } from "react";
import "./styles/index.css";
import { getHomes } from "./services/homeService";

import Header from "./components/Header";
import HomeModal from "./components/HomeModal";
import Dashboard from "./pages/Dashboard";
import Analytics from "./pages/Analytics";
import LoadingScreen from "./components/LoadingScreen";
import ErrorScreen from "./components/ErrorScreen";
import AddHomeModal from "./components/AddHomeModal";

function App() {
  const [liveHomes, setLiveHomes] = useState([]);
  const [selectedHomeId, setSelectedHomeId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [activePage, setActivePage] = useState("dashboard");
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isAddHomeModalOpen, setIsAddHomeModalOpen] = useState(false);

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

  const loadDashboardData = async () => {
    setIsLoading(true);
    setError(null);

    try {
      await new Promise((resolve) => setTimeout(resolve, 1200));

      const data = await getHomes();

      if (!Array.isArray(data)) {
        throw new Error("Ev verileri geçerli formatta değil.");
      }

      const normalizedHomes = data.map((home) => ({
        ...home,
        status:
          home.status || getStatusByQuota(home.quotaPercentage),
      }));

      setLiveHomes(normalizedHomes);
      setLastUpdated(new Date());
    } catch (loadError) {
      console.error("Dashboard verileri yüklenemedi:", loadError);

      setError(
        "Enerji verileri yüklenirken bir sorun oluştu. Lütfen bağlantınızı kontrol edip tekrar deneyin."
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  useEffect(() => {
    if (error) {
      return undefined;
    }

    const intervalId = setInterval(() => {
      setLiveHomes((previousHomes) =>
        previousHomes.map((home) => {
          const consumptionIncrease = Number(
            (Math.random() * 1.5).toFixed(1)
          );

          const newConsumption = Number(
            (home.consumption + consumptionIncrease).toFixed(1)
          );

          const newBill = Number(
            (newConsumption * 2.1).toFixed(2)
          );

          const newQuotaPercentage = Math.min(
            120,
            Math.round(home.quotaPercentage + Math.random() * 2)
          );

          return {
            ...home,
            consumption: newConsumption,
            bill: newBill,
            quotaPercentage: newQuotaPercentage,
            status: getStatusByQuota(newQuotaPercentage),
          };
        })
      );

      setLastUpdated(new Date());
    }, 2000);

    return () => clearInterval(intervalId);
  }, [error]);

  const handleRetry = () => {
    loadDashboardData();
  };

  const handleAddHome = (newHome) => {
      setLiveHomes((previousHomes) => [
        ...previousHomes,
        newHome,
      ]);

      setLastUpdated(new Date());
      setActivePage("dashboard");
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
            getStatusText={getStatusText}
          />
        )}

        {activePage === "analytics" && (
          <Analytics homes={liveHomes} />
        )}
      </main>

      <HomeModal
        home={selectedHome}
        onClose={() => setSelectedHomeId(null)}
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