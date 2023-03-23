/*
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigSyntax;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.typesafe.config.ConfigFactory.defaultOverrides;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public class TypeSafeConfigurationFactory<T> extends YamlConfigurationFactory<T> {

    public static final String ENV_KEY = "env";
    public static final String SECRET_KEY = "secret";
    public static final String ENVIRONMENTS_CONFIG_KEY = "environments";

    private static final Logger log = LoggerFactory.getLogger(TypeSafeConfigurationFactory.class);

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;
    private final String propertyPrefix;

    public TypeSafeConfigurationFactory(Class<T> klass, Validator validator, ObjectMapper mapper, String propertyPrefix) {
        super(klass, validator, mapper, propertyPrefix);
        this.propertyPrefix = propertyPrefix.endsWith(".") ? propertyPrefix : propertyPrefix + '.';
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    @Override
    public T build(ConfigurationSourceProvider sourceProvider, String path) throws IOException, ConfigurationException {
        Config loaded = loadConfig(sourceProvider, path);

        Config config = loaded.resolveWith(defaultOverrides(), ConfigResolveOptions.defaults().setAllowUnresolved(true));

        List<String> environments = firstAvailableSystemProperty(ENV_KEY, propertyPrefix + ENV_KEY)
                .map(commaSeparatedEnvs -> Stream.of(commaSeparatedEnvs.split(",\\s?")))
                .orElseThrow(() -> new RuntimeException(
                        "System.property " + ENV_KEY + " is required and must have a corresponding section in the config file. Example: -Denv=local"))
                .collect(toList());

        Config envSpecificConfig = reduceToEnvironmentSpecific(environments, config);

        Config configWithSecrets = firstAvailableSystemProperty(SECRET_KEY)
                .map(secretsPath -> loadConfig(sourceProvider, secretsPath))
                .map(secretsConfig -> reduceToEnvironmentSpecific(environments, secretsConfig).withFallback(envSpecificConfig))
                .orElse(envSpecificConfig);

        ConfigObject rootConfigObject = configWithSecrets.resolve().withoutPath("variables").root();

        logConfig(configWithSecrets, rootConfigObject);

        String configJsonString = rootConfigObject.render(ConfigRenderOptions.concise());
        return super.build(str -> new ByteArrayInputStream(configJsonString.getBytes(UTF_8)), path);
    }

    private Config loadConfig(ConfigurationSourceProvider sourceProvider, String path) {
        try (InputStream source = sourceProvider.open(path)) {
            return path.endsWith(".yml") ? loadYamlConfig(source) : loadConfig(source);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to load config from '" + path + "', because " +
                    e.getClass().getSimpleName() + ": '" + e.getMessage() + "'", e);
        }
    }

    private Config loadConfig(InputStream input) {
        InputStreamReader inputReader = new InputStreamReader(input, UTF_8);
        return ConfigFactory.parseReader(inputReader, ConfigParseOptions.defaults().setAllowMissing(false));
    }

    private Config loadYamlConfig(InputStream input) throws IOException {
        JsonNode node = mapper.readTree(yamlFactory.createParser(input));
        String jsonString = mapper.writeValueAsString(node);
        Config preConfig = ConfigFactory.parseString(jsonString, ConfigParseOptions.defaults().setAllowMissing(false));
        String render = preConfig.root().render(ConfigRenderOptions.defaults().setJson(false));
        return ConfigFactory.parseString(render.replaceAll("=\\s?\"([^$]?\\$.*?)\"", "=$1"), ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF));
    }

    private void logConfig(Config finalConfig, ConfigObject rootConfigObject) {
        String configFactoryLogKey = "logging.loggers.\"" + getClass().getName() + "\"";
        if (finalConfig.hasPath(configFactoryLogKey)) {
            if (finalConfig.getString(configFactoryLogKey).equalsIgnoreCase("debug")) {
                log.debug(rootConfigObject.render(ConfigRenderOptions.defaults().setComments(true).setOriginComments(false)));
            }
        }
    }

    private static Optional<String> firstAvailableSystemProperty(String ... propertyNames) {
        return Stream.of(propertyNames)
            .map(System::getProperty)
            .filter(property -> property != null && !property.isEmpty())
            .findFirst();
    }

    private static Config reduceToEnvironmentSpecific(List<String> environments, Config config) {
        Config envConfig = environments.stream()
                .map(environment -> ENVIRONMENTS_CONFIG_KEY + "." + environment)
                .filter(environmentKey -> config.hasPath(environmentKey))
                .map(environmentKey -> config.getConfig(environmentKey))
                .reduce(ConfigFactory.empty(), Config::withFallback);
        return envConfig.withFallback(config).withoutPath(ENVIRONMENTS_CONFIG_KEY);
    }
}
