package software.amazon.opsworkscm.server;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;
import software.amazon.opsworkscm.server.utils.LoggerWrapper;

import java.util.List;

import static software.amazon.opsworkscm.server.ResourceModel.IDENTIFIER_KEY_SERVERNAME;

abstract public class BaseOpsWorksCMHandler extends BaseHandler<CallbackContext> {

    public final static String resourceTypeName = "OpsWorksCM::Server";

    protected ClientWrapper client;
    protected LoggerWrapper log;

    protected static int NO_CALLBACK_DELAY = 0;
    protected static int CALLBACK_DELAY_SECONDS = 60;
    private static final int MAX_LENGTH_CONFIGURATION_SET_NAME = 40;

    @Override
    abstract public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger);

    protected InvocationContext initializeContext(final AmazonWebServicesClientProxy proxy,
                                     final ResourceHandlerRequest<ResourceModel> request,
                                     final CallbackContext callbackContext,
                                     final Logger logger) {
        InvocationContext context = new InvocationContext();
        context.setRequest(request);
        context.setModel(request.getDesiredResourceState());
        context.setOldModel(request.getPreviousResourceState());
        context.setCallbackContext(callbackContext == null ? new CallbackContext() : callbackContext);
        this.log = new LoggerWrapper(logger);

        setModelServerName(context);

        final OpsWorksCmClient opsWorksCmClient = ClientBuilder.getClient();
        this.client = new ClientWrapper(opsWorksCmClient, context.getModel(), context.getOldModel(), proxy, this.log);
        return context;
    }

    private void setModelServerName(InvocationContext context) {
        ResourceModel model = context.getModel();
        ResourceModel oldModel = context.getOldModel();
        if (oldModel != null && !StringUtils.isNullOrEmpty(oldModel.getServerName())) {
            model.setServerName(oldModel.getServerName());
        } else if (StringUtils.isNullOrEmpty(model.getServerName())) {
            log.log("RequestModel doesn't have the server name. Setting it using request identifier and client token");
            model.setServerName(
                    IdentifierUtils.generateResourceIdentifier(
                            context.getRequest().getLogicalResourceIdentifier(),
                            context.getRequest().getClientRequestToken(),
                            MAX_LENGTH_CONFIGURATION_SET_NAME
                    )
            );
        } else if (model.getServerName().length() > MAX_LENGTH_CONFIGURATION_SET_NAME) {
            log.log(String.format("ServerName length was greater than %d characters. Truncating the ServerName", MAX_LENGTH_CONFIGURATION_SET_NAME));
            model.setServerName(model.getServerName().substring(0, MAX_LENGTH_CONFIGURATION_SET_NAME));
        }
    }

    protected void addOutputAttributes(InvocationContext context) {
        final DescribeServersResponse result;
        final String serverName = context.getModel().getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString();
        result = client.describeServer(serverName);
        Server server = result.servers().get(0);
        context.getModel().setEndpoint(server.endpoint());
        context.getModel().setArn(server.serverArn());
    }

    protected ResourceModel generateModelFromServer(Server server, List<Tag> tags) {
        return ResourceModel.builder()
                .backupRetentionCount(server.backupRetentionCount())
                .customDomain(server.customDomain())
                .disableAutomatedBackup(server.disableAutomatedBackup())
                .associatePublicIpAddress(server.associatePublicIpAddress())
                .engine(server.engine())
                .engineModel(server.engineModel())
                .engineVersion(server.engineVersion())
                .instanceProfileArn(server.instanceProfileArn())
                .instanceType(server.instanceType())
                .keyPair(server.keyPair())
                .preferredBackupWindow(server.preferredBackupWindow())
                .preferredMaintenanceWindow(server.preferredMaintenanceWindow())
                .serverName(server.serverName())
                .serviceRoleArn(server.serviceRoleArn())
                .subnetIds(server.subnetIds())
                .endpoint(server.endpoint())
                .arn(server.serverArn())
                .tags(tags)
                .build();
    }

}
