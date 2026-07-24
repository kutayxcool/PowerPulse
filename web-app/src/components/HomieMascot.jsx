import { useEffect, useRef, useState } from "react";

// PowerPulse'un maskotu "Homie" - header'daki marka logomuzla BIREBIR
// ayni ev siluetini kullanir: cati logomuzdaki gibi iki acik cizgiden
// olusur (kapali bir ucgen degil), govde de logomuzdaki gibi ici bos/
// beyaz ve sadece mavi bir cizgiyle cevrilidir, kapi kaldirilmistir.
// Bunun uzerine sadece 4 sey eklenmistir: goz, agiz, el ve ayak - hepsi
// marka mavisiyle. Marka rengi (mavi) hicbir temada degismez - tipki
// Header'daki ev logosu gibi "kutsal" sabit renkte kalir.
function HomieMascot({
  size = 56,
  className = "",
  swingFeet = false,
  legLength = 7,
  gripBox = false,
  handShiftPx = 0,
  mood = "normal",
  lookX = 0.5,
  rightArmRaised = false,
  trackMouse = false,
}) {
  const svgRef = useRef(null);
  // trackMouse: sadece "normal" ruh halindeyken (yazi yazmiyor,
  // kutucuk cevrilmemis) Homie'nin gozleri fare imlecini takip eder -
  // sadece gozler, kafa/govde sabit kalir. Diger ruh halleri (sasirmis/
  // dusunceli) kendi sabit bakislarini korur, fareyi yok sayar.
  const [mouseOffset, setMouseOffset] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (!trackMouse) {
      return undefined;
    }

    let frameId = null;

    const handleMouseMove = (event) => {
      if (frameId) {
        return;
      }

      frameId = requestAnimationFrame(() => {
        frameId = null;

        if (!svgRef.current) {
          return;
        }

        const rect = svgRef.current.getBoundingClientRect();
        // Goz hizasi, SVG kutusunun ust yariisina denk gelir (gozler
        // govdenin yukari kesiminde, ~%55-60 civarinda).
        const eyeCenterX = rect.left + rect.width * 0.5;
        const eyeCenterY = rect.top + rect.height * 0.42;
        const dx = event.clientX - eyeCenterX;
        const dy = event.clientY - eyeCenterY;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 4) {
          setMouseOffset({ x: 0, y: 0 });
          return;
        }

        const maxOffset = 3;
        setMouseOffset({
          x: (dx / distance) * maxOffset,
          y: (dy / distance) * maxOffset,
        });
      });
    };

    window.addEventListener("mousemove", handleMouseMove);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      if (frameId) {
        cancelAnimationFrame(frameId);
      }
    };
  }, [trackMouse]);

  const isSurprised = mood === "surprised";
  // "dusunceli" mod - AI kartinin arka yuzunde (Homie'nin onerisi
  // gosterilirken) kullanilir: gozler yukari kayar (sanki dusunuyormus
  // gibi havaya bakar), kaslardan biri digerinden daha yukarida/egik
  // durur, agiz gulumseme yerine kucuk, duz/sikilmis bir cizgidir.
  const isThoughtful = mood === "thoughtful";
  // "Duz/normal" (ozel bir ruh hali olmayan) durum - hem ana logodaki
  // (header) statik Homie hem de fareyi takip eden oturan Homie bu
  // gruba girer; ikisi de AYNI goz halkasi + gulumseme stilini
  // kullanir, tek fark fareyi takip edip etmemeleridir.
  const isPlainNormal = !isSurprised && !isThoughtful;
  // Fareyi takip etmek sadece "normal" ruh halindeyken anlamlidir -
  // sasirmis/dusunceli kendi sabit bakislarini korur, fareyi yok sayar.
  const isTrackingMouse = trackMouse && isPlainNormal;
  // Goz halkasi (beyaz kure + mavi bebek) stili artik TUM Homie'ler
  // icin ortak/tek gorunum - ana logo da dahil ("homieler ayni olsun"
  // istegi). Fare takip etmiyorsa bebek sadece merkezde durur.
  // lookX: 0 = tam sola bak, 1 = tam saga bak, 0.5 = merkez. Goz
  // bebeginin, beyaz goz kuresi icinde ne kadar kayacagini belirler -
  // "Homie'ye Sor" kutusuna yazi yazarken imlecin/son harfin oldugu
  // yone bakiyormus hissi vermek icin kullanilir. Dusunceli modda
  // yazi yazma durumu olmadigi icin sabit, hafif yana kaymis bir bakis
  // kullanilir (dusunurken yukari-yana bakma hissi). Fare takibinde
  // ise gercek imlec yonu (mouseOffset) kullanilir.
  const maxPupilOffset = 3.2;
  const pupilOffsetX = isThoughtful
    ? 1.8
    : isTrackingMouse
    ? mouseOffset.x
    : (Math.min(1, Math.max(0, lookX)) - 0.5) * 2 * maxPupilOffset;
  // Yazi kutusu Homie'nin altinda oldugu icin sasirinca goz bebekleri
  // hem yatayda (lookX) hem de asagiya dogru kayar - tam olarak
  // yazdigi kutuya bakiyormus gibi. Dusunurken ise yukari bakar.
  const pupilOffsetY = isSurprised
    ? 2.8
    : isThoughtful
    ? -3.4
    : isTrackingMouse
    ? mouseOffset.y
    : 0;
  const leftFootClass = swingFeet ? "homie-foot homie-foot--left" : "";
  const rightFootClass = swingFeet ? "homie-foot homie-foot--right" : "";

  // Bacaklar varsayilan olarak 7 birim uzunlugunda (govdenin geri
  // kalaniyla ayni oranda). "Bank gibi oturan" Homie'de daha uzun
  // bacak istenirse (legLength arttirilirsa), viewBox yuksekligi de
  // orantili buyutulur ki ayaklar tekrar kesilmesin - genislik/
  // yukseklik orani hep 1 birim = ayni px kalir, goruntu bozulmaz.
  // Bacak artik duz bir dikdortgen degil, dizden kirilan bir cizgi -
  // kalca (84) -> diz (yaklasik ortada, disa dogru) -> ayak bilegi.
  const kneeY = 84 + legLength * 0.5;
  const ankleY = 84 + legLength;
  // viewBox, ayagin gercek alt noktasindan (ankleY + elips yaricapi)
  // fazla bosluk birakmayacak sekilde siki tutuluyor - boylece disaridan
  // "bottom: 100%" ile hizalandiginda ayagin altinda gizli bir bosluk
  // kalmaz, gercekten deger.
  const viewBoxHeight = Math.max(100, ankleY + 3.6 + 2);

  // gripBox: eller govdenin disindan asagi inip, ayaklarla ayni
  // "zemin" seviyesinde (ankleY) kutunun kenarini tutuyormus gibi durur.
  // Disari cikan bir yörünge izler ki dizle (34/66) ust uste binmesin.
  const groundY = ankleY;

  // Disaridan (CSS'te) tum ikon asagi kaydirilmissa (ör. govde bilinçli
  // olarak butona biraz gomulsun diye translateY uygulaniyorsa),
  // ellerin GERCEKTEN butonun ust kenarini tutuyormus gibi gorunmesi
  // icin bu kaymayi telafi eden, daha yukarida bir hedef nokta
  // kullanilir - ayaklar (groundY) ise oldugu gibi kalir.
  const pxToUnit = 100 / size;
  const handGroundY = groundY - handShiftPx * pxToUnit;

  return (
    <svg
      ref={svgRef}
      className={`homie-mascot ${className}`}
      width={size}
      height={(size * viewBoxHeight) / 100}
      viewBox={`0 0 100 ${viewBoxHeight}`}
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      {/* govde - logomuzdaki duvar siluetiyle ayni: ust kenari yok
          (cati onu kapatiyor), alt kosele yuvarlatilmis, kapisiz.
          Ic dolgu YOK (fill="none") - boylece hangi arka plan
          uzerinde durursa dursun (beyaz kart, bej logo kutusu, koyu
          tema...) her zaman o zeminle birebir kaynasir, ayrica bir
          "beyaz yama" gibi gorunmez. */}
      <path
        d="M22 42 L22 80 Q22 84 26 84 L74 84 Q78 84 78 80 L78 42"
        fill="none"
        stroke="#1769aa"
        strokeWidth="7.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* cati - logomuzdaki gibi iki acik cizgi, kapali ucgen degil.
          Apex yuksekligi logomuzdaki oranla ayni (cati yuksekligi,
          govde yuksekliginin ~%60'i - logoda da boyle, oncesinde cati
          cok sivri/yuksekti). Eave uclari govdenin ust koseleriyle
          tam ayni dogru uzerinde olacak sekilde hesaplandi - boylece
          cati govdeye gercekten deiyor. Tum sekil, ayaklarin viewBox
          disina tasip kesilmemesi icin 8 birim yukari kaydirildi. */}
      <path
        d="M19.2 44.5 L50 17 L80.8 44.5"
        fill="none"
        stroke="#1769aa"
        strokeWidth="7.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {gripBox ? (
        <>
          {/* sol el - govdenin disindan (dizin disindan gecerek, ust
              uste binmeden) inip ayaklarla ayni zemin seviyesinde
              kutunun kenarini tutar */}
          <path
            d={`M23 55 Q6 ${(55 + handGroundY) / 2} 8 ${handGroundY}`}
            stroke="#1769aa"
            strokeWidth="7"
            strokeLinecap="round"
            fill="none"
          />
          <circle cx="8" cy={handGroundY} r="6" fill="#1769aa" />

          {/* sag el - normalde disaridan sag kenari tutar. Homie bir
              kutuyu isaret ederken (rightArmRaised) bu kol hic
              cizilmez - onun yerine disaridaki (HomiePointerOverlay)
              mavi cizgi tam omuzdan baslayip tek parca bir kol gibi
              hedefe uzanir; iki ayri kol gorunmesin diye. */}
          {!rightArmRaised && (
            <>
              <path
                d={`M77 55 Q94 ${(55 + handGroundY) / 2} 92 ${handGroundY}`}
                stroke="#1769aa"
                strokeWidth="7"
                strokeLinecap="round"
                fill="none"
              />
              <circle cx="92" cy={handGroundY} r="6" fill="#1769aa" />
            </>
          )}
        </>
      ) : (
        <>
          {/* sol el */}
          <circle cx="10" cy="76" r="6.5" fill="#1769aa" />
          <path
            d="M22 70 Q8 70 10 80"
            stroke="#1769aa"
            strokeWidth="7"
            strokeLinecap="round"
            fill="none"
          />

          {/* sag el (sallanan) */}
          <circle cx="90" cy="50" r="6.5" fill="#1769aa" />
          <path
            d="M78 62 Q94 56 90 44"
            stroke="#1769aa"
            strokeWidth="7"
            strokeLinecap="round"
            fill="none"
          />
        </>
      )}

      {/* sol ayak - dizden kirilan bacak + ayak. swingFeet true ise
          (kucuk "oturan" Homie'de) kalcadan sallanir gibi
          animasyonlanir */}
      <g className={leftFootClass}>
        <path
          d={`M39 84 L34 ${kneeY} L39 ${ankleY}`}
          stroke="#1769aa"
          strokeWidth="7"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
        <ellipse cx="39" cy={ankleY} rx="6.5" ry="3.6" fill="#1769aa" />
      </g>

      {/* sag ayak */}
      <g className={rightFootClass}>
        <path
          d={`M61 84 L66 ${kneeY} L61 ${ankleY}`}
          stroke="#1769aa"
          strokeWidth="7"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
        <ellipse cx="61" cy={ankleY} rx="6.5" ry="3.6" fill="#1769aa" />
      </g>

      <>
          {isThoughtful && (
            <>
              {/* dusunceli kaslar - asimetrik, biri digerinden daha
                  yukarida/egik (soru soruyormus/dusunuyormus gibi) */}
              <path
                d="M30 49 Q37 47 45 49"
                stroke="#1769aa"
                strokeWidth="3"
                strokeLinecap="round"
                fill="none"
              />
              <path
                d="M55 45 Q63 39 71 43"
                stroke="#1769aa"
                strokeWidth="3"
                strokeLinecap="round"
                fill="none"
              />
            </>
          )}

          {isSurprised && (
            <>
              {/* sasirmis kaslar - yukarida, kavisli */}
              <path
                d="M30 48 Q38 43 46 48"
                stroke="#1769aa"
                strokeWidth="3"
                strokeLinecap="round"
                fill="none"
              />
              <path
                d="M54 48 Q62 43 70 48"
                stroke="#1769aa"
                strokeWidth="3"
                strokeLinecap="round"
                fill="none"
              />
            </>
          )}

          {/* isTrackingMouse (sadece fare takibi, ozel bir ruh hali
              yok) durumunda kas cizilmez - rahat/normal yuz ifadesi
              korunur, sadece gozler kayar. */}

          {/* hareketli gozler - eski/orijinal kucuk nokta goz stili,
              halkasiz (yazi yazarken imlece, dusunurken yukari bakar) */}
          <circle
            cx={38 + pupilOffsetX}
            cy={58 + pupilOffsetY}
            r="3.4"
            fill="#1769aa"
          />
          <circle
            cx={62 + pupilOffsetX}
            cy={58 + pupilOffsetY}
            r="3.4"
            fill="#1769aa"
          />

          {isThoughtful && (
            /* dusunceli agiz - kucuk, duz/sikilmis bir cizgi (dudak
               bukme, "hmm" ifadesi) */
            <path
              d="M43 73 Q50 71 57 73"
              stroke="#1769aa"
              strokeWidth="3.4"
              strokeLinecap="round"
              fill="none"
            />
          )}

          {isSurprised && (
            /* sasirmis agiz - kucuk "o" */
            <circle
              cx="50"
              cy="74"
              r="4.4"
              fill="none"
              stroke="#1769aa"
              strokeWidth="3.4"
            />
          )}

          {isPlainNormal && (
            /* normal (ozel ruh hali olmayan) gulumseme agzi - hem
               statik ana logo hem de fareyi takip eden Homie icin
               ayni; sadece gozlerin hareketli olup olmamasi degisir */
            <path
              d="M40 70 Q50 78 60 70"
              stroke="#1769aa"
              strokeWidth="4"
              strokeLinecap="round"
              fill="none"
            />
          )}
      </>
    </svg>
  );
}

export default HomieMascot;
