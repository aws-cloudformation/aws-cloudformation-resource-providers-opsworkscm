package software.amazon.opsworkscm.server;

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-opsworkscm-server.json");
    }
}
