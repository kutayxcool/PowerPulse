package com.powerpulse.core.home;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.auth.User;
import com.powerpulse.core.home.dto.RegisterHomeRequest;
import com.powerpulse.core.home.dto.RegisteredApplianceResponse;
import com.powerpulse.core.home.dto.RegisteredHomeResponse;
import com.powerpulse.core.registration.HomeRegistrationEvent;
import com.powerpulse.core.registration.RegistrationApplianceEvent;
import com.powerpulse.core.registration.RegistrationPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HomeRegistrationService {

    private final HomePersistenceService persistenceService;
    private final RegistrationPublisher registrationPublisher;
    private final BigDecimal baseRatePerKwh;

    public HomeRegistrationService(
            HomePersistenceService persistenceService,
            RegistrationPublisher registrationPublisher,
            @Value("${powerpulse.billing.base-rate-per-kwh}")
            BigDecimal baseRatePerKwh
    ) {
        this.persistenceService = persistenceService;
        this.registrationPublisher = registrationPublisher;
        this.baseRatePerKwh = baseRatePerKwh;
    }

    public RegisteredHomeResponse register(RegisterHomeRequest request, User owner) {
        Home home = createHome(request, owner);

        Home savedHome = persistenceService.save(home);

        registrationPublisher.publish(toRegistrationEvent(savedHome));

        return toResponse(savedHome);
    }

    private Home createHome(RegisterHomeRequest request, User owner) {
        Home home = new Home(
                UUID.randomUUID(),
                request.name().trim(),
                request.contactEmail().trim().toLowerCase(Locale.ROOT),
                request.budgetQuotaKwh(),
                baseRatePerKwh,
                owner
        );

        request.appliances().forEach(requestAppliance -> {
            Appliance appliance = new Appliance(
                    UUID.randomUUID(),
                    requestAppliance.name().trim(),
                    requestAppliance.safeLimitWatt()
            );

            home.addAppliance(appliance);
        });

        return home;
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