package software.amazon.opsworkscm.server;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    ResourceModel model;
    ResourceModel oldModel;
    CallbackContext callbackContext;
    Logger logger;
    ResourceHandlerRequest<ResourceModel> request;
    ClientWrapper client;

    private static int NO_CALLBACK_DELAY = 0;
    private static final int MAX_LENGTH_CONFIGURATION_SET_NAME = 40;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        this.request = request;
        this.model = request.getDesiredResourceState();
        this.oldModel = request.getPreviousResourceState();
        this.callbackContext = callbackContext;
        this.logger = logger;

        setModelServerName();
        setModelId();

        final OpsWorksCmClient opsWorksCmClientclient = ClientBuilder.getClient();
        this.client = new ClientWrapper(opsWorksCmClientclient, model, oldModel, proxy, logger);

        try {
            if (!callbackContext.isUpdateTagComplete()) {
                return updateTags();
            }
            if (!callbackContext.isUpdateServerComplete()) {
                return updateServer();
            }
            return ProgressEvent.defaultSuccessHandler(model);
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("ResourceNotFoundException during update of server %s, with message %s", model.getServerName(), e.getMessage()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        } catch (InvalidStateException e) {
            logger.log(String.format("InvalidStateException during update of server %s, with message %s", model.getServerName(), e.getMessage()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotUpdatable);
        } catch (ValidationException e) {
            logger.log(String.format("ValidationException during update of server %s, with message %s", model.getServerName(), e.getMessage()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InvalidRequest);
        }
    }

    public ProgressEvent<ResourceModel, CallbackContext> updateTags() {
        client.untagServer();
        client.tagServer();
        callbackContext.setUpdateTagComplete(true);
        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    public ProgressEvent<ResourceModel, CallbackContext> updateServer() {
        client.updateServer();
        callbackContext.setUpdateServerComplete(true);
        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    private void setModelServerName() {
        if (StringUtils.isNullOrEmpty(model.getServerName())) {
            logger.log("RequestModel doesn't have the server name. Setting it using request identifier and client token");
            model.setServerName(
                    IdentifierUtils.generateResourceIdentifier(
                            request.getLogicalResourceIdentifier(),
                            request.getClientRequestToken(),
                            MAX_LENGTH_CONFIGURATION_SET_NAME
                    )
            );
        } else if (model.getServerName().length() > MAX_LENGTH_CONFIGURATION_SET_NAME) {
            logger.log(String.format("ServerName length was greater than %d characters. Truncating the ServerName", MAX_LENGTH_CONFIGURATION_SET_NAME));
            model.setServerName(model.getServerName().substring(0, MAX_LENGTH_CONFIGURATION_SET_NAME));
        }
    }

    private void setModelId() {
        if (model.getId() == null) {
            logger.log("RequestModel doesn't have the model id. Setting it using request identifier and client token");
            model.setId(IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken()
            ));
        }
    }

}
