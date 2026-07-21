import { useEffect, useState } from "react";
import "./styles/index.css";
import { homes } from "./data/homes";

import Header from "./components/Header";
import HomeModal from "./components/HomeModal";
import Dashboard from "./pages/Dashboard";
import Analytics from "./pages/Analytics";
import LoadingScreen from "./components/LoadingScreen";
import ErrorScreen from "./components/ErrorScreen";

function App() {
  const [liveHomes, setLiveHomes] = useState(homes);
  const [selectedHomeId, setSelectedHomeId] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [activePage, setActivePage] = useState("dashboard");
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const getStatusText = (status) => {
    if (status === "danger") {
      return "Kota aşıldı";
    }

    if (status === "warning") {
      return "Sınıra yaklaşıldı";
    }

    return "Normal";
  };

  const getStatusByQuota = (quotaPercentage) => {
    if (quotaPercentage >= 100) {
      return "danger";
    }

    if (quotaPercentage >= 80) {
      return "warning";
    }

    return "normal";
  };

  const loadDashboardData = () => {
    setIsLoading(true);
    setError(null);

    const loadingTimer = setTimeout(() => {
      try {
        if (!Array.isArray(homes)) {
          throw new Error("Ev verileri geçerli formatta değil.");
        }

        setLiveHomes(homes);
        setLastUpdated(new Date());
      } catch (loadError) {
        console.error("Dashboard verileri yüklenemedi:", loadError);

        setError(
          "Enerji verileri yüklenirken bir sorun oluştu. Lütfen bağlantınızı kontrol edip tekrar deneyin."
        );
      } finally {
        setIsLoading(false);
      }
    }, 1200);

    return loadingTimer;
  };

  useEffect(() => {
    const loadingTimer = loadDashboardData();

    return () => clearTimeout(loadingTimer);
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
    }, 5000);

    return () => clearInterval(intervalId);
  }, [error]);

  const handleRetry = () => {
    loadDashboardData();
  };

  const filteredHomes = liveHomes.filter((home) => {
    const matchesSearch = home.name
      .toLocaleLowerCase("tr-TR")
      .includes(searchTerm.toLocaleLowerCase("tr-TR"));

    const matchesStatus =
      selectedStatus === "all" || home.status === selectedStatus;

    return matchesSearch && matchesStatus;
  });

  const selectedHome = liveHomes.find(
    (home) => home.id === selectedHomeId
  );

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (error) {
    return <ErrorScreen message={error} onRetry={handleRetry} />;
  }

  return (
    <div className="app">
      <Header
        activePage={activePage}
        onPageChange={setActivePage}
        lastUpdated={lastUpdated}
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
    </div>
  );
}

export default App;