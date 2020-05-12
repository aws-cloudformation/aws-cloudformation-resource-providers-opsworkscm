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

public class DeleteHandler extends BaseOpsWorksCMHandler {

    public static final String SERVER_DELETION_FAILED_MESSAGE = "Server %s deletion has failed with reason: %s";
    public static final String SERVER_OPERATION_STILL_IN_PROGRESS_MESSAGE = "Cannot delete the server '%s'. The current operation on the server is still in progress\\." +
            " \\(Service: AWSOpsWorksCM; Status Code: 400; Error Code: ValidationException; Request ID: .*\\)";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        initialize(proxy, request, callbackContext, logger);

        try {
            if (callbackContext.isStabilizationStarted()) {
                return handleStabilize();
            } else {
                return handleExecute();
            }
        } catch (InvalidStateException e) {
            logger.log(String.format("Service Side failure during delete-server for %s.", model.getServerName()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotStabilized, "Service Internal Failure");
        } catch (ResourceNotFoundException e) {
            return handleServerNotFound(model.getServerName());
        } catch (ValidationException e) {
            if (e.getMessage().matches(String.format(SERVER_OPERATION_STILL_IN_PROGRESS_MESSAGE, model.getServerName()))) {
                logger.log(String.format("Server operation still in progress during delete-server of %s.", model.getServerName()));
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
            }
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InvalidRequest);
        } catch (Exception e) {
            logger.log(String.format("CreateHandler failure during delete-server for %s.", model.getServerName()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure, "Internal Failure");
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleExecute() {
        client.deleteServer();
        callbackContext.setStabilizationStarted(true);
        return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleStabilize() {
        final DescribeServersResponse result;
        String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        result = client.describeServer();

        if (result == null || result.servers() == null) {
            logger.log("Describe result is Null. Retrying request.");
            return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
        }

        if (result.servers().size() < 1) {
            return handleServerNotFound(serverName);
        }
        Server server = result.servers().get(0);

        ServerStatus serverStatus = server.status();
        String statusReason = server.statusReason();
        String actualServerName = server.serverName();

        switch (serverStatus) {
            case DELETING:
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
            case FAILED:
                logger.log(String.format(SERVER_DELETION_FAILED_MESSAGE, actualServerName, statusReason));
                return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotUpdatable, String.format(SERVER_DELETION_FAILED_MESSAGE, actualServerName, statusReason));
            default:
                logger.log(String.format("Server %s is in an unexpected state. Server should be deleted, but is %s. With reason: %s",
                        actualServerName, serverStatus, statusReason));
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleServerNotFound(final String serverName) {
        logger.log(String.format("Server %s deleted successfully.", serverName));
        return ProgressEvent.defaultSuccessHandler(model);
    }
}
