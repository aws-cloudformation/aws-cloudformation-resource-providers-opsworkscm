package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.OpsWorksCmException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.opsworkscm.server.ResourceModel.IDENTIFIER_KEY_SERVERNAME;

public class ReadHandler extends BaseOpsWorksCMHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        InvocationContext context = initializeContext(proxy, request, callbackContext, logger);

        final String serverName = context.getModel().getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString();
        context.getCallbackContext().incrementRetryTimes();

        log.info(String.format("Calling Describe Servers for ServerName %s", serverName));

        try {
            return ProgressEvent.defaultSuccessHandler(generateModel(context));
        } catch (final software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException e) {
            log.error(String.format("Server %s was not found.", serverName), e);
            throw new CfnNotFoundException(e);
        } catch (final OpsWorksCmException e) {
            log.error(String.format("Server %s was not found.", serverName), e);
            throw new CfnGeneralServiceException(e.getMessage());
        } catch (Exception e) {
            log.error(String.format("ReadHandler failure during delete-server for %s.", serverName), e);
            throw new CfnInternalFailureException(e);
        }
    }

}
