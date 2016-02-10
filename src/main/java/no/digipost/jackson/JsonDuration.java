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
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class JsonDuration implements TemporalAmount, Serializable {

    public static final List<ChronoUnit> supportedUnits = unmodifiableList(Stream.of(ChronoUnit.values()).filter(u -> !u.isDurationEstimated() || u == DAYS).collect(toList()));

    public final long amount;
    public final TemporalUnit unit;
    public final Duration duration;
    private final String stringRepresentation;

    @JsonCreator
    public static JsonDuration of(String jsonString) {
        return new JsonDuration(jsonString);
    }

    private JsonDuration(String jsonString) {
        try {
            String[] amountAndUnit = jsonString.split("\\s+");
            amount = Long.parseLong(amountAndUnit[0]);
            ChronoUnit chronoUnit = ChronoUnit.valueOf(amountAndUnit[1].toUpperCase());
            unit = chronoUnit;
            duration = Duration.of(amount, unit);
            stringRepresentation = amount + " " + chronoUnit.name();
        } catch (Exception e) {
            throw new CannotConvertToJsonDuration(jsonString, e);
        }
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