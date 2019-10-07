package no.digipost.dropwizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import no.digipost.dropwizard.configuration.ConfigurationSourceProviderWithFallback;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;

import java.io.IOException;

import static no.digipost.dropwizard.TypeSafeConfigFactory.ENV_KEY;
import static no.digipost.dropwizard.TypeSafeConfigFactory.SECRET_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigurationTest {

    final Validator validator;
    final ObjectMapper objectMapper;
    final ConfigurationFactory<TestConfig> configFactory;

    public ConfigurationTest() {
        validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .buildValidatorFactory().getValidator();
        objectMapper = Jackson.newObjectMapper();
        configFactory = new TypeSafeConfigFactory<>(TestConfig.class, validator, objectMapper, "dw", false);
    }

    @BeforeEach
    public void setUp() {
        System.setProperty("driverClassSystemProperty", "driverClassFromSystemProperty");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(ENV_KEY);
        System.clearProperty(SECRET_KEY);
        System.clearProperty("driverClassSystemProperty");
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

    @Test
    public void should_interpolate_variables() throws IOException, ConfigurationException {
        setEnv("test");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("default variable value"));
    }

    @Test
    public void should_interpolate_variables_with_overrides() throws IOException, ConfigurationException {
        setEnv("local");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("overridden variable value"));
    }

    @Test
    public void should_resolve_system_properties() throws IOException, ConfigurationException {
        setEnv("test2");
        final TestConfig config =
                configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("driverClassFromSystemProperty"));
    }

    private void setEnv(final String env) {
        System.setProperty(ENV_KEY, env);
    }

    private final ConfigurationSourceProvider configSourceProvider =
            new ConfigurationSourceProviderWithFallback(new FileConfigurationSourceProvider(), new ResourceConfigurationSourceProvider());
}
