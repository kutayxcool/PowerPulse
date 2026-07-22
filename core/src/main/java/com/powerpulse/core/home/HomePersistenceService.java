package com.powerpulse.core.home;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomePersistenceService {

    private final HomeRepository homeRepository;

    public HomePersistenceService(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    @Transactional
    public Home save(Home home) {
        return homeRepository.saveAndFlush(home);
    }
}