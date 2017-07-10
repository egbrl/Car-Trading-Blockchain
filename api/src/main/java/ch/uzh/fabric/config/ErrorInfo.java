package ch.uzh.fabric.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(Include.NON_NULL)
public class ErrorInfo extends JsonObject {
    public final int status;
    public final String location;
    @JsonUnwrapped
    @JsonInclude(Include.NON_NULL)
    public final Object errorMessage;

    public ErrorInfo(int status, String pathinfo, Object errorMessage) {
        this.status = status;
        this.location = pathinfo;
        this.errorMessage = errorMessage;
    }
}