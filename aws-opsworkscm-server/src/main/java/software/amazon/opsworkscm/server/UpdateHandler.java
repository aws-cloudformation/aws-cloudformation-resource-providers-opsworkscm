package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseOpsWorksCMHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        initialize(proxy, request, callbackContext, logger);

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

    private ProgressEvent<ResourceModel, CallbackContext> updateTags() {
        client.untagServer();
        client.tagServer();
        callbackContext.setUpdateTagComplete(true);
        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateServer() {
        client.updateServer();
        callbackContext.setUpdateServerComplete(true);
        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }
}
