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
import java.util.stream.Collectors;

public class ListHandler extends BaseOpsWorksCMHandler {

    private static final int NO_CALLBACK_DELAY = 0;

    LoggerWrapper log;
    ClientWrapper client;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        this.log = new LoggerWrapper(logger);

        final OpsWorksCmClient opsWorksCmClient = ClientBuilder.getClient();
        this.client = new ClientWrapper(opsWorksCmClient, request.getDesiredResourceState(), request.getPreviousResourceState(), proxy, log);

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

    //use same function for all returns
    private List<ResourceModel> addDescribeServersResponseAttributes(final DescribeServersResponse response) {
        List<ResourceModel> models = new ArrayList<>();
        List<Server> servers = response.hasServers() ? response.servers() : new ArrayList<>();
        servers.forEach(server -> {
            List<Tag> tags = client.listServerTags(server.serverArn()).tags().stream()
                    .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build()).collect(Collectors.toList());
            models.add(generateModelFromServer(server, tags));
        });
        return models;
    }
}
