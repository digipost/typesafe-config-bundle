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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class JsonDuration implements TemporalAmount, Serializable {

    private static final long serialVersionUID = 7565437081331942214L;

    public static final List<ChronoUnit> supportedUnits = unmodifiableList(Stream.of(ChronoUnit.values()).filter(u -> !u.isDurationEstimated() || u == DAYS).collect(toList()));

    public final long amount;
    public final TemporalUnit unit;
    public final Duration duration;
    private final String stringRepresentation;

    @Deprecated
    public static JsonDuration of(String jsonString) {
        return parse(jsonString);
    }

    @JsonCreator
    public static JsonDuration parse(String jsonString) {
        try {
            String[] amountAndUnit = jsonString.split("\\s+");
            long amount = Long.parseLong(amountAndUnit[0]);
            ChronoUnit unit = ChronoUnit.valueOf(amountAndUnit[1].toUpperCase());
            return new JsonDuration(amount, unit, null);
        } catch (Exception e) {
            throw new CannotConvertToJsonDuration(jsonString, e);
        }

    }

    public static JsonDuration from(Duration duration) {
        long nanosPart = duration.toNanosPart();
        if (nanosPart != 0) {
            long totalNanos = duration.toNanos();
            if (totalNanos % 1000 != 0) {
                return new JsonDuration(totalNanos, NANOS, duration);
            } else if (totalNanos % 1_000_000 != 0) {
                return new JsonDuration(totalNanos / 1000, MICROS, duration);
            } else {
                return new JsonDuration(duration.toMillis(), MILLIS, duration);
            }
        } else if (duration.toSecondsPart() != 0) {
            return new JsonDuration(duration.toSeconds(), SECONDS, duration);
        } else if (duration.toMinutesPart() != 0) {
            return new JsonDuration(duration.toMinutes(), MINUTES, duration);
        } else if (duration.toHoursPart() != 0) {
            return new JsonDuration(duration.toHours(), HOURS, duration);
        } else {
            return new JsonDuration(duration.toDays(), DAYS, duration);
        }
    }

    private JsonDuration(long amount, ChronoUnit unit, Duration duration) {
        this.amount = amount;
        this.unit = unit;
        this.stringRepresentation = amount + " " + unit.name();
        this.duration = duration != null ? duration : Duration.of(amount, unit);
    }


    @Override
    @JsonValue
    public String toString() {
        return stringRepresentation;
    }

    public static class CannotConvertToJsonDuration extends RuntimeException {
        public CannotConvertToJsonDuration(String jsonString, Throwable cause) {
            super("Unable to convert \"" + jsonString + "\" to " + JsonDuration.class.getSimpleName() + " because " +
                  cause.getClass().getSimpleName() + ": '" + cause.getMessage() + "'. " +
                  "String must be on the form \"<amount> <unit>\", where the unit is one of " +
                  supportedUnits.stream().map(Enum::name).collect(joining(", ")) + " (case-insensitive)");
        }
    }


    @Override
    public long get(TemporalUnit unit) {
        return duration.get(unit);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return duration.getUnits();
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        return duration.addTo(temporal);
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        return duration.subtractFrom(temporal);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JsonDuration) {
            JsonDuration that = (JsonDuration) obj;
            return Objects.equals(this.duration, that.duration);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

}
