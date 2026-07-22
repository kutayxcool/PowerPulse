package com.powerpulse.core.ignite;

import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IgniteCacheService {

    private final IgniteClientManager clientManager;

    public IgniteCacheService(IgniteClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void putJson(String cacheName, String key, String json) {
        try {
            ClientCache<String, String> cache = clientManager
                    .getClient()
                    .getOrCreateCache(cacheName);

            cache.put(key, json);
        } catch (ClientException exception) {
            clientManager.invalidate();

            throw new IgniteUnavailableException(
                    "Ignite cache güncellenemedi: " + cacheName,
                    exception
            );
        }
    }

    public Optional<String> getJson(String cacheName, String key) {
        try {
            ClientCache<String, String> cache = clientManager
                    .getClient()
                    .getOrCreateCache(cacheName);

            return Optional.ofNullable(cache.get(key));
        } catch (ClientException exception) {
            clientManager.invalidate();

            throw new IgniteUnavailableException(
                    "Ignite cache okunamadı: " + cacheName,
                    exception
            );
        }
    }

    public void putCounter(String key, int value) {
        try {
            ClientCache<String, Integer> cache = clientManager
                    .getClient()
                    .getOrCreateCache(IgniteCacheNames.BREACH_COUNTERS);

            cache.put(key, value);
        } catch (ClientException exception) {
            clientManager.invalidate();

            throw new IgniteUnavailableException(
                    "Ignite anomali sayacı güncellenemedi.",
                    exception
            );
        }
    }

    public int getCounter(String key) {
        try {
            ClientCache<String, Integer> cache = clientManager
                    .getClient()
                    .getOrCreateCache(IgniteCacheNames.BREACH_COUNTERS);

            Integer value = cache.get(key);
            return value == null ? 0 : value;
        } catch (ClientException exception) {
            clientManager.invalidate();

            throw new IgniteUnavailableException(
                    "Ignite anomali sayacı okunamadı.",
                    exception
            );
        }
    }

    public void removeCounter(String key) {
        try {
            ClientCache<String, Integer> cache = clientManager
                    .getClient()
                    .getOrCreateCache(IgniteCacheNames.BREACH_COUNTERS);

            cache.remove(key);
        } catch (ClientException exception) {
            clientManager.invalidate();

            throw new IgniteUnavailableException(
                    "Ignite anomali sayacı sıfırlanamadı.",
                    exception
            );
        }
    }
}