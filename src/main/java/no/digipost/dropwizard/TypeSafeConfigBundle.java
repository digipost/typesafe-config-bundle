package no.digipost.dropwizard;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;

public class TypeSafeConfigBundle<C> implements ConfiguredBundle<C> {

    private final boolean includeEnvironmentInConfig;

    public TypeSafeConfigBundle() {
        this(false);
    }

    public TypeSafeConfigBundle(boolean includeEnvironmentInConfig) {
        this.includeEnvironmentInConfig = includeEnvironmentInConfig;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

        bootstrap.setConfigurationFactoryFactory(
                (c, v, m, p) -> new TypeSafeConfigFactory<>(c, v, m, p, includeEnvironmentInConfig));
        bootstrap.setConfigurationSourceProvider(new FileOrResourceConfigurationSourceProvider());
    }

}
