import { useEffect, useState } from "react";
import { askAssistant } from "../services/aiService";
import HomieMascot from "./HomieMascot";
import InfoHint from "./InfoHint";

// Gercek AI cevabi gelene kadar (ya da hic gelmezse) gosterilecek,
// onceden hazirlanmis oneri havuzu. Backend'in RecommendationResponse
// semasiyla ayni sekle sahip, boylece render kodu ikisini de ayni
// sekilde islenebiliyor. Amac: kullanici karta bastiginda "yukleniyor"
// yazisiyla karsilasmasin, her zaman aninda bir icerik gorsun.
const FALLBACK_RECOMMENDATIONS = [
  {
    title: "Enerji Tasarrufu Önerisi",
    recommendations: [
      "Yüksek tüketimli cihazları gece tarifesine kaydırarak fatura maliyetini azaltabilirsiniz.",
      "Kullanılmayan odalardaki klima ve ısıtıcıları kapalı tutmak tüketimi belirgin şekilde düşürür.",
      "Buzdolabı ve derin dondurucu gibi sürekli çalışan cihazların kapı contalarını kontrol edin.",
    ],
    estimatedSavingPercentage: 12,
    estimatedSavingAmount: 45.5,
  },
  {
    title: "Akıllı Tüketim Tavsiyesi",
    recommendations: [
      "Çamaşır ve bulaşık makinelerini gece tarifesi saatlerinde çalıştırmayı deneyin.",
      "Klimanın termostatını 1-2 derece yükseltmek fark edilmeden tasarruf sağlar.",
      "Standby modda bekleyen televizyon ve şarj cihazlarını fişten çekmeyi alışkanlık haline getirin.",
    ],
    estimatedSavingPercentage: 9,
    estimatedSavingAmount: 32.75,
  },
  {
    title: "Kota Dostu Öneriler",
    recommendations: [
      "Aylık kotanıza yaklaştığınızda en yüksek tüketimli cihazı önceliklendirip kısıtlayın.",
      "Fırın ve su ısıtıcısı gibi yüksek watt'lı cihazları aynı anda çalıştırmaktan kaçının.",
      "Güneş ışığından faydalanarak gündüz saatlerinde aydınlatma kullanımını azaltın.",
    ],
    estimatedSavingPercentage: 15,
    estimatedSavingAmount: 58.2,
  },
  {
    title: "Verimlilik İpuçları",
    recommendations: [
      "A+++ enerji sınıfı cihazlara geçiş uzun vadede faturalarınızı belirgin şekilde azaltır.",
      "Elektrikli süpürge ve fırın gibi cihazları arka arkaya değil, aralıklı kullanmayı tercih edin.",
      "Cihazlarınızın güvenli limit üzerinde çalışmadığından düzenli olarak emin olun.",
    ],
    estimatedSavingPercentage: 11,
    estimatedSavingAmount: 39.9,
  },
  {
    title: "Gece Tarifesi Fırsatı",
    recommendations: [
      "Elektrikli araç şarjını mutlaka gece tarifesi saatlerine planlayın.",
      "Su ısıtıcınızı gece saatlerinde çalıştırmak maliyeti düşürebilir.",
      "Yüksek tüketimli cihazları aynı anda değil, sırayla çalıştırın.",
    ],
    estimatedSavingPercentage: 13,
    estimatedSavingAmount: 41.3,
  },
  {
    title: "Bekleme Modu Uyarısı",
    recommendations: [
      "Televizyon, konsol ve şarj cihazlarının bekleme modu yıllık faturaya sessizce eklenir.",
      "Akıllı priz kullanarak bekleme modundaki cihazları tek tuşla tamamen kapatabilirsiniz.",
      "Kullanılmayan odalardaki elektronik cihazları fişten çekmeyi alışkanlık yapın.",
    ],
    estimatedSavingPercentage: 8,
    estimatedSavingAmount: 27.4,
  },
  {
    title: "Isıtma ve Soğutma Dengesi",
    recommendations: [
      "Klimayı 22-24°C aralığında tutmak hem konforu korur hem tüketimi azaltır.",
      "Kapı ve pencerelerdeki hava kaçaklarını gidermek ısıtma/soğutma yükünü düşürür.",
      "Perdeleri gün içinde kapalı tutmak klimanın daha az çalışmasını sağlar.",
    ],
    estimatedSavingPercentage: 14,
    estimatedSavingAmount: 49.9,
  },
  {
    title: "Güvenli Limit Kontrolü",
    recommendations: [
      "Cihazlarınızın güvenli watt limitine yakın çalışıp çalışmadığını düzenli kontrol edin.",
      "Aynı anda çok sayıda yüksek watt'lı cihaz çalıştırmak hem riski hem faturayı artırır.",
      "Eski cihazları enerji verimliliği yüksek modellerle değiştirmeyi planlayın.",
    ],
    estimatedSavingPercentage: 10,
    estimatedSavingAmount: 35.6,
  },
  {
    title: "Aydınlatma Tasarrufu",
    recommendations: [
      "LED ampullere geçiş, aydınlatma maliyetini uzun vadede belirgin şekilde azaltır.",
      "Gün ışığından maksimum faydalanmak için gündüz yapay aydınlatmayı azaltın.",
      "Kullanılmayan odalarda ışıkları kapatmayı otomatik sensörlerle kolaylaştırabilirsiniz.",
    ],
    estimatedSavingPercentage: 7,
    estimatedSavingAmount: 22.8,
  },
];

// "current" verilirse, ayni oneriyi arka arkaya gostermemek icin
// (yenile butonuna basildiginda fark edilir bir degisiklik olsun diye)
// listeden farkli bir tanesi secilmeye calisilir.
function getRandomFallback(current) {
  const candidates = FALLBACK_RECOMMENDATIONS.filter(
    (item) => item !== current
  );

  const pool = candidates.length > 0 ? candidates : FALLBACK_RECOMMENDATIONS;
  const randomIndex = Math.floor(Math.random() * pool.length);

  return pool[randomIndex];
}

// On yuz icin gercek ev verisinden birden fazla farkli icgoru turetiyoruz -
// boylece her zaman "en yuksek tuketen ev" gibi tek/sabit bir bilgiyle
// karsilasilmiyor, evlerle ilgili farklı gercek bilgiler donuyor.
function buildFrontInsights(homes) {
  const totalConsumption = homes.reduce(
    (sum, home) => sum + Number(home.consumption || 0),
    0
  );

  const totalBill = homes.reduce(
    (sum, home) => sum + Number(home.bill || 0),
    0
  );

  const highestConsumptionHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(secondHome.consumption) - Number(firstHome.consumption)
  )[0];

  const highestQuotaHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(secondHome.quotaPercentage) -
      Number(firstHome.quotaPercentage)
  )[0];

  const mostEfficientHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(firstHome.consumption) - Number(secondHome.consumption)
  )[0];

  const riskyHomes = homes.filter(
    (home) => Number(home.quotaPercentage || 0) >= 90
  );

  const insights = [
    {
      title: highestConsumptionHome.name,
      description: `${highestConsumptionHome.name}, şu anda en yüksek enerji tüketimine sahip eviniz.`,
      value: `${Number(
        highestConsumptionHome.consumption || 0
      ).toFixed(1)} kWh`,
    },
    {
      title: highestQuotaHome.name,
      description: `${highestQuotaHome.name}, aylık kota kullanımı en yüksek eviniz.`,
      value: `%${Number(
        highestQuotaHome.quotaPercentage || 0
      ).toFixed(0)} kota kullanımı`,
    },
    {
      title: mostEfficientHome.name,
      description: `${mostEfficientHome.name}, en verimli, en az tüketen eviniz.`,
      value: `${Number(mostEfficientHome.consumption || 0).toFixed(
        1
      )} kWh`,
    },
    {
      title: `${homes.length} Ev`,
      description: "Tüm evlerinizin toplam anlık tüketimi.",
      value: `${totalConsumption.toFixed(1)} kWh`,
    },
    {
      title: "Toplam Fatura",
      description: "Tüm evlerinizin bu ayki tahmini toplam faturası.",
      value: `₺${totalBill.toFixed(2)}`,
    },
  ];

  if (riskyHomes.length > 0) {
    const riskyHomeNames = riskyHomes
      .map((home) => home.name)
      .join(", ");

    insights.push({
      title: `${riskyHomes.length} Ev Riskli`,
      description: `Kota sınırına yaklaşan veya aşan eviniz: ${riskyHomeNames}.`,
      value: riskyHomeNames,
    });
  }

  return insights;
}

const FRONT_INSIGHT_INTERVAL_MS = 16000;

function AIRecommendation({ homes, homieRef, onOpenHint, isPointing }) {
  const [isFlipped, setIsFlipped] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [chatQuestion, setChatQuestion] = useState("");
  const [chatAnswer, setChatAnswer] = useState(null);
  const [isAsking, setIsAsking] = useState(false);
  const [chatError, setChatError] = useState(null);
  const [isTyping, setIsTyping] = useState(false);
  const [fallbackRecommendation, setFallbackRecommendation] = useState(
    () => getRandomFallback()
  );
  const [insightIndex, setInsightIndex] = useState(0);

  // On yuzdeki icgoru belirli araliklarla degisir, boylece karta her
  // bakildiginda ayni bilgiyle karsilasilmaz.
  useEffect(() => {
    const intervalId = setInterval(() => {
      setInsightIndex((previousIndex) => previousIndex + 1);
    }, FRONT_INSIGHT_INTERVAL_MS);

    return () => clearInterval(intervalId);
  }, []);

  if (!homes || homes.length === 0) {
    return null;
  }

  const frontInsights = buildFrontInsights(homes);
  const currentInsight =
    frontInsights[insightIndex % frontInsights.length];

  const highestQuotaHome = [...homes].sort(
    (firstHome, secondHome) =>
      Number(secondHome.quotaPercentage) -
      Number(firstHome.quotaPercentage)
  )[0];

  // Kullanici "Homie'ye Sor" kutusuna yazi yazarken Homie'nin gozleri
  // sasirmis bir sekilde, yazdigi son harfe dogru (yani metin
  // uzadikca saga dogru) bakar - sadece yazi yazilirken (input odakta)
  // aktif olan kucuk, eglenceli bir detay.
  const typingLookX = Math.min(1, chatQuestion.length / 40);

  const handleFlip = () => {
    setIsFlipped((previousValue) => !previousValue);
  };

  // Onceleri bu buton her tiklamada Core'daki Gemini destekli
  // /recommendation ucundan gercek bir oneri cekiyordu - bu da
  // gunluk Gemini kotasini gereksiz yere tuketiyordu. Artik Gemini
  // sadece kullanici "Homie'ye Sor" ile actikca soru
  // sordugunda cagriliyor; bu kart her zaman hazirlanmis, zengin
  // oneri havuzundan (yukarida FALLBACK_RECOMMENDATIONS) besleniyor.
  const handleRefreshClick = (event) => {
    event.stopPropagation();
    setFallbackRecommendation((current) => getRandomFallback(current));
  };

  const handleHomieClick = (event) => {
    event.stopPropagation();
    setIsChatOpen((previousValue) => !previousValue);
  };

  const handleCloseMessage = (event) => {
    event.stopPropagation();
    setIsChatOpen(false);
  };

  const handleChatSubmit = async (event) => {
    event.preventDefault();
    event.stopPropagation();

    const trimmedQuestion = chatQuestion.trim();

    if (!trimmedQuestion || !highestQuotaHome) {
      return;
    }

    setIsAsking(true);
    setChatError(null);

    try {
      const data = await askAssistant(
        highestQuotaHome.id,
        trimmedQuestion
      );

      setChatAnswer(data.answer);
      setChatQuestion("");
    } catch (error) {
      console.error("Homie'den cevap alınamadı:", error);
      setChatError(
        "Homie şu anda cevap veremedi. Lütfen birazdan tekrar dene."
      );
    } finally {
      setIsAsking(false);
    }
  };

  return (
    <section className="ai-recommendation-wrapper">
      <InfoHint
        id="ai-recommendation"
        text="Bu kutu, evlerinizle ilgili öne çıkan bir bilgiyi gösterir (en çok tüketen ev, en verimli ev, toplam fatura gibi) ve birkaç saniyede bir değişir. Tıklayınca arkasında hazır tasarruf önerileri var, 'Homie'ye Sor' ile de bana özel bir soru sorabilirsin."
        onOpen={onOpenHint}
      />

      <button
        type="button"
        className={`ai-flip-card ${
          isFlipped ? "is-flipped" : ""
        }`}
        onClick={handleFlip}
        aria-label="Homie'nin öneri kartını çevir"
      >
        <div className="ai-flip-card-inner">
          <div className="ai-flip-card-front ai-flip-card-face">
            <div className="ai-card-top">
              <span className="ai-card-badge">
                <span aria-hidden="true">✨</span>
                Homie İçgörüsü
              </span>
            </div>

            <div className="ai-card-content ai-speech-bubble">
              <h3>{currentInsight.title}</h3>

              <p>{currentInsight.description}</p>

              <strong>{currentInsight.value}</strong>
            </div>

            <div className="ai-card-footer">
              <span>Detaylı öneri için tıkla</span>
              <span
                className="rotate-icon"
                aria-hidden="true"
              >
                ↻
              </span>
            </div>
          </div>

          <div className="ai-flip-card-face ai-flip-card-back">
            <div className="ai-card-top">
              <span className="ai-card-badge">
                Homie'nin Önerisi
              </span>

              <button
                type="button"
                className="ai-refresh-button"
                onClick={handleRefreshClick}
                aria-label="Başka bir öneri göster"
                title="Başka bir öneri göster"
              >
                <span aria-hidden="true">↻</span>
              </button>
            </div>

            <div className="ai-card-content ai-speech-bubble">
              <h3>{fallbackRecommendation.title}</h3>

              <ul>
                {fallbackRecommendation.recommendations
                  .slice(0, 2)
                  .map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
              </ul>

              <strong>
                Tahmini tasarruf: ₺
                {Number(
                  fallbackRecommendation.estimatedSavingAmount || 0
                ).toFixed(2)}{" "}
                (%
                {Number(
                  fallbackRecommendation.estimatedSavingPercentage || 0
                ).toFixed(0)}
                )
              </strong>
            </div>

            <div className="ai-card-footer">
              <span>Ön yüze dön</span>
              <span
                className="rotate-icon"
                aria-hidden="true"
              >
                ↻
              </span>
            </div>
          </div>
        </div>
      </button>

      {/* Ust bilgi kutusu, altta oturan Homie'nin agzindan cikan bir
          konusma baloncugu gibi gorunsun diye kucuk bir kuyruk -
          flip animasyonunun disinda, sabit durur. */}
      <div className="ai-speech-tail" aria-hidden="true" />

      {/* "Homie'ye Sor" butonunun tam ustune, ayaklari butonun ust
          kenariyla tam ayni hizada (deyecek sekilde) oturan Homie -
          saf dekoratif/interaktif bir detay. Butonla ayni grup icinde
          mutlak konumlandirildigi icin bosluk/hizalama sorunu olmaz. */}
      <div className="homie-button-group">
        <div className="homie-sitting" aria-hidden="true" ref={homieRef}>
          <HomieMascot
            size={80}
            legLength={22}
            swingFeet
            gripBox
            handShiftPx={20}
            mood={
              isTyping
                ? "surprised"
                : isFlipped
                ? "thoughtful"
                : "normal"
            }
            lookX={isTyping ? typingLookX : 0.5}
            rightArmRaised={isPointing}
            trackMouse
          />
        </div>

        <button
          type="button"
          className="gemini-button"
          onClick={handleHomieClick}
        >
          <span aria-hidden="true">💬</span>
          Homie'ye Sor
        </button>
      </div>

      {isChatOpen && (
        <div className="gemini-message gemini-chat-panel">
          <div className="gemini-chat-header">
            <span className="gemini-chat-header-title">
              Homie
              {highestQuotaHome ? ` · ${highestQuotaHome.name}` : ""}
            </span>

            <button
              type="button"
              onClick={handleCloseMessage}
              aria-label="Sohbeti kapat"
            >
              ×
            </button>
          </div>

          <form
            className="gemini-chat-form"
            onSubmit={handleChatSubmit}
          >
            <input
              type="text"
              value={chatQuestion}
              onChange={(event) =>
                setChatQuestion(event.target.value)
              }
              onClick={(event) => event.stopPropagation()}
              onFocus={() => setIsTyping(true)}
              onBlur={() => setIsTyping(false)}
              placeholder="Örn: Bu ay faturam neden yüksek?"
              maxLength={500}
              disabled={isAsking}
            />

            <button
              type="submit"
              disabled={isAsking || !chatQuestion.trim()}
            >
              {isAsking ? "..." : "Sor"}
            </button>
          </form>

          {isAsking && (
            <p className="gemini-chat-status">
              Homie cevap hazırlıyor...
            </p>
          )}

          {!isAsking && chatError && (
            <p className="gemini-chat-status gemini-chat-error">
              {chatError}
            </p>
          )}

          {!isAsking && !chatError && chatAnswer && (
            <p className="gemini-chat-answer">{chatAnswer}</p>
          )}
        </div>
      )}
    </section>
  );
}

export default AIRecommendation;
