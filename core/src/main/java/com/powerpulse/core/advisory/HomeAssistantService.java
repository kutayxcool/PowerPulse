package com.powerpulse.core.advisory;

import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeNotFoundException;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.telemetry.HomeLiveState;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class HomeAssistantService {

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;
    private final GeminiChatService chatService;

    public HomeAssistantService(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore,
            GeminiChatService chatService
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
        this.chatService = chatService;
    }

    public AskQuestionResponse ask(UUID homeId, UUID ownerId, String question) {
        Home home = homeRepository
                .findWithAppliancesByIdAndOwnerId(homeId, ownerId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        HomeLiveState homeState = liveStateStore
                .findHome(homeId)
                .orElse(null);

        BigDecimal consumption = homeState == null
                ? home.getTotalConsumptionKwh()
                : homeState.totalConsumptionKwh();

        BigDecimal bill = homeState == null
                ? home.getCurrentBillAmount()
                : homeState.currentBillAmount();

        String applianceList = home.getAppliances().stream()
                .map(appliance -> appliance.getName()
                        + " (güvenli limit: "
                        + appliance.getSafeLimitWatt()
                        + " W)")
                .reduce((first, second) -> first + ", " + second)
                .orElse("Kayıtlı cihaz yok");

        String prompt = buildPrompt(
                home, consumption, bill, applianceList, question
        );

        String answer = chatService.ask(prompt);

        return new AskQuestionResponse(answer);
    }

    private String buildPrompt(
            Home home,
            BigDecimal consumption,
            BigDecimal bill,
            String applianceList,
            String question
    ) {
        return """
                Sen PowerPulse enerji takip uygulamasının yardımcı
                asistanısın. Kullanıcıya kısa, samimi ve doğrudan
                Türkçe cevap ver. Cevabın en fazla 3-4 cümle olsun,
                gereksiz uzatma, markdown kullanma.

                Ev: %s
                Toplam tüketim: %.2f kWh
                Aylık kota: %.2f kWh
                Güncel fatura: %.2f TL
                Cihazlar: %s

                Kullanıcının sorusu: %s
                """
                .formatted(
                        home.getName(),
                        consumption.doubleValue(),
                        home.getBudgetQuotaKwh().doubleValue(),
                        bill.doubleValue(),
                        applianceList,
                        question
                );
    }
}
