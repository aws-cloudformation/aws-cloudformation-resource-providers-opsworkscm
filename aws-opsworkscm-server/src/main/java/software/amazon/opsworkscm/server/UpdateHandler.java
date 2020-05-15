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
            if (!this.callbackContext.isUpdateTagComplete()) {
                return updateTags();
            }
            if (!this.callbackContext.isUpdateServerComplete()) {
                return updateServer();
            }
            return ProgressEvent.defaultSuccessHandler(this.model);
        } catch (ResourceNotFoundException e) {
            log.error(String.format("ResourceNotFoundException during update of server %s, with message %s", this.model.getServerName(), e.getMessage()), e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        } catch (InvalidStateException e) {
            log.error(String.format("InvalidStateException during update of server %s, with message %s", this.model.getServerName(), e.getMessage()), e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotUpdatable);
        } catch (ValidationException e) {
            log.error(String.format("ValidationException during update of server %s, with message %s", this.model.getServerName(), e.getMessage()), e);
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
