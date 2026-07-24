package com.powerpulse.core.home;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeRepository extends JpaRepository<Home, UUID> {
    List<Home> findAllByOrderByCreatedAtDesc();
    @Override
    @EntityGraph(attributePaths = "appliances")
    Optional<Home> findById(UUID id);

    @EntityGraph(attributePaths = "appliances")
    List<Home> findAllByOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "appliances")
    Optional<Home> findWithAppliancesById(UUID id);

    // Sahiplige gore filtrelenmis sorgular - bir kullanici sadece
    // kendi evlerini listeleyebilir/gorebilir/degistirebilir.
    // homeId dogru ama owner farkliysa da (baska birinin evi tahmin
    // edilmeye calisilirsa) bilerek HomeNotFoundException ile ayni
    // sonuc (bos Optional) donulur - evin var olup olmadigi bile
    // yetkisiz kisiye sizdirilmaz.
    List<Home> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "appliances")
    Optional<Home> findByIdAndOwnerId(UUID id, UUID ownerId);

    @EntityGraph(attributePaths = "appliances")
    Optional<Home> findWithAppliancesByIdAndOwnerId(UUID id, UUID ownerId);
}