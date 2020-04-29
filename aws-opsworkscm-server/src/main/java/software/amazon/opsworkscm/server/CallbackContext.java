package software.amazon.opsworkscm.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext {
    private boolean stabilizationStarted;
    private int stabilizationRetryTimes;

    public int incrementRetryTimes() {
        final int newRetryTimes = getStabilizationRetryTimes() + 1;
        setStabilizationRetryTimes(newRetryTimes);
        return newRetryTimes;
    }
}
