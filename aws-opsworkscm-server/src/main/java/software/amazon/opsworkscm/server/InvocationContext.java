package software.amazon.opsworkscm.server;

import lombok.Data;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.opsworkscm.server.utils.LoggerWrapper;

@Data
public class InvocationContext {
    ResourceModel model;
    ResourceModel oldModel;
    CallbackContext callbackContext;
    ResourceHandlerRequest<ResourceModel> request;
}
