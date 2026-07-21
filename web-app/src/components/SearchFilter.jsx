function SearchFilter({
  searchTerm,
  onSearchChange,
  selectedStatus,
  onStatusChange,
}) {
  return (
    <section className="search-filter">
      <input
        type="text"
        placeholder="Ev ara..."
        value={searchTerm}
        onChange={(event) => onSearchChange(event.target.value)}
      />

      <select
        value={selectedStatus}
        onChange={(event) => onStatusChange(event.target.value)}
      >
        <option value="all">Tümü</option>
        <option value="normal">Normal</option>
        <option value="warning">Sınıra Yaklaşıldı</option>
        <option value="danger">Kota Aşıldı</option>
      </select>
    </section>
  );
}

export default SearchFilter;