package no.posten.digipost.dropwizard;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.*;

public class FileOrResourceConfigurationSourceProvider implements ConfigurationSourceProvider {
    @Override
    public InputStream open(String path) throws IOException {
        final InputStream result;
        final File file = new File(path);
        if (file.exists()) {
            result = new FileInputStream(file);
        } else {
            result = getClass().getClassLoader().getResourceAsStream(path);
            if (result == null) {
                throw new FileNotFoundException("File " + file + " not found");
            }
        }

        return result;
    }
}
