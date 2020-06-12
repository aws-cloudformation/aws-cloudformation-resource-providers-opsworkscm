package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.awssdk.services.opsworkscm.model.ServerStatus;
import software.amazon.awssdk.services.opsworkscm.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.opsworkscm.server.ResourceModel.IDENTIFIER_KEY_SERVERNAME;

public class DeleteHandler extends BaseOpsWorksCMHandler {

    public static final String SERVER_DELETION_FAILED_MESSAGE = "Server %s deletion has failed with reason: %s";
    public static final String SERVER_OPERATION_STILL_IN_PROGRESS_MESSAGE = "Cannot delete the server '%s'. The current operation on the server is still in progress\\..*";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callBackContext,
            final Logger logger) {

        InvocationContext context = initializeContext(proxy, request, callBackContext, logger);

        final String serverName = context.getModel().getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString();

        try {
            if (context.getCallbackContext().isStabilizationStarted()) {
                return handleStabilize(context);
            } else {
                return handleExecute(context);
            }
        } catch (InvalidStateException e) {
            log.error(String.format("Service Side failure during delete-server for %s.", serverName), e);
            return ProgressEvent.failed(context.getModel(), context.getCallbackContext(), HandlerErrorCode.NotStabilized, "Service Internal Failure");
        } catch (ResourceNotFoundException e) {
            return handleServerNotFound(context, serverName);
        } catch (ValidationException e) {
            log.error(String.format("ValidationException during delete-server of %s.", serverName), e);
            if (e.getMessage().matches(String.format(SERVER_OPERATION_STILL_IN_PROGRESS_MESSAGE, serverName))) {
                log.error(String.format("Server operation still in progress during delete-server of %s.", serverName));
                return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), CALLBACK_DELAY_SECONDS, context.getModel());
            }
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InvalidRequest);
        } catch (Exception e) {
            log.error(String.format("CreateHandler failure during delete-server for %s.", serverName), e);
            return ProgressEvent.failed(context.getModel(), context.getCallbackContext(), HandlerErrorCode.InternalFailure, "Internal Failure");
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleExecute(InvocationContext context) {
        client.deleteServer();
        context.getCallbackContext().setStabilizationStarted(true);
        return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), CALLBACK_DELAY_SECONDS, context.getModel());
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleStabilize(InvocationContext context) {
        final DescribeServersResponse result;
        ResourceModel model = context.getModel();
        CallbackContext callbackContext = context.getCallbackContext();
        String serverName = model.getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString();
        callbackContext.incrementRetryTimes();

        result = client.describeServer(serverName);

        if (result == null || result.servers() == null) {
            log.info("Describe result is Null. Retrying request.");
            return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
        }

        if (result.servers().size() < 1) {
            return handleServerNotFound(context, serverName);
        }
        Server server = result.servers().get(0);

        ServerStatus serverStatus = server.status();
        String statusReason = server.statusReason();
        String actualServerName = server.serverName();

        switch (serverStatus) {
            case DELETING:
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
            case FAILED:
                log.info(String.format(SERVER_DELETION_FAILED_MESSAGE, actualServerName, statusReason));
                return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotUpdatable, String.format(SERVER_DELETION_FAILED_MESSAGE, actualServerName, statusReason));
            default:
                log.info(String.format("Server %s is in an unexpected state. Server should be deleted, but is %s. With reason: %s",
                        actualServerName, serverStatus, statusReason));
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleServerNotFound(InvocationContext context, final String serverName) {
        log.info(String.format("Server %s deleted successfully.", serverName));
        return ProgressEvent.defaultSuccessHandler(context.getModel());
    }
}
