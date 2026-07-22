package com.powerpulse.core.appliance;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplianceRepository
        extends JpaRepository<Appliance, UUID> {

    @Override
    @EntityGraph(attributePaths = "home")
    Optional<Appliance> findById(UUID id);

    List<Appliance> findAllByHomeId(UUID homeId);
}