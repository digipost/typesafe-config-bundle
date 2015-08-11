package no.digipost.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class TestConfig {

    @Valid
    @NotNull
    @JsonProperty
    public DataSourceFactory database = new DataSourceFactory();

    @JsonProperty
    public String environment;
}
