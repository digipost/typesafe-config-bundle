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
package no.digipost.jackson;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static no.digipost.jackson.JsonDuration.supportedUnits;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.arbitrary;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.longs;
import static org.quicktheories.generators.SourceDSL.strings;
import static uk.co.probablyfine.matchers.Java8Matchers.where;

class JsonDurationTest {

    @Test
    void correctEqualsAndHashcode() {
        EqualsVerifier
            .forRelaxedEqualExamples(JsonDuration.parse("1 days"), JsonDuration.parse("24 hours"), JsonDuration.parse("1440 minutes"))
            .andUnequalExamples(JsonDuration.parse("4 days"), JsonDuration.parse("5 days"), JsonDuration.parse("1337 nanos"))
            .verify();

        assertThat(JsonDuration.parse("1 days"), is(JsonDuration.parse("24 hours")));
        assertThat(JsonDuration.parse("42 minutes"), not("42 minutes"));
    }

    @Test
    void parsesUnitsSupportedByJavaTimeDuration() {
        qt()
            .forAll(integers().all(), arbitrary().pick(supportedUnits))
            .checkAssert((amount, unit) -> {
                JsonDuration parsed = JsonDuration.parse(amount + " " + unit.name().toLowerCase());
                assertThat(parsed.duration.toMillis(), is(Duration.of(amount, unit).toMillis()));
            });
    }

    @Test
    void stringRepresentationOfItselfIsParsable() {
        qt()
            .forAll(integers().all(), arbitrary().pick(supportedUnits))
            .as((amount, unit) -> JsonDuration.parse(amount + " " + unit.name()))
            .checkAssert(parsed -> assertThat(JsonDuration.parse(parsed.toString()), is(parsed)));

    }

    @Test
    void unableToParseMalforedStrings() {
        qt()
            .forAll(strings().allPossible().ofLengthBetween(0, 100).assuming(s -> !s.matches("\\d+ \\w\\w+")))
            .checkAssert(notParseable -> assertThrows(JsonDuration.CannotConvertToJsonDuration.class, () -> JsonDuration.parse(notParseable)));

    }

    @Nested
    class ConvertFromDuration {
        @Test
        void resolvedAmountAndUnitIsEquivalentToTheDuration() {
            qt()
                .forAll(longs().all().map(Duration::ofNanos).map(JsonDuration::from))
                .checkAssert(jsonDuration -> assertThat(Duration.of(jsonDuration.amount, jsonDuration.unit), is(jsonDuration.duration)));
        }

        @Test
        void useASensibleLargestPossibleUnit() {
            assertAll(
                    () -> assertThat(JsonDuration.from(Duration.ofHours(24)), where(d -> d.unit, is(DAYS))),
                    () -> assertThat(JsonDuration.from(Duration.ofHours(25)), where(d -> d.unit, is(HOURS))),
                    () -> assertThat(JsonDuration.from(Duration.ofHours(25).plusSeconds(42)), where(d -> d.unit, is(SECONDS))),
                    () -> assertThat(JsonDuration.from(Duration.ofHours(24).plusMinutes(10)), where(d -> d.unit, is(MINUTES))));
        }

        @Test
        void handlesSubSecondUnits() {
            assertAll(
                    () -> assertThat(JsonDuration.from(Duration.ofNanos(1_000_000_000)), where(d -> d.unit, is(SECONDS))),
                    () -> assertThat(JsonDuration.from(Duration.ofNanos(1_001_000_000)), where(d -> d.unit, is(MILLIS))),
                    () -> assertThat(JsonDuration.from(Duration.ofNanos(1_001_001_000)), where(d -> d.unit, is(MICROS))),
                    () -> assertThat(JsonDuration.from(Duration.ofNanos(1_001_001_001)), where(d -> d.unit, is(NANOS))));
        }
    }

}
