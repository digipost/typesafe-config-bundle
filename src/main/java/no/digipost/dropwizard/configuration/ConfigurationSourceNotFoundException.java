package no.digipost.dropwizard.configuration;

import java.io.IOException;

public class ConfigurationSourceNotFoundException extends IOException {

    public ConfigurationSourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
