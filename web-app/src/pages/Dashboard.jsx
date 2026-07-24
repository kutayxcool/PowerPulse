import SummaryCards from "../components/SummaryCards";
import BudgetProgressBar from "../components/BudgetProgressBar";
import MiniConsumptionTrend from "../components/MiniConsumptionTrend";
import AIRecommendation from "../components/AIRecommendation";
import SearchFilter from "../components/SearchFilter";
import HomeCard from "../components/HomeCard";
import InfoHint from "../components/InfoHint";

function Dashboard({
  homes,
  filteredHomes,
  searchTerm,
  setSearchTerm,
  selectedStatus,
  setSelectedStatus,
  setSelectedHomeId,
  setEditingHomeId,
  getStatusText,
  homieRef,
  aiColumnRef,
  activeHint,
  onOpenHint,
}) {
  return (
    <>
      <div className="dashboard-top-grid">
        <aside className="dashboard-top-side" ref={aiColumnRef}>
          <AIRecommendation
            homes={homes}
            homieRef={homieRef}
            onOpenHint={onOpenHint}
            isPointing={Boolean(activeHint)}
          />
        </aside>

        <div className="dashboard-top-center">
          <SummaryCards homes={homes} onOpenHint={onOpenHint} />
          <MiniConsumptionTrend homes={homes} onOpenHint={onOpenHint} />
        </div>

        <aside className="dashboard-top-side">
          <BudgetProgressBar homes={homes} onOpenHint={onOpenHint} />
        </aside>
      </div>

      <div className="section-header">
        <div>
          <h2 className="section-title">Kayıtlı Evler</h2>
          <p className="section-description">
            Evlerin enerji tüketimlerini ve durumlarını
            görüntüleyin.
          </p>
        </div>

        <InfoHint
          id="registered-homes"
          text="Sisteme kayıtlı tüm evlerinizin listesi. Arama kutusuyla isme göre bulabilir, sağdaki menüyle duruma (normal/uyarı/kritik) göre filtreleyebilirsiniz."
          onOpen={onOpenHint}
        />
      </div>

      <SearchFilter
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        selectedStatus={selectedStatus}
        onStatusChange={setSelectedStatus}
      />

      <div className="home-grid">
        {filteredHomes.length > 0 ? (
          filteredHomes.map((home) => (
            <HomeCard
              key={home.id}
              home={home}
              onSelectHome={(selectedHome) =>
                setSelectedHomeId(selectedHome.id)
              }
              onEditHome={(editedHome) =>
                setEditingHomeId(editedHome.id)
              }
              getStatusText={getStatusText}
            />
          ))
        ) : (
          <div className="empty-state">
            Arama kriterlerine uygun ev bulunamadı.
          </div>
        )}
      </div>
    </>
  );
}

export default Dashboard;
