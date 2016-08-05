package no.digipost.jackson;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.ValuesOf;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.NANOS;
import static no.digipost.jackson.JsonDuration.supportedUnits;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;

@RunWith(JUnitQuickcheck.class)
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

    @Property
    public void parsesUnitsSupportedByJavaTimeDuration(int amount, @ValuesOf ChronoUnit unit) {
        assumeThat(unit, isIn(supportedUnits));

        JsonDuration parsed = JsonDuration.of(amount + " " + unit.name().toLowerCase());
        assertThat(parsed.get(NANOS), is(Duration.of(amount, unit).get(NANOS)));
    }

    @Property
    public void stringRepresentationOfItselfIsParsable(int amount, @ValuesOf ChronoUnit unit) {
        assumeThat(unit, isIn(supportedUnits));

        JsonDuration parsed = JsonDuration.of(amount + " " + unit.name());
        assumeThat(JsonDuration.of(parsed.toString()), is(parsed));

    }


    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Property
    public void unableToParseMalforedStrings(String anything) {
        assumeFalse(anything.matches("\\d+ \\w\\w+"));

        expectedException.expect(JsonDuration.CannotConvertToJsonDuration.class);
        JsonDuration.of(anything);
    }
}
