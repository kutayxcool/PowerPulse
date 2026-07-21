package com.powerpulse.core.appliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplianceRepository extends JpaRepository<Appliance, UUID> {

    List<Appliance> findAllByHomeId(UUID homeId);
}