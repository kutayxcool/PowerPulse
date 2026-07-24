// Kartlarin sag ust kosesine konan kucuk "?" butonu. Tiklaninca kendi
// ekran konumunu (getBoundingClientRect) ust bilesene (Dashboard'a)
// bildirir - boylece Homie'nin eli tam bu noktaya dogru
// uzatilabilir/isaret edebilir.
function InfoHint({ id, text, onOpen }) {
  const handleClick = (event) => {
    event.stopPropagation();
    const rect = event.currentTarget.getBoundingClientRect();
    onOpen(id, text, rect);
  };

  return (
    <button
      type="button"
      className="info-hint-button"
      onClick={handleClick}
      aria-label="Bu bölüm hakkında bilgi al"
      title="Bu bölüm hakkında bilgi al"
    >
      ?
    </button>
  );
}

export default InfoHint;
