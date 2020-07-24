package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.OpsWorksCmException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static software.amazon.opsworkscm.server.ResourceModel.IDENTIFIER_KEY_SERVERNAME;

public class UpdateHandler extends BaseOpsWorksCMHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        InvocationContext context = initializeContext(proxy, request, callbackContext, logger);
        String serverName = context.getModel().getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString();

        try {
            if (!context.getCallbackContext().isUpdateTagComplete()) {
                return updateTags(context);
            }
            if (!context.getCallbackContext().isUpdateServerComplete()) {
                return updateServer(context);
            }
            Server server = client.describeServer(context.getModel().getServerName()).servers().get(0);
            List<Tag> tags = context.getRequest().getDesiredResourceState().getTags();
            return ProgressEvent.defaultSuccessHandler(generateModelFromServer(server, tags));
        } catch (ResourceNotFoundException e) {
            log.error(String.format("ResourceNotFoundException during update of server %s, with message %s", serverName, e.getMessage()), e);
            throw new CfnNotFoundException(resourceTypeName, serverName);
        } catch (InvalidStateException e) {
            log.error(String.format("InvalidStateException during update of server %s, with message %s", serverName, e.getMessage()), e);
            throw new CfnNotStabilizedException(resourceTypeName, serverName);
        } catch (OpsWorksCmException e) {
            log.error(String.format("ValidationException during update of server %s, with message %s", serverName, e.getMessage()), e);
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(String.format("UpdateHandler failure during delete-server for %s.", serverName), e);
            throw new CfnInternalFailureException(e);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(InvocationContext context) {
        client.untagServer();
        client.tagServer();
        context.getCallbackContext().setUpdateTagComplete(true);
        return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), NO_CALLBACK_DELAY, context.getModel());
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateServer(InvocationContext context) {
        client.updateServer();
        context.getCallbackContext().setUpdateServerComplete(true);
        return ProgressEvent.defaultInProgressHandler(context.getCallbackContext(), NO_CALLBACK_DELAY, context.getModel());
    }
}
