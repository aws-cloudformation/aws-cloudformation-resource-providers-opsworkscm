package software.amazon.opsworkscm.server;

import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    ReadHandler handler;
    CallbackContext callbackContext;
    ResourceModel model;
    ResourceHandlerRequest<ResourceModel> request;

    private static final String REGION = "us-east-1";
    private static final String RESOURCE_IDENTIFIER = "MyOpsWorksCMServer";
    private static final String SERVER_NAME = "ServerName";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new ReadHandler();

        callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(false)
                .build();

        model = ResourceModel.builder()
                .serverName(SERVER_NAME)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        lenient().doReturn(DescribeServersResponse.builder().servers(Server.builder().serverName(SERVER_NAME).status("HEALTHY").build()).build())
                .when(proxy).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void testDescribeSimpleSuccess() {
        assertDescribeSuccess(request);
    }

    @Test
    public void testDescribeSimpleServerNoName() {
        final String longResourceIdentifier = "OpsWorksCMServerHasAVeryLongResourceId";
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(longResourceIdentifier)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = assertDescribeSuccess(request);
        String actualServerName = response.getResourceModel().getServerName();
        assertThat(actualServerName).startsWith(longResourceIdentifier.substring(0, 27) + "-");
        assertThat(actualServerName.length()).isEqualTo(40);
    }

    @Test
    public void testDescribeSimpleServerLongName() {
        final ResourceModel model = ResourceModel.builder()
                .serverName("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);
        String actualServerName = response.getResourceModel().getServerName();
        assertThat(actualServerName).isEqualTo("OpsWorksCMServerHasAVeryLongServerNameIn");
        assertThat(actualServerName.length()).isEqualTo(40);
    }

    @Test
    public void testDescribeServerNotFound() {
        software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException opsWorksCmResourceNotFoundException = software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException.builder().message("Nani??!").build();

        doThrow(opsWorksCmResourceNotFoundException).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        assertThrows(ResourceNotFoundException.class,
                ()->{
                    handler.handleRequest(proxy, request, callbackContext, logger);
                });
    }

    private ProgressEvent<ResourceModel, CallbackContext> assertDescribeSuccess(ResourceHandlerRequest<ResourceModel> request) {
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        return response;
    }
}
