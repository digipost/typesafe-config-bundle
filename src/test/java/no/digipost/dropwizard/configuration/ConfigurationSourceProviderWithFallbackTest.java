package no.digipost.dropwizard.configuration;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static co.unruly.matchers.Java8Matchers.where;
import static com.google.common.io.ByteStreams.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationSourceProviderWithFallbackTest {

    @Test
    void resolveConfigurationSourceFromMainProvider() throws IOException {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> new ByteArrayInputStream("main".getBytes()),
                (path) -> { throw new AssertionError("fallback should not be invoked"); });

        try (InputStream source = providerWithFallback.open("path")) {
            assertThat(toByteArray(source), is("main".getBytes()));
        }
    }

    @Test
    void ioExceptionFromMainProviderIsIgnoredIfFallbackSucceeds() throws IOException {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> { throw new IOException("main failure"); },
                (path) -> new ByteArrayInputStream("fallback".getBytes()));

        try (InputStream source = providerWithFallback.open("path")) {
            assertThat(new String(toByteArray(source)), is("fallback"));
        }
    }

    @Test
    void runtimeExceptionFromMainProviderIsThrownAsIs() {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> { throw new IllegalStateException("bug"); },
                (path) -> { throw new AssertionError("fallback should not be invoked"); });

        assertThrows(IllegalStateException.class, () -> providerWithFallback.open("path"));
    }

    @Test
    void runtimeExceptionFromFallbackIncludesSuppressedExceptionFromMainProvider() {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> { throw new IOException("main failure"); },
                (path) -> { throw new IllegalStateException("bug in fallback provider"); });

        IllegalStateException bug = assertThrows(IllegalStateException.class, () -> providerWithFallback.open("path"));
        assertThat(bug, where(Throwable::getMessage, is("bug in fallback provider")));
        assertThat(bug, where(Throwable::getSuppressed, arrayContaining(instanceOf(IOException.class))));
    }

    @Test
    void ioExceptionInFallbackIsPropagatedAsSourceNotFound() {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> { throw new FileNotFoundException(path + " not found"); },
                (path) -> { throw new FileNotFoundException(path + " still not found"); });

        ConfigurationSourceNotFoundException notFound = assertThrows(ConfigurationSourceNotFoundException.class, () -> providerWithFallback.open("some/path"));
        assertThat(notFound.getCause(), where(Throwable::getMessage, is("some/path not found")));
        assertThat(notFound.getSuppressed(), arrayContaining(where(Throwable::getMessage, is("some/path still not found"))));
    }




}
