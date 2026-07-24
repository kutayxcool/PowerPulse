package com.powerpulse.core.advisory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeNotFoundException;
import com.powerpulse.core.home.HomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AiRecommendationPersistenceService {

    private final HomeRepository homeRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public AiRecommendationPersistenceService(
            HomeRepository homeRepository,
            AiRecommendationRepository recommendationRepository,
            ObjectMapper objectMapper
    ) {
        this.homeRepository = homeRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void save(
            UUID homeId,
            AdvisoryResult result,
            OffsetDateTime generatedAt
    ) {
        Home home = homeRepository
                .findById(homeId)
                .orElseThrow(() ->
                        new HomeNotFoundException(homeId)
                );

        recommendationRepository.save(
                new AiRecommendation(
                        home,
                        result.title(),
                        serializeRecommendations(result),
                        BigDecimal.valueOf(
                                result.estimatedSavingPercentage()
                        ),
                        BigDecimal.valueOf(
                                result.estimatedSavingAmount()
                        ),
                        generatedAt
                )
        );
    }

    private String serializeRecommendations(
            AdvisoryResult result
    ) {
        try {
            return objectMapper.writeValueAsString(
                    result.recommendations()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "AI tavsiyeleri JSON formatına dönüştürülemedi.",
                    exception
            );
        }
    }
}