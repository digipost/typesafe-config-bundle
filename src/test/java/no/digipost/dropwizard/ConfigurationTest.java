/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import static no.digipost.dropwizard.TypeSafeConfigurationFactory.ENV_KEY;
import static no.digipost.dropwizard.TypeSafeConfigurationFactory.SECRET_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConfigurationTest {

    private static final Validator validator = Validation.byProvider(HibernateValidator.class).configure()
                                                  .buildValidatorFactory().getValidator();
    private static final ObjectMapper objectMapper = Jackson.newObjectMapper();

    private final ConfigurationFactory<TestConfig> configFactory =
        new TypeSafeConfigurationFactory<>(TestConfig.class, validator, objectMapper, "dw");
    private final ConfigurationSourceProvider configSourceProvider =
        new ConfigurationSourceProviderWithFallback(new FileConfigurationSourceProvider(), new ResourceConfigurationSourceProvider());

    @BeforeEach
    void setUp() {
        System.setProperty("driverClassSystemProperty", "driverClassFromSystemProperty");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(ENV_KEY);
        System.clearProperty(SECRET_KEY);
        System.clearProperty("driverClassSystemProperty");
    }

    @Test
    void should_load_local_config_without_errors() throws IOException, ConfigurationException {
        setEnv("local");
        configFactory.build(configSourceProvider, "test-config.yml");
    }

    @Test
    void should_load_test_config_without_errors() throws IOException, ConfigurationException {
        setEnv("test");
        configFactory.build(configSourceProvider, "test-config.yml");
    }

    @Test
    void should_use_default_values_when_environment_has_no_specific_value() throws IOException, ConfigurationException {
        setEnv("local");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("org.postgresql.Driver"));
    }

    @Test
    void should_override_defaults_with_environment_specific_values() throws IOException, ConfigurationException {
        setEnv("test");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("overridden"));
    }

    @Test
    void should_allow_empty_default_with_environment_specific_value() throws IOException, ConfigurationException {
        setEnv("test");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getUrl(), is("test_url"));
    }

    @Test
    void should_load_secret_config_from_file() throws IOException, ConfigurationException {
        setEnv("local");
        System.setProperty(TypeSafeConfigurationFactory.SECRET_KEY, getClass().getResource("/test-secret.yml").getFile());

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("secret_password"));
    }

    @Test
    void supports_environment_specific_config_in_secret_config() throws IOException, ConfigurationException {
        setEnv("test");
        System.setProperty(TypeSafeConfigurationFactory.SECRET_KEY, getClass().getResource("/test-secret.yml").getFile());

        TestConfig testEnvConfig = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(testEnvConfig.secrets.verySecret, is("keep this to yourself!"));

        setEnv("local");
        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.secrets.verySecret, is("not so secret"));
    }

    @Test
    void should_interpolate_variables() throws IOException, ConfigurationException {
        setEnv("test");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("default variable value"));
    }

    @Test
    void should_interpolate_variables_with_overrides() throws IOException, ConfigurationException {
        setEnv("local");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getPassword(), is("overridden variable value"));
    }

    @Test
    void should_resolve_system_properties() throws IOException, ConfigurationException {
        setEnv("test2");

        TestConfig config = configFactory.build(configSourceProvider, "test-config.yml");
        assertThat(config.database.getDriverClass(), is("driverClassFromSystemProperty"));
    }

    private static void setEnv(final String env) {
        System.setProperty(ENV_KEY, env);
    }

}
