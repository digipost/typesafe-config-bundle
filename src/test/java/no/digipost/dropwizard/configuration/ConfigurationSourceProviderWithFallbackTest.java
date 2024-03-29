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
package no.digipost.dropwizard.configuration;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.co.probablyfine.matchers.Java8Matchers.where;

class ConfigurationSourceProviderWithFallbackTest {

    @Test
    void resolveConfigurationSourceFromMainProvider() throws IOException {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> new ByteArrayInputStream("main".getBytes()),
                (path) -> { throw new AssertionError("fallback should not be invoked"); });

        try (InputStream source = providerWithFallback.open("path")) {
            assertThat(source.readAllBytes(), is("main".getBytes()));
        }
    }

    @Test
    void ioExceptionFromMainProviderIsIgnoredIfFallbackSucceeds() throws IOException {
        ConfigurationSourceProviderWithFallback providerWithFallback = new ConfigurationSourceProviderWithFallback(
                (path) -> { throw new IOException("main failure"); },
                (path) -> new ByteArrayInputStream("fallback".getBytes()));

        try (InputStream source = providerWithFallback.open("path")) {
            assertThat(new String(source.readAllBytes()), is("fallback"));
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
