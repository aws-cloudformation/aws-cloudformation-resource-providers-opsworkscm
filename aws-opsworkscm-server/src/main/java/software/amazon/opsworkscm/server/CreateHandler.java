package software.amazon.opsworkscm.server;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
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
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandler<CallbackContext> {

    ResourceModel model;
    ResourceModel oldModel;
    CallbackContext callbackContext;
    Logger logger;
    ResourceHandlerRequest<ResourceModel> request;
    ClientWrapper client;

    private static int NO_CALLBACK_DELAY = 0;
    private static int CALLBACK_DELAY_SECONDS = 60;
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
            if (callbackContext.isStabilizationStarted()) {
                return handleStabilize();
            } else {
                return handleExecute();
            }
        } catch (InvalidStateException e) {
            logger.log(String.format("Service Side failure during create-server for %s.", model.getServerName()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure, "Service Internal Failure");
        } catch (Exception e) {
            logger.log(String.format("CreateHandler failure during create-server for %s.", model.getServerName()));
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InternalFailure, "Internal Failure");
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleExecute() {
        try {
            client.createServer();
            callbackContext.setStabilizationStarted(true);
            return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Server %s already exists.", model.getServerName()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleStabilize() {
        final DescribeServersResponse result;
        final String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        try {
            result = client.describeServer();
        } catch (final ResourceNotFoundException e) {
            return handleServerNotFound(serverName);
        }

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
            case HEALTHY:
                logger.log(String.format("Server %s succeeded CREATE.", actualServerName));
                return ProgressEvent.defaultSuccessHandler(model);
            case BACKING_UP:
            case MODIFYING:
            case RESTORING:
            case UNDER_MAINTENANCE:
            case CREATING:
                return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, model);
            default:
                logger.log(String.format("Server %s failed to CREATE because of reason: %s", actualServerName, statusReason));
                return ProgressEvent.failed(
                        model,
                        callbackContext,
                        HandlerErrorCode.NotStabilized,
                        String.format("Server %s creation has failed. Server should be %s, but is %s. With reason: %s",
                                serverName, ServerStatus.HEALTHY.toString(), serverStatus.toString(), statusReason));
        }
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

    private ProgressEvent<ResourceModel, CallbackContext> handleServerNotFound(final String serverName) {
        logger.log(String.format("Server %s failed to CREATE because it was not found.", serverName));
        return ProgressEvent.failed(
                model,
                callbackContext,
                HandlerErrorCode.NotFound,
                String.format("Server %s was deleted.", serverName));
    }
}
