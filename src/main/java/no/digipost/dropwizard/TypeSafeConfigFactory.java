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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TypeSafeConfigFactory<T> extends YamlConfigurationFactory<T> {

    public static final String ENV_KEY = "env";
    public static final String SECRET_KEY = "secret";
    public static final String ENVIRONMENTS_CONFIG_KEY = "environments";

    private static final Logger log = LoggerFactory.getLogger(TypeSafeConfigFactory.class);

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;

    public TypeSafeConfigFactory(Class<T> klass, Validator validator, ObjectMapper mapper, String propertyPrefix) {
        super(klass, validator, mapper, propertyPrefix);
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    @Override
    public T build(ConfigurationSourceProvider sourceProvider, String path) throws IOException, ConfigurationException {
        Config loaded = loadConfig(sourceProvider, path);

        Config config = loaded.resolveWith(ConfigFactory.defaultOverrides(), ConfigResolveOptions.defaults().setAllowUnresolved(true));

        String env = Optional.ofNullable(System.getProperty(ENV_KEY))
                .filter(envString -> !envString.isEmpty())
                .orElseThrow(() -> new RuntimeException(
                        "System.property " + ENV_KEY + " is required and must have a corresponding section in the config file. Example: -Denv=local"));

        Config envConfig = applyOverrides(env, config);

        Optional<Config> secretConfig = Optional.ofNullable(System.getProperty(SECRET_KEY)).map(secretsPath -> loadConfig(sourceProvider, secretsPath));
        Config configWithSecrets = secretConfig.map(c -> c.withFallback(envConfig)).orElse(envConfig);

        Config finalConfig = configWithSecrets.withoutPath(ENVIRONMENTS_CONFIG_KEY);
        ConfigObject rootConfigObject = finalConfig.resolve().withoutPath("variables").root();

        logConfig(finalConfig, rootConfigObject);

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

    private static Config applyOverrides(String envsSeparatedByComma, Config config) {
        String[] envs = envsSeparatedByComma.split(",\\s?");
        Config envConfig = Stream.of(envs)
                .map(environment -> ENVIRONMENTS_CONFIG_KEY + "." + environment)
                .map(environmentKey -> config.getConfig(environmentKey))
                .reduce(ConfigFactory.empty(), Config::withFallback);
        return envConfig.withFallback(config);
    }
}
