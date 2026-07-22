package com.powerpulse.core.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingLedgerRepository
        extends JpaRepository<BillingLedger, Long> {
}