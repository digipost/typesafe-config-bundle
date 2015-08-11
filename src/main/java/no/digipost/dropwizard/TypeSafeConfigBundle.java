package no.digipost.dropwizard;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class TypeSafeConfigBundle implements Bundle {

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        bootstrap.setConfigurationFactoryFactory(TypeSafeConfigFactory::new);
        bootstrap.setConfigurationSourceProvider(new FileOrResourceConfigurationSourceProvider());
    }


    @Override
    public void run(final Environment environment) {

    }
}
