package no.digipost.dropwizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.validation.valuehandling.OptionalValidatedValueUnwrapper;
import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;

import static no.digipost.dropwizard.TypeSafeConfigFactory.ENV_KEY;
import static no.digipost.dropwizard.TypeSafeConfigFactory.SECRET_KEY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigurationTest {

    final Validator validator;
    final ObjectMapper objectMapper;
    final ConfigurationFactory<TestConfig> configFactory;

    public ConfigurationTest() {
        validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .addValidatedValueHandler(new OptionalValidatedValueUnwrapper())
                .buildValidatorFactory().getValidator();
        objectMapper = Jackson.newObjectMapper();
        configFactory = new TypeSafeConfigFactory<>(TestConfig.class, validator, objectMapper, "dw");
    }

    @After
    public void clearEnvironmentProperties() {
        System.clearProperty(ENV_KEY);
        System.clearProperty(SECRET_KEY);
    }

    @Test
    public void should_load_local_config_without_errors() throws IOException, ConfigurationException {
        setEnv("local");
        configFactory.build(configSourceProvider, "test-config.yml");
    }

    @Test
    public void should_load_test_config_without_errors() throws IOException, ConfigurationException {
        setEnv("test");
        configFactory.build(configSourceProvider, "test-config.yml");
    }

    @Test
    public void should_use_default_values_when_environment_has_no_specific_value() throws IOException, ConfigurationException {
        setEnv("local");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("org.postgresql.Driver"));
    }

    @Test
    public void should_override_defaults_with_environment_specific_values() throws IOException, ConfigurationException {
        setEnv("test");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("overridden"));
    }

    @Test
    public void should_allow_empty_default_with_environment_specific_value() throws IOException, ConfigurationException {
        setEnv("test");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getUrl(), is("test_url"));
    }

    @Test
    public void should_load_secret_config_from_file() throws IOException, ConfigurationException {
        setEnv("local");
        System.setProperty(TypeSafeConfigFactory.SECRET_KEY, getClass().getResource("/test-secret.yml").getFile());
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("secret_password"));
    }

    private void setEnv(final String env) {
        System.setProperty(ENV_KEY, env);
    }

    private ConfigurationSourceProvider configSourceProvider = new FileOrResourceConfigurationSourceProvider();
}
