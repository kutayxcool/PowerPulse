import SummaryCards from "../components/SummaryCards";
import AIRecommendation from "../components/AIRecommendation";
import SearchFilter from "../components/SearchFilter";
import HomeCard from "../components/HomeCard";

function Dashboard({
  homes,
  filteredHomes,
  searchTerm,
  setSearchTerm,
  selectedStatus,
  setSelectedStatus,
  setSelectedHomeId,
  getStatusText,
}) {
  return (
    <>
      <SummaryCards homes={homes} />

      <AIRecommendation homes={homes} />

      <div className="section-header">
        <div>
          <h2 className="section-title">Kayıtlı Evler</h2>
          <p className="section-description">
            Evlerin enerji tüketimlerini ve durumlarını
            görüntüleyin.
          </p>
        </div>
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