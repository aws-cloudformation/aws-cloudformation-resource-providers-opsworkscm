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

        InvocationContext context = initializeContext(proxy, request, callbackContext, logger);

        try {
            if (!context.getCallbackContext().isUpdateTagComplete()) {
                return updateTags(context);
            }
            if (!context.getCallbackContext().isUpdateServerComplete()) {
                return updateServer(context);
            }
            return ProgressEvent.defaultSuccessHandler(context.getModel());
        } catch (ResourceNotFoundException e) {
            log.error(String.format("ResourceNotFoundException during update of server %s, with message %s", context.getModel().getServerName(), e.getMessage()), e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        } catch (InvalidStateException e) {
            log.error(String.format("InvalidStateException during update of server %s, with message %s", context.getModel().getServerName(), e.getMessage()), e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotUpdatable);
        } catch (ValidationException e) {
            log.error(String.format("ValidationException during update of server %s, with message %s", context.getModel().getServerName(), e.getMessage()), e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InvalidRequest);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(InvocationContext context) {
        client.untagServer();
        client.tagServer();
        context.getCallbackContext().setUpdateTagComplete(true);
        return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), NO_CALLBACK_DELAY, context.getModel());
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateServer(InvocationContext context) {
        client.updateServer();
        context.getCallbackContext().setUpdateServerComplete(true);
        return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), NO_CALLBACK_DELAY, context.getModel());
    }
}
