CREATE TABLE billing_ledger (
    id BIGSERIAL PRIMARY KEY,
    home_id UUID NOT NULL,
    appliance_id UUID NOT NULL,
    telemetry_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,

    added_consumption_kwh NUMERIC(18, 10) NOT NULL,
    normal_charged_kwh NUMERIC(18, 10) NOT NULL,
    penalty_charged_kwh NUMERIC(18, 10) NOT NULL,

    incremental_cost NUMERIC(18, 8) NOT NULL,
    penalty_tier INTEGER NOT NULL,
    penalty_multiplier NUMERIC(6, 2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_billing_ledger_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_billing_ledger_appliance
        FOREIGN KEY (appliance_id)
        REFERENCES appliances (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_billing_ledger_telemetry
        UNIQUE (
            home_id,
            appliance_id,
            telemetry_timestamp
        ),

    CONSTRAINT chk_billing_added_consumption_non_negative
        CHECK (added_consumption_kwh >= 0),

    CONSTRAINT chk_billing_normal_kwh_non_negative
        CHECK (normal_charged_kwh >= 0),

    CONSTRAINT chk_billing_penalty_kwh_non_negative
        CHECK (penalty_charged_kwh >= 0),

    CONSTRAINT chk_billing_cost_non_negative
        CHECK (incremental_cost >= 0),

    CONSTRAINT chk_billing_penalty_tier_non_negative
        CHECK (penalty_tier >= 0),

    CONSTRAINT chk_billing_multiplier_positive
        CHECK (penalty_multiplier > 0)
);

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    home_id UUID NOT NULL,
    appliance_id UUID,

    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,

    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,

    delivery_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_notification_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_appliance
        FOREIGN KEY (appliance_id)
        REFERENCES appliances (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_notification_type
        CHECK (
            notification_type IN (
                'QUOTA_WARNING_80',
                'QUOTA_REACHED_100',
                'PENALTY_TIER_CHANGED',
                'APPLIANCE_ANOMALY',
                'AI_RECOMMENDATION'
            )
        ),

    CONSTRAINT chk_notification_channel
        CHECK (
            channel IN (
                'SYSTEM',
                'EMAIL'
            )
        ),

    CONSTRAINT chk_notification_delivery_status
        CHECK (
            delivery_status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        )
);

CREATE INDEX idx_billing_ledger_home_time
    ON billing_ledger (
        home_id,
        telemetry_timestamp DESC
    );

CREATE INDEX idx_billing_ledger_appliance_time
    ON billing_ledger (
        appliance_id,
        telemetry_timestamp DESC
    );

CREATE INDEX idx_notification_home_created
    ON notification_logs (
        home_id,
        created_at DESC
    );

CREATE INDEX idx_notification_status
    ON notification_logs (
        delivery_status,
        created_at
    );