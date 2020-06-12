package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.OpsWorksCmException;
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

        InvocationContext context = initializeContext(proxy, request, callbackContext, logger);

        try {
            if (context.getCallbackContext().isStabilizationStarted()) {
                return handleStabilize(context);
            } else {
                return handleExecute(context);
            }
        } catch (InvalidStateException e) {
            log.error(String.format("Service Side failure during create-server for %s.", context.getModel().getServerName()), e);
            return ProgressEvent.failed(context.getModel(), context.getCallbackContext(), HandlerErrorCode.InternalFailure, "Service Internal Failure");
        } catch (OpsWorksCmException e) {
            log.error(String.format("ValidationException during create-server for %s.", context.getModel().getServerName()), e);
            return ProgressEvent.failed(context.getModel(), context.getCallbackContext(), HandlerErrorCode.InvalidRequest, e.getMessage());
        } catch (Exception e) {
            log.error(String.format("CreateHandler failure during create-server for %s.", context.getModel().getServerName()), e);
            return ProgressEvent.failed(context.getModel(), context.getCallbackContext(), HandlerErrorCode.InternalFailure, "Internal Failure");
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleExecute(InvocationContext context) {
        try {
            client.createServer();
            context.getCallbackContext().setStabilizationStarted(true);
            return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), CALLBACK_DELAY_SECONDS, context.getModel());
        } catch (ResourceAlreadyExistsException e) {
            log.info(String.format("Server %s already exists.", context.getModel().getServerName()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleStabilize(InvocationContext context) {
        final DescribeServersResponse result;
        ResourceModel model = context.getModel();
        CallbackContext callbackContext = context.getCallbackContext();

        final String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        try {
            result = client.describeServer(model.getServerName());
        } catch (final ResourceNotFoundException e) {
            return handleServerNotFound(context, serverName);
        }

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


    private ProgressEvent<ResourceModel, CallbackContext> handleServerNotFound(InvocationContext context, final String serverName) {
        log.info(String.format("Server %s failed to CREATE because it was not found.", serverName));
        return ProgressEvent.failed(
                context.getModel(),
                context.getCallbackContext(),
                HandlerErrorCode.NotFound,
                String.format("Server %s was deleted.", serverName));
    }
}
