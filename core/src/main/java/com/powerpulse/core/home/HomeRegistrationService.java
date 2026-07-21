package com.powerpulse.core.home;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.dto.RegisterHomeRequest;
import com.powerpulse.core.home.dto.RegisteredApplianceResponse;
import com.powerpulse.core.home.dto.RegisteredHomeResponse;
import com.powerpulse.core.registration.HomeRegistrationEvent;
import com.powerpulse.core.registration.RegistrationApplianceEvent;
import com.powerpulse.core.registration.RegistrationPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HomeRegistrationService {

    private final HomeRepository homeRepository;
    private final RegistrationPublisher registrationPublisher;
    private final BigDecimal baseRatePerKwh;

    public HomeRegistrationService(
            HomeRepository homeRepository,
            RegistrationPublisher registrationPublisher,
            @Value("${powerpulse.billing.base-rate-per-kwh}")
            BigDecimal baseRatePerKwh
    ) {
        this.homeRepository = homeRepository;
        this.registrationPublisher = registrationPublisher;
        this.baseRatePerKwh = baseRatePerKwh;
    }

    @Transactional
    public RegisteredHomeResponse register(RegisterHomeRequest request) {
        Home home = new Home(
                UUID.randomUUID(),
                request.name().trim(),
                request.contactEmail().trim().toLowerCase(Locale.ROOT),
                request.budgetQuotaKwh(),
                baseRatePerKwh
        );

        request.appliances().forEach(applianceRequest -> {
            Appliance appliance = new Appliance(
                    UUID.randomUUID(),
                    applianceRequest.name().trim(),
                    applianceRequest.safeLimitWatt()
            );

            home.addAppliance(appliance);
        });

        Home savedHome = homeRepository.saveAndFlush(home);

        registrationPublisher.publish(toRegistrationEvent(savedHome));

        return toResponse(savedHome);
    }

    private HomeRegistrationEvent toRegistrationEvent(Home home) {
        List<RegistrationApplianceEvent> appliances = home.getAppliances()
                .stream()
                .map(appliance -> new RegistrationApplianceEvent(
                        appliance.getId().toString(),
                        appliance.getName(),
                        appliance.getSafeLimitWatt().doubleValue()
                ))
                .toList();

        return new HomeRegistrationEvent(
                home.getId().toString(),
                home.getContactEmail(),
                home.getBudgetQuotaKwh().doubleValue(),
                appliances
        );
    }

    private RegisteredHomeResponse toResponse(Home home) {
        List<RegisteredApplianceResponse> appliances = home.getAppliances()
                .stream()
                .map(appliance -> new RegisteredApplianceResponse(
                        appliance.getId(),
                        appliance.getName(),
                        appliance.getSafeLimitWatt()
                ))
                .toList();

        return new RegisteredHomeResponse(
                home.getId(),
                home.getName(),
                home.getContactEmail(),
                home.getBudgetQuotaKwh(),
                home.getBaseRatePerKwh(),
                home.getTotalConsumptionKwh(),
                home.getCurrentBillAmount(),
                appliances,
                home.getCreatedAt()
        );
    }
}