package no.digipost.dropwizard;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import no.digipost.dropwizard.configuration.ConfigurationSourceProviderWithFallback;

/**
 * Dropwizard {@link ConfiguredBundle} to support configuration with
 * https://github.com/lightbend/config
 *
 * @param <C> The type of config object
 */
public class TypeSafeConfiguredBundle<C> implements ConfiguredBundle<C> {

    private final ConfigurationSourceProvider configurationSourceProvider;

    public TypeSafeConfiguredBundle() {
        this(new ConfigurationSourceProviderWithFallback(new FileConfigurationSourceProvider(), new ResourceConfigurationSourceProvider()));
    }

    public TypeSafeConfiguredBundle(ConfigurationSourceProvider configurationSourceProvider) {
        this.configurationSourceProvider = configurationSourceProvider;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationFactoryFactory(TypeSafeConfigurationFactory::new);
        bootstrap.setConfigurationSourceProvider(configurationSourceProvider);
    }

}
