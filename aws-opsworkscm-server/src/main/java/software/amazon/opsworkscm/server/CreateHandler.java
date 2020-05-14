package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.awssdk.services.opsworkscm.model.ServerStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseOpsWorksCMHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        initialize(proxy, request, callbackContext, logger);

        try {
            if (this.callbackContext.isStabilizationStarted()) {
                return handleStabilize();
            } else {
                return handleExecute();
            }
        } catch (InvalidStateException e) {
            log.error(String.format("Service Side failure during create-server for %s.", this.model.getServerName()), e);
            return ProgressEvent.failed(this.model, this.callbackContext, HandlerErrorCode.InternalFailure, "Service Internal Failure");
        } catch (Exception e) {
            log.error(String.format("CreateHandler failure during create-server for %s.", this.model.getServerName()), e);
            return ProgressEvent.failed(this.model, this.callbackContext, HandlerErrorCode.InternalFailure, "Internal Failure");
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleExecute() {
        try {
            client.createServer();
            callbackContext.setStabilizationStarted(true);
            return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
        } catch (ResourceAlreadyExistsException e) {
            log.info(String.format("Server %s already exists.", model.getServerName()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleStabilize() {
        final DescribeServersResponse result;
        final String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        try {
            result = client.describeServer(model.getServerName());
        } catch (final ResourceNotFoundException e) {
            return handleServerNotFound(serverName);
        }

        if (result == null || result.servers() == null) {
            log.info("Describe result is Null. Retrying request.");
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
            case HEALTHY:
                log.info(String.format("Server %s succeeded CREATE.", actualServerName));
                return ProgressEvent.defaultSuccessHandler(model);
            case BACKING_UP:
            case MODIFYING:
            case RESTORING:
            case UNDER_MAINTENANCE:
            case CREATING:
                log.info(String.format("Server %s is still creating.", actualServerName));
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
            default:
                log.info(String.format("Server %s failed to CREATE because of reason: %s", actualServerName, statusReason));
                return ProgressEvent.failed(
                        model,
                        callbackContext,
                        HandlerErrorCode.NotStabilized,
                        String.format("Server %s creation has failed. Server should be %s, but is %s. With reason: %s",
                                serverName, ServerStatus.HEALTHY.toString(), serverStatus.toString(), statusReason));
        }
    }


    private ProgressEvent<ResourceModel, CallbackContext> handleServerNotFound(final String serverName) {
        log.info(String.format("Server %s failed to CREATE because it was not found.", serverName));
        return ProgressEvent.failed(
                model,
                callbackContext,
                HandlerErrorCode.NotFound,
                String.format("Server %s was deleted.", serverName));
    }
}
