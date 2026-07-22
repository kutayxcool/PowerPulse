package com.powerpulse.core.billing;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class BillingLedgerService {

    private final BillingLedgerRepository ledgerRepository;

    public BillingLedgerService(
            BillingLedgerRepository ledgerRepository
    ) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public void record(
            Home home,
            Appliance appliance,
            OffsetDateTime telemetryTimestamp,
            BillingCalculation calculation
    ) {
        ledgerRepository.save(
                new BillingLedger(
                        home,
                        appliance,
                        telemetryTimestamp,
                        calculation
                )
        );
    }
}