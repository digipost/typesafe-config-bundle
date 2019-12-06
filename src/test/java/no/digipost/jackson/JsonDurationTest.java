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
package no.digipost.jackson;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static no.digipost.jackson.JsonDuration.supportedUnits;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.arbitrary;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.strings;

public class JsonDurationTest {

    @Test
    public void correctEqualsAndHashcode() {
        EqualsVerifier
            .forRelaxedEqualExamples(JsonDuration.of("1 days"), JsonDuration.of("24 hours"), JsonDuration.of("1440 minutes"))
            .andUnequalExamples(JsonDuration.of("4 days"), JsonDuration.of("5 days"), JsonDuration.of("1337 nanos"))
            .verify();

        assertThat(JsonDuration.of("1 days"), is(JsonDuration.of("24 hours")));
        assertThat(JsonDuration.of("42 minutes"), not("42 minutes"));
    }

    @Test
    public void parsesUnitsSupportedByJavaTimeDuration() {
        qt()
            .forAll(integers().all(), arbitrary().pick(supportedUnits))
            .checkAssert((amount, unit) -> {
                JsonDuration parsed = JsonDuration.of(amount + " " + unit.name().toLowerCase());
                assertThat(parsed.duration.toMillis(), is(Duration.of(amount, unit).toMillis()));
            });
    }

    @Test
    public void stringRepresentationOfItselfIsParsable() {
        qt()
            .forAll(integers().all(), arbitrary().pick(supportedUnits))
            .as((amount, unit) -> JsonDuration.of(amount + " " + unit.name()))
            .checkAssert(parsed -> assertThat(JsonDuration.of(parsed.toString()), is(parsed)));

    }

    @Test
    public void unableToParseMalforedStrings() {
        qt()
            .forAll(strings().allPossible().ofLengthBetween(0, 100).assuming(s -> !s.matches("\\d+ \\w\\w+")))
            .checkAssert(notParseable -> assertThrows(JsonDuration.CannotConvertToJsonDuration.class, () -> JsonDuration.of(notParseable)));

    }
}
