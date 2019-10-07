package no.digipost.dropwizard.configuration;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;

import java.io.IOException;
import java.io.InputStream;

/**
 * This allows for trying to resolve configuration source from several types of locations.
 * If the first provider throws an {@link IOException}, a second attempt is done with a
 * fallback provider. The typical composition of providers is to first try using a
 * {@link FileConfigurationSourceProvider}, and then a {@link ResourceConfigurationSourceProvider}.
 *
 * <pre>
 * {@code
 * new ConfigurationSourceProviderWithFallback(
 *     new FileConfigurationSourceProvider(),
 *     new ResourceConfigurationSourceProvider()
 * )
 * }
 * </pre>
 *
 */
public class ConfigurationSourceProviderWithFallback implements ConfigurationSourceProvider {

    private final ConfigurationSourceProvider main;
    private final ConfigurationSourceProvider fallback;

    public ConfigurationSourceProviderWithFallback(ConfigurationSourceProvider main, ConfigurationSourceProvider fallback) {
        this.main = main;
        this.fallback = fallback;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ConfigurationSourceNotFoundException if both providers fails with {@link IOException} trying to resolve
     *                                              a configuration source from the given {@code path}. The thrown exception
     *                                              will contain the exception from the main provider as cause, and the exception
     *                                              from the second provider as suppressed.
     */
    @Override
    public final InputStream open(String path) throws IOException {
        try {
            return main.open(path);
        } catch (IOException exceptionOnMainProvider) {
            try {
                return fallback.open(path);
            } catch (IOException exceptionOnFallbackProvider) {
                ConfigurationSourceNotFoundException e = new ConfigurationSourceNotFoundException(
                        "No " + ConfigurationSourceProvider.class.getSimpleName() + " were able to resolve configuration from " +
                        path + ", because " + exceptionOnMainProvider.getMessage() + ", and " + exceptionOnFallbackProvider.getMessage(), exceptionOnMainProvider);
                e.addSuppressed(exceptionOnFallbackProvider);
                throw e;
            } catch (RuntimeException e) {
                e.addSuppressed(exceptionOnMainProvider);
                throw e;
            }
        }
    }
}
