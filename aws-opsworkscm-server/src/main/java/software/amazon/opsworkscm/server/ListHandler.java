package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.opsworkscm.server.utils.LoggerWrapper;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {

    private static final int NO_CALLBACK_DELAY = 0;

    CallbackContext callbackContext;
    LoggerWrapper log;
    ResourceHandlerRequest<ResourceModel> request;
    ClientWrapper client;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        this.log = new LoggerWrapper(logger);

        final OpsWorksCmClient opsWorksCmClientclient = ClientBuilder.getClient();
        this.client = new ClientWrapper(opsWorksCmClientclient, request.getDesiredResourceState(), request.getPreviousResourceState(), proxy, log);

        log.info("Calling Describe Servers with no ServerName");

        DescribeServersResponse result = client.describeAllServers();
        if (result == null || result.servers() == null) {
            log.info("Describe result is Null. Retrying request.");
            return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, request.getDesiredResourceState());
        }

        List<ResourceModel> models = addDescribeServersResponseAttributes(result);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private List<ResourceModel> addDescribeServersResponseAttributes(final DescribeServersResponse response) {
        List<ResourceModel> models = new ArrayList<>();
        List<Server> servers = response.hasServers() ? response.servers() : new ArrayList<>();
        servers.forEach(server -> models.add(ResourceModel.builder()
                .endpoint(server.endpoint())
                .serverName(server.serverName())
                .build()));
        return models;
    }
}
