CREATE TABLE homes (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    budget_quota_kwh NUMERIC(12, 3) NOT NULL,
    base_rate_per_kwh NUMERIC(10, 4) NOT NULL DEFAULT 2.10,
    total_consumption_kwh NUMERIC(14, 4) NOT NULL DEFAULT 0,
    current_bill_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_home_budget_quota_positive
        CHECK (budget_quota_kwh > 0),

    CONSTRAINT chk_home_base_rate_positive
        CHECK (base_rate_per_kwh > 0),

    CONSTRAINT chk_home_consumption_non_negative
        CHECK (total_consumption_kwh >= 0),

    CONSTRAINT chk_home_bill_non_negative
        CHECK (current_bill_amount >= 0)
);

CREATE TABLE appliances (
    id UUID PRIMARY KEY,
    home_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    safe_limit_watt NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appliance_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_appliance_safe_limit_positive
        CHECK (safe_limit_watt > 0)
);

CREATE TABLE daily_consumption_snapshots (
    id BIGSERIAL PRIMARY KEY,
    home_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    consumption_kwh NUMERIC(14, 4) NOT NULL,
    bill_amount NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_snapshot_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_home_snapshot_date
        UNIQUE (home_id, snapshot_date),

    CONSTRAINT chk_snapshot_consumption_non_negative
        CHECK (consumption_kwh >= 0),

    CONSTRAINT chk_snapshot_bill_non_negative
        CHECK (bill_amount >= 0)
);

CREATE TABLE operational_events (
    id BIGSERIAL PRIMARY KEY,
    home_id UUID NOT NULL,
    appliance_id UUID,
    event_type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_event_appliance
        FOREIGN KEY (appliance_id)
        REFERENCES appliances (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_event_type
        CHECK (
            event_type IN (
                'QUOTA_WARNING_80',
                'QUOTA_REACHED_100',
                'PENALTY_TIER_CHANGED',
                'APPLIANCE_ANOMALY'
            )
        )
);

CREATE TABLE ai_recommendations (
    id BIGSERIAL PRIMARY KEY,
    home_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    recommendation_text TEXT NOT NULL,
    estimated_saving_percentage NUMERIC(5, 2),
    estimated_saving_amount NUMERIC(14, 2),
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_recommendation_home
        FOREIGN KEY (home_id)
        REFERENCES homes (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_saving_percentage
        CHECK (
            estimated_saving_percentage IS NULL
            OR (
                estimated_saving_percentage >= 0
                AND estimated_saving_percentage <= 100
            )
        ),

    CONSTRAINT chk_saving_amount_non_negative
        CHECK (
            estimated_saving_amount IS NULL
            OR estimated_saving_amount >= 0
        )
);

CREATE INDEX idx_appliances_home_id
    ON appliances (home_id);

CREATE INDEX idx_snapshots_home_date
    ON daily_consumption_snapshots (home_id, snapshot_date);

CREATE INDEX idx_events_home_time
    ON operational_events (home_id, event_time);

CREATE INDEX idx_recommendations_home_generated
    ON ai_recommendations (home_id, generated_at);