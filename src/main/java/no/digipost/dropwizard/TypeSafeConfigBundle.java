package no.digipost.dropwizard;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class TypeSafeConfigBundle implements Bundle {

    private final boolean includeEnvironmentInConfig;

    public TypeSafeConfigBundle() {
        this(false);
    }

    public TypeSafeConfigBundle(final boolean includeEnvironmentInConfig) {
        this.includeEnvironmentInConfig = includeEnvironmentInConfig;
    }

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {

        bootstrap.setConfigurationFactoryFactory(
                (c, v, m, p) -> new TypeSafeConfigFactory<>(c, v, m, p, includeEnvironmentInConfig));
        bootstrap.setConfigurationSourceProvider(new FileOrResourceConfigurationSourceProvider());
    }

    @Override
    public void run(final Environment environment) {

    }
}
