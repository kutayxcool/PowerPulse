package com.powerpulse.core.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalEventRepository
        extends JpaRepository<OperationalEvent, Long> {
}