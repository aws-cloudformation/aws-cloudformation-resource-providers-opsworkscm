package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseOpsWorksCMHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        initialize(proxy, request, callbackContext, logger);

        final DescribeServersResponse result;
        final String serverName = model.getServerName();
        callbackContext.incrementRetryTimes();

        log.info(String.format("Calling Describe Servers for ServerName %s", serverName));

        try {
            result = client.describeServer();
            Server server = result.servers().get(0);
            addDescribeServerResponseAttributes(server);
            return ProgressEvent.defaultSuccessHandler(model);
        } catch (final software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException e) {
            log.error(String.format("Server %s was not found.", serverName), e);
            throw new ResourceNotFoundException(String.format("Server %s was not found.", serverName), e.getMessage());
        }
    }

    private void addDescribeServerResponseAttributes(final Server server) {
        model.setEndpoint(server.endpoint());
        model.setArn(server.serverArn());
    }
}
