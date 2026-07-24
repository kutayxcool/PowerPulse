import { useEffect, useRef, useState } from "react";
import HomieMascot from "./HomieMascot";

// Bir InfoHint'e (kartlardaki/grafiklerdeki "?" butonlarina) tiklaninca
// gorunen genel katman. Homie hem Ana Sayfa'da hem Analizler
// sayfasinda, ekranin SOL tarafindaki dar bir "aside" sutununda
// oturuyor. O yuzden hedefe giden kol, Homie'nin omzundan cikip
// once bu aside sutununun HEMEN SAGINDAKI bos dikey koridordan
// ("gutter") gider - bu koridor, aside sutunuyla ayni satirdaki
// butun kartlar/grafikler boyunca gercekten bostur (grid-gap). Hedef
// Homie ile ayni hizadaysa bu koridor cok kisa surer (neredeyse duz
// bir cizgi); hedef daha asagidaki bir grafikse koridor boyunca
// asagi inilip sonra hedefe girilir - ama hicbir zaman sayfanin
// tamamen diger ucuna (sag kenara) dolanmaz. Aside sutunu
// bulunamazsa (beklenmeyen bir yerlesim), guvenli sekilde dogrudan
// kisa bir yol kullanilir. Homie o an ekranda degilse (baska bir
// sayfadaysa) sadece aciklama balonu, tiklanan butonun yanindan
// gorunur (kol cizilmez). Scroll olursa (konumlar bozulmasin diye)
// otomatik kapanir.
function HomiePointerOverlay({ activeHint, homieRef, aiColumnRef, onClose }) {
  const [rects, setRects] = useState(null);
  const pathRef = useRef(null);
  const [pathLength, setPathLength] = useState(0);

  useEffect(() => {
    if (!activeHint) {
      setRects(null);
      return;
    }

    setRects({
      homie: homieRef.current
        ? homieRef.current.getBoundingClientRect()
        : null,
      aiColumn:
        aiColumnRef && aiColumnRef.current
          ? aiColumnRef.current.getBoundingClientRect()
          : null,
    });
  }, [activeHint, homieRef, aiColumnRef]);

  // Konumlar tek seferlik yakalaniyor - scroll sirasinda gecersiz hale
  // gelmesin diye, scroll basladigi an ipucu tamamen kapatilir.
  useEffect(() => {
    if (!activeHint) {
      return undefined;
    }

    const handleScroll = () => onClose();
    window.addEventListener("scroll", handleScroll, true);

    return () => window.removeEventListener("scroll", handleScroll, true);
  }, [activeHint, onClose]);

  const hasHomie = Boolean(rects && rects.homie);

  let pathD = "";
  let endX = 0;
  let endY = 0;

  if (activeHint && hasHomie) {
    const homieRect = rects.homie;

    // Bu cizgi Homie'nin gercek kolunun YERINE geciyor (rightArmRaised
    // ile HomieMascot'ta o kol hic cizilmiyor), o yuzden tam OMUZ
    // noktasindan baslamali (SVG'deki "M77 55" omuz noktasiyla ayni
    // oran) - boylece tek parca, govdeye yapisik bir kol gibi gorunur.
    const startX = homieRect.left + homieRect.width * 0.77;
    const startY = homieRect.top + homieRect.height * 0.493;

    endX = activeHint.rect.left + activeHint.rect.width / 2;
    // "?" butonlari her zaman kartin sag-ustunde oldugu icin, hedefe
    // ASAGIDAN degil YUKARIDAN yaklasilir - kartin icinden gecmek
    // gerekmez, sadece kisa bir inis yeterli olur.
    endY = activeHint.rect.top - 8;

    // Aside sutunun HEMEN sagindaki bos koridor - hedef ayni hizadaysa
    // (ör. yan sutundaki bir grafik) neredeyse duz bir cizgi olur,
    // hedef daha asagidaysa bu koridordan kisa yoldan inilir. Aside
    // bulunamazsa (guvenlik icin), Homie'nin kendi konumunun hemen
    // sagi kullanilir - boylece hicbir zaman sayfanin diger ucuna
    // dolanmaz.
    const gutterX = rects.aiColumn
      ? rects.aiColumn.right + 11
      : startX + 24;

    const points = [
      [startX, startY],
      [gutterX, startY],
      [gutterX, endY],
      [endX, endY],
    ];

    pathD = points
      .map(
        (point, index) => `${index === 0 ? "M" : "L"} ${point[0]} ${point[1]}`
      )
      .join(" ");
  }

  // Kol, hazir/durgun cizilmis olarak degil, Homie'den hedefe dogru
  // GERCEKTEN UZANIYORMUS gibi gorunsun diye: yolun toplam uzunlugu
  // olculur, cizgi once tamamen "gizli" (dashoffset = uzunluk) baslar,
  // sonra CSS animasyonuyla 0'a inerek cizilir.
  useEffect(() => {
    if (pathRef.current && pathD) {
      setPathLength(pathRef.current.getTotalLength());
    }
  }, [pathD]);

  if (!activeHint || !rects) {
    return null;
  }

  // Aciklama balonu: Homie ekrandaysa onun hemen yaninda, degilse
  // (ör. Ana Sayfa'da) dogrudan tiklanan "?" butonunun yaninda,
  // ekrandan tasmayacak sekilde konumlanir.
  const bubbleWidth = 280;
  let bubbleTop;
  let bubbleLeft;

  if (hasHomie) {
    bubbleTop = Math.max(16, rects.homie.top - 210);
    bubbleLeft = Math.min(
      Math.max(16, rects.homie.left - 20),
      window.innerWidth - bubbleWidth - 16
    );
  } else {
    bubbleTop = Math.min(
      Math.max(16, activeHint.rect.bottom + 14),
      window.innerHeight - 220
    );
    bubbleLeft = Math.min(
      Math.max(16, activeHint.rect.left - 20),
      window.innerWidth - bubbleWidth - 16
    );
  }

  return (
    <div
      className="homie-pointer-overlay"
      onClick={onClose}
      role="presentation"
    >
      {hasHomie && (
        <svg className="homie-pointer-svg">
          <path
            key={activeHint.id}
            ref={pathRef}
            d={pathD}
            className={`homie-pointer-path ${
              pathLength > 1 ? "is-drawing" : ""
            }`}
            style={{
              strokeDasharray: pathLength || 1,
              strokeDashoffset: pathLength || 1,
            }}
          />
        </svg>
      )}

      {hasHomie && (
        <div
          key={`${activeHint.id}-hand`}
          className="homie-pointer-hand"
          style={{
            left: endX,
            top: endY,
            animationDelay: "0.5s",
          }}
          aria-hidden="true"
        />
      )}

      <div
        className="homie-hint-bubble"
        style={{ top: bubbleTop, left: bubbleLeft, width: bubbleWidth }}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="homie-hint-bubble-header">
          <HomieMascot size={26} />
          <span>Homie açıklıyor</span>
          <button
            type="button"
            className="homie-hint-close"
            onClick={onClose}
            aria-label="Kapat"
          >
            ×
          </button>
        </div>

        <p>{activeHint.text}</p>
      </div>
    </div>
  );
}

export default HomiePointerOverlay;
