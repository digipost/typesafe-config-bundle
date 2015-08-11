package no.posten.digipost.dropwizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.typesafe.config.*;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TypeSafeConfigFactory<T> extends ConfigurationFactory<T> {

    public static final String ENV_KEY = "env";
    public static final String SECRET_KEY = "secret";
    public static final String ENVIRONMENTS_CONFIG_KEY = "environments";

    private static final Logger log = LoggerFactory.getLogger(TypeSafeConfigFactory.class);

    private final ObjectMapper mapper;
    private final YAMLFactory yamlFactory;

    public TypeSafeConfigFactory(final Class<T> klass, final Validator validator, final ObjectMapper mapper, final String propertyPrefix) {
        super(klass, validator, mapper, propertyPrefix);
        this.mapper = mapper;
        this.yamlFactory = new YAMLFactory();
    }

    @Override
    public T build(final ConfigurationSourceProvider provider, final String path) throws IOException, ConfigurationException {
        try (InputStream input = provider.open(path)) {

            final Config config;
            if (path.endsWith(".yml")) {
                config = loadYamlConfig(input);
            } else {
                config = loadConfig(input);
            }

            final Optional<String> env = Optional.ofNullable(System.getProperty(ENV_KEY)).filter(not(String::isEmpty));
            if (!env.isPresent()) {
                throw new RuntimeException("System.property " + ENV_KEY + " is required and must have a corresponding section in the config file. Example: -Denv=local");
            }
            final Config envConfig = env.map(applyOverrides(config)).orElse(config).withValue("environment", ConfigValueFactory.fromAnyRef(env.get()));

            final Optional<String> secret = Optional.ofNullable(System.getProperty(SECRET_KEY));
            final Config secretConfig = secret.map(this::loadSecret).orElse(ConfigFactory.empty());
            final Config configWithSecrets = secretConfig.withFallback(envConfig);

            final Config finalConfig = configWithSecrets.withoutPath(ENVIRONMENTS_CONFIG_KEY);
            final ConfigObject rootConfigObject = finalConfig.resolve().root();

            logConfig(finalConfig, rootConfigObject);

            final String configJsonString = rootConfigObject.render(ConfigRenderOptions.concise());
            return super.build(
                    str -> new ByteArrayInputStream(configJsonString.getBytes(StandardCharsets.UTF_8)), path);
        }
    }

    private Config loadConfig(final InputStream input) {
        final Config config;InputStreamReader inputReader = new InputStreamReader(input, StandardCharsets.UTF_8);
        config = ConfigFactory.parseReader(inputReader, ConfigParseOptions.defaults().setAllowMissing(false));
        return config;
    }

    private Config loadYamlConfig(final InputStream input) throws IOException {
        final JsonNode node = mapper.readTree(yamlFactory.createParser(input));
        final String jsonString = mapper.writeValueAsString(node);
        final Config preConfig = ConfigFactory.parseString(jsonString, ConfigParseOptions.defaults().setAllowMissing(false));
        final String render = preConfig.root().render(ConfigRenderOptions.defaults().setJson(false));
        return ConfigFactory.parseString(render.replaceAll("=\\s?\"([^$]?\\$.*?)\"", "=$1"), ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF));
    }

    private void logConfig(final Config finalConfig, final ConfigObject rootConfigObject) {
        final String configFactoryLogKey = "logging.loggers.\"" + getClass().getName() + "\"";
        if (finalConfig.hasPath(configFactoryLogKey)) {
            if (finalConfig.getString(configFactoryLogKey).equalsIgnoreCase("debug")) {
                log.debug(rootConfigObject.render(ConfigRenderOptions.defaults().setComments(true).setOriginComments(false)));
            }
        }
    }

    private Predicate<String> not(final Predicate<String> predicate) {
        return predicate.negate();
    }

    private Config loadSecret(final String filename) {
        final File file = new File(filename);
        try(final InputStream input = new FileInputStream(file)){
            if (filename.endsWith(".yml")) {
                return loadYamlConfig(input);
            } else {
                return loadConfig(input);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Function<String, Config> applyOverrides(final Config config) {
        return env -> {
            final String[] envs = env.split(",\\s?");
            final Config envConfig = Stream.of(envs)
                    .map(e -> ENVIRONMENTS_CONFIG_KEY + "." + e)
                    .map(config::getConfig)
                    .reduce(ConfigFactory.empty(), Config::withFallback);
            return envConfig.withFallback(config);
        };
    }
}
