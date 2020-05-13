package software.amazon.opsworkscm.server;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;
import software.amazon.opsworkscm.server.utils.LoggerWrapper;

abstract public class BaseOpsWorksCMHandler extends BaseHandler<CallbackContext> {


    protected ResourceModel model;
    protected ResourceModel oldModel;
    protected CallbackContext callbackContext;
    protected LoggerWrapper log;
    protected ResourceHandlerRequest<ResourceModel> request;
    protected ClientWrapper client;

    protected static int NO_CALLBACK_DELAY = 0;
    protected static int CALLBACK_DELAY_SECONDS = 60;
    private static final int MAX_LENGTH_CONFIGURATION_SET_NAME = 40;

    @Override
    abstract public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger);

    protected void initialize(final AmazonWebServicesClientProxy proxy,
                              final ResourceHandlerRequest<ResourceModel> request,
                              final CallbackContext callbackContext,
                              final Logger logger) {
        this.request = request;
        this.model = request.getDesiredResourceState();
        this.oldModel = request.getPreviousResourceState();
        this.callbackContext = callbackContext;
        this.log = new LoggerWrapper(logger);

        setModelServerName();
        setModelId();

        final OpsWorksCmClient opsWorksCmClientclient = ClientBuilder.getClient();
        this.client = new ClientWrapper(opsWorksCmClientclient, model, oldModel, proxy, log);
    }

    private void setModelServerName() {
        if (StringUtils.isNullOrEmpty(model.getServerName())) {
            log.log("RequestModel doesn't have the server name. Setting it using request identifier and client token");
            model.setServerName(
                    IdentifierUtils.generateResourceIdentifier(
                            request.getLogicalResourceIdentifier(),
                            request.getClientRequestToken(),
                            MAX_LENGTH_CONFIGURATION_SET_NAME
                    )
            );
        } else if (model.getServerName().length() > MAX_LENGTH_CONFIGURATION_SET_NAME) {
            log.log(String.format("ServerName length was greater than %d characters. Truncating the ServerName", MAX_LENGTH_CONFIGURATION_SET_NAME));
            model.setServerName(model.getServerName().substring(0, MAX_LENGTH_CONFIGURATION_SET_NAME));
        }
    }

    private void setModelId() {
        if (model.getId() == null) {
            log.log("RequestModel doesn't have the model id. Setting it using request identifier and client token");
            model.setId(IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken()
            ));
        }
    }
}
