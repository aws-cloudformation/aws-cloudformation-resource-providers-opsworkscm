package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerResponse;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;
        final ResourceModel model = request.getDesiredResourceState();

        if (!context.isCreateServerStarted()) {

        }

        if (!context.isCreateServerStabilized()) {

        }

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateServerResponse createServer(final ResourceModel model, final AmazonWebServicesClientProxy proxy) {
        final OpsWorksCmClient opsWorksCmClient = ClientBuilder.getClient();
        final CreateServerRequest request = Translator.createServerRequest(model);
        try {
            return proxy.injectCredentialsAndInvokeV2(request, opsWorksCmClient::createServer);
        } catch (ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);
        }
    }
}
