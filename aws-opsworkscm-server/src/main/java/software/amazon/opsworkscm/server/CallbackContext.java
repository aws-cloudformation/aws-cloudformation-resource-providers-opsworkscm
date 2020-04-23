package software.amazon.opsworkscm.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext {
    private boolean createServerStarted;
    private boolean createServerStabilized;
    private int stabilizationRetriesRemaining;

    @JsonPOJOBuilder(withPrefix = "")
    public static class CallbackContextBuilder {
    }

    @JsonIgnore
    public void decrementStabilizationRetriesRemaining() {
        stabilizationRetriesRemaining--;
    }
}
