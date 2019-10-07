package no.digipost.dropwizard;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import no.digipost.dropwizard.configuration.ConfigurationSourceProviderWithFallback;

public class TypeSafeConfigBundle<C> implements ConfiguredBundle<C> {

    private final ConfigurationSourceProvider configurationSourceProvider;

    public TypeSafeConfigBundle() {
        this(new ConfigurationSourceProviderWithFallback(new FileConfigurationSourceProvider(), new ResourceConfigurationSourceProvider()));
    }

    public TypeSafeConfigBundle(ConfigurationSourceProvider configurationSourceProvider) {
        this.configurationSourceProvider = configurationSourceProvider;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationFactoryFactory(TypeSafeConfigFactory::new);
        bootstrap.setConfigurationSourceProvider(configurationSourceProvider);
    }

}
