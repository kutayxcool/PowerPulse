package com.powerpulse.core.home;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeRepository extends JpaRepository<Home, UUID> {

    @Override
    @EntityGraph(attributePaths = "appliances")
    Optional<Home> findById(UUID id);

    @EntityGraph(attributePaths = "appliances")
    List<Home> findAllByOrderByCreatedAtAsc();
}