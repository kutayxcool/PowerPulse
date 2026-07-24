package com.powerpulse.core.ignite;

import jakarta.annotation.PreDestroy;
import org.apache.ignite.IgniteException;
import org.apache.ignite.client.ClientConnectionException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.Ignition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IgniteClientManager {

    private final String igniteAddress;
    private IgniteClient client;

    public IgniteClientManager(
            @Value("${powerpulse.ignite.address}") String igniteAddress
    ) {
        this.igniteAddress = igniteAddress;
    }

    public synchronized IgniteClient getClient() {
        if (client == null) {
            client = connect();
        }

        return client;
    }

    public synchronized void invalidate() {
        if (client != null) {
            try {
                client.close();
            } finally {
                client = null;
            }
        }
    }

    private IgniteClient connect() {
        try {
            ClientConfiguration configuration =
                    new ClientConfiguration()
                            .setAddresses(igniteAddress);

            return Ignition.startClient(configuration);

        } catch (ClientConnectionException | IgniteException exception) {
            throw new IgniteUnavailableException(
                    "Apache Ignite bağlantısı kurulamadı: " + igniteAddress,
                    exception
            );
        }
    }

    @PreDestroy
    public synchronized void close() {
        invalidate();
    }
}