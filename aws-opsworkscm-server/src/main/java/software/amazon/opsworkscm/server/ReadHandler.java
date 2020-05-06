package software.amazon.opsworkscm.server;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;

public class ReadHandler extends BaseHandler<CallbackContext> {

    ResourceModel model;
    ResourceModel oldModel;
    CallbackContext callbackContext;
    Logger logger;
    ResourceHandlerRequest<ResourceModel> request;
    ClientWrapper client;

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

        final DescribeServersResponse result;
        final String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        logger.log(String.format("Calling Describe Servers for ServerName %s", serverName));

        try {
            result = client.describeServer();
            Server server = result.servers().get(0);
            addDescribeServerResponseAttributes(server);
            return ProgressEvent.defaultSuccessHandler(model);
        } catch (final software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException e) {
            logger.log(String.format("Server %s was not found.", serverName));
            throw new ResourceNotFoundException(String.format("Server %s was not found.", serverName), e.getMessage());
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
            logger.log(String.format("ServerName %s was greater than %d characters", model.getServerName(), MAX_LENGTH_CONFIGURATION_SET_NAME));
            throw new CfnInvalidRequestException(String.format("ServerName %s must have less than or equal to %d characters", model.getServerName(), MAX_LENGTH_CONFIGURATION_SET_NAME));
        } else {
            model.setServerName(model.getServerName());
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

    private void addDescribeServerResponseAttributes(final Server server) {
        model.setEndpoint(server.endpoint());
        model.setArn(server.serverArn());
    }
}
