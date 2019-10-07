package no.digipost.dropwizard;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import no.digipost.dropwizard.configuration.ConfigurationSourceProviderWithFallback;

public class TypeSafeConfigBundle<C> implements ConfiguredBundle<C> {

    private final boolean includeEnvironmentInConfig;
    private final ConfigurationSourceProvider configurationSourceProvider;

    public TypeSafeConfigBundle() {
        this(new ConfigurationSourceProviderWithFallback(new FileConfigurationSourceProvider(), new ResourceConfigurationSourceProvider()));
    }

    public TypeSafeConfigBundle(ConfigurationSourceProvider configurationSourceProvider) {
        this(configurationSourceProvider, false);
    }

    public TypeSafeConfigBundle(ConfigurationSourceProvider configurationSourceProvider, boolean includeEnvironmentInConfig) {
        this.includeEnvironmentInConfig = includeEnvironmentInConfig;
        this.configurationSourceProvider = configurationSourceProvider;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationFactoryFactory(
                (c, v, m, p) -> new TypeSafeConfigFactory<>(c, v, m, p, includeEnvironmentInConfig));
        bootstrap.setConfigurationSourceProvider(configurationSourceProvider);
    }

}
