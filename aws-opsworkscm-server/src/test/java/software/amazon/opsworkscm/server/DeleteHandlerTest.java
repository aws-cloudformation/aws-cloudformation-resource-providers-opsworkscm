package software.amazon.opsworkscm.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.opsworkscm.model.DeleteServerResponse;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.awssdk.services.opsworkscm.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    DeleteHandler handler;
    CallbackContext callbackContext;
    ResourceModel model;
    ResourceHandlerRequest<ResourceModel> request;

    private static final String REGION = "us-east-1";
    private static final String RESOURCE_IDENTIFIER = "MyOpsWorksCMServer";
    private static final String SERVER_NAME = "ServerName";
    private static final String ENGINE = "Chef";
    private static final String ENGINE_VERSION = "12";
    private static final String ENGINE_MODEL = "Single";
    private static final String INSTANCE_TYPE = "m5.xlarge";
    private static final String INSTANCE_PROFILE = "arn:aws:iam::012345678912:instance-profile/aws-opsworks-cm-ec2-role";
    private static final String SERVICE_ROLE = "arn:aws:iam::012345678912:role/service-role/aws-opsworks-cm-service-role";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);

        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new DeleteHandler();

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
    }

    @Test
    public void testSimpleDelete() {
        assertStabilizeSuccess(request);
    }

    @Test
    public void testDeleteSimpleServerNoName() {
        final String longResourceIdentifier = "OpsWorksCMServerHasAVeryLongResourceId";
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(longResourceIdentifier)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = assertStabilizeSuccess(request);
        String actualServerName = response.getResourceModel().getServerName();
        assertThat(actualServerName).startsWith(longResourceIdentifier.substring(0, 27) + "-");
        assertThat(actualServerName.length()).isEqualTo(40);
    }

    @Test
    public void testDeleteSimpleServerAllCreateOptions() {
        final ResourceModel model = ResourceModel.builder()
                .serverName(SERVER_NAME)
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
                .keyPair("myKey")
                .associatePublicIpAddress(true)
                .backupRetentionCount(10)
                .customCertificate("MyCert")
                .customDomain("myDomain")
                .preferredMaintenanceWindow("window")
                .preferredBackupWindow("window2")
                .securityGroupIds(Collections.singletonList("sg-123"))
                .disableAutomatedBackup(true)
                .endpoint("asd.com")
                .tags(Collections.singletonList(Tag.builder().key("k").value("v").build()))
                .engineAttributes(Collections.singletonList(EngineAttribute.builder().name("N").value("V").build()))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        assertStabilizeSuccess(request);
    }

    @Test
    public void testDeleteSimpleServerLongName() {
        final ResourceModel model = ResourceModel.builder()
                .serverName("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response = assertStabilizeSuccess(request);
        String actualServerName = response.getResourceModel().getServerName();
        assertThat(actualServerName).isEqualTo("OpsWorksCMServerHasAVeryLongServerNameIn");
        assertThat(actualServerName.length()).isEqualTo(40);
    }

//    @Test
//    public void testExecuteResourceNotFound() {
//        doThrow(ResourceNotFoundException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
//        final ProgressEvent<ResourceModel, CallbackContext> response
//                = handler.handleRequest(proxy, request, callbackContext, logger);
//        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
//    }

    @Test
    public void testStabilizeResourceNotFound() {
        callbackContext.setStabilizationStarted(true);
        doThrow(ResourceNotFoundException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testStabilizeDescribeReturnsNoServers() {
        callbackContext.setStabilizationStarted(true);
        doReturn(DescribeServersResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testExecuteOperationStillInProgress() {
        doThrow(ValidationException.builder().message("Cannot delete the server '" + SERVER_NAME + "'. The current operation on the server is still in progress." +
                " (Service: OpsWorksCm, Status Code: 400, Request ID: " + request.getClientRequestToken() + ")").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    }

    @Test
    public void testStabilizeOperationStillInProgress() {
        callbackContext.setStabilizationStarted(true);
        doThrow(ValidationException.builder().message("Cannot delete the server '" + SERVER_NAME + "'. The current operation on the server is still in progress." +
                " (Service: OpsWorksCM, Status Code: 400, Request ID: " + request.getClientRequestToken() + ")").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

    }

    @Test
    public void testExecuteOperationStillInProgressTooLongServerName() {
        request.getDesiredResourceState().setServerName("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty");
        doThrow(ValidationException.builder().message("Cannot delete the server 'OpsWorksCMServerHasAVeryLongServerNameIn'. The current operation on the server is still in progress." +
                " (Service: AWSOpsWorksCM; Status Code: 400; Error Code: ValidationException; Request ID: " + request.getClientRequestToken() + ")").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    }

    @Test
    public void testStabilizeOperationStillInProgressTooLongServerName() {
        callbackContext.setStabilizationStarted(true);
        request.getDesiredResourceState().setServerName("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty");
        doThrow(ValidationException.builder().message("Cannot delete the server 'OpsWorksCMServerHasAVeryLongServerNameIn'. The current operation on the server is still in progress." +
                " (Service: AWSOpsWorksCM; Status Code: 400; Error Code: ValidationException; Request ID: " + request.getClientRequestToken() + ")").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

    }

    @Test
    public void testExecuteValidationException() {
        String exceptionMessage = "come on..";
        doThrow(ValidationException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: " + exceptionMessage);
        }
    }

    @Test
    public void testStabilizeValidationException() {
        callbackContext.setStabilizationStarted(true);
        String exceptionMessage = "come on..";
        doThrow(ValidationException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: " + exceptionMessage);
        }
    }

    @Test
    public void testStabilizeInvalidStateException() {
        doThrow(InvalidStateException.builder().message("come on..").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotStabilizedException e) {
            assertThat(e.getMessage()).isEqualTo("Resource of type 'OpsWorksCM::Server' with identifier 'ServerName' did not stabilize.");
        }
    }

    @Test
    public void testExecuteInvalidStateException() {
        callbackContext.setStabilizationStarted(true);
        doThrow(InvalidStateException.builder().message("come on..").build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotStabilizedException e) {
            assertThat(e.getMessage()).isEqualTo("Resource of type 'OpsWorksCM::Server' with identifier 'ServerName' did not stabilize.");
        }
    }

    @Test
    public void testExecuteUnknownExceptionNotForwarded() {
        doThrow(new ArrayIndexOutOfBoundsException("come on..")).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInternalFailureException e) {
            assertThat(e.getMessage()).isEqualTo("Internal error occurred.");
        }
    }

    @Test
    public void testStabilizeUnknownExceptionNotForwarded() {
        callbackContext.setStabilizationStarted(true);
        String exceptionMessage = "come on..";
        doThrow(new ArrayIndexOutOfBoundsException(exceptionMessage)).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInternalFailureException e) {
            assertThat(e.getMessage()).isEqualTo("Internal error occurred.");
        }
    }

    @Test
    public void testStabilizeRetryStatus() {
        String[] transientStates = new String[]{"BACKING_UP", "MODIFYING", "RESTORING", "UNDER_MAINTENANCE", "CREATING", "DELETING", "CONNECTION_LOST", "TERMINATED"};
        for (String state : transientStates) {
            callbackContext = CallbackContext.builder()
                    .stabilizationRetryTimes(0)
                    .stabilizationStarted(false)
                    .build();
            assertStabilizeSuccess(state, request);
        }
    }

    @Test
    public void testStabilizeBadStatus() {
        String state = "FAILED";
        String statusReason = "customers fault";

        doReturn(DeleteServerResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> executeResponse
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(executeResponse).isNotNull();
        assertThat(executeResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

        doReturn(DescribeServersResponse.builder().servers(Server.builder().serverName(SERVER_NAME).status(state).statusReason(statusReason).build()).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        ProgressEvent<ResourceModel, CallbackContext> stabilizeResponse
                = handler.handleRequest(proxy, request, executeResponse.getCallbackContext(), logger);
        assertThat(stabilizeResponse).isNotNull();
        assertThat(stabilizeResponse.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(stabilizeResponse.getMessage()).isEqualTo("Server " + SERVER_NAME + " deletion has failed with reason: " + statusReason);
        assertThat(stabilizeResponse.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void testStabilizeDescribeResultNull() {
        callbackContext.setStabilizationStarted(true);
        doReturn(null).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        ProgressEvent<ResourceModel, CallbackContext> stabilizeResponse
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(stabilizeResponse).isNotNull();
        assertThat(stabilizeResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(stabilizeResponse.getCallbackContext().getStabilizationRetryTimes()).isEqualTo(1);
        assertThat(stabilizeResponse.getCallbackDelaySeconds()).isEqualTo(0);
    }

    private ProgressEvent<ResourceModel, CallbackContext> assertStabilizeSuccess(ResourceHandlerRequest<ResourceModel> request) {
        return assertStabilizeSuccess("DELETING", request);
    }

    private ProgressEvent<ResourceModel, CallbackContext> assertStabilizeSuccess(String stabilizeStatus, ResourceHandlerRequest<ResourceModel> request) {
        doReturn(DeleteServerResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> executeResponse
                = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(executeResponse).isNotNull();
        assertThat(executeResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(executeResponse.getCallbackContext()).isNotNull();
        assertThat(executeResponse.getCallbackContext().isStabilizationStarted()).isTrue();
        assertThat(executeResponse.getCallbackContext().getStabilizationRetryTimes()).isEqualTo(0);
        assertThat(executeResponse.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(executeResponse.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(executeResponse.getResourceModels()).isNull();
        assertThat(executeResponse.getMessage()).isNull();
        assertThat(executeResponse.getErrorCode()).isNull();

        doReturn(DescribeServersResponse.builder().servers(Server.builder().serverName(SERVER_NAME).status(stabilizeStatus).build()).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        ProgressEvent<ResourceModel, CallbackContext> stabilizeResponse
                = handler.handleRequest(proxy, request, executeResponse.getCallbackContext(), logger);
        assertThat(stabilizeResponse).isNotNull();
        assertThat(stabilizeResponse.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(stabilizeResponse.getCallbackContext()).isNotNull();
        assertThat(stabilizeResponse.getCallbackContext().isStabilizationStarted()).isTrue();
        assertThat(stabilizeResponse.getCallbackContext().getStabilizationRetryTimes()).isEqualTo(1);
        assertThat(stabilizeResponse.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(stabilizeResponse.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(stabilizeResponse.getResourceModels()).isNull();
        assertThat(stabilizeResponse.getMessage()).isNull();
        assertThat(stabilizeResponse.getErrorCode()).isNull();

        doThrow(ResourceNotFoundException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        stabilizeResponse = handler.handleRequest(proxy, request, executeResponse.getCallbackContext(), logger);
        assertThat(stabilizeResponse).isNotNull();
        assertThat(stabilizeResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(stabilizeResponse.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(stabilizeResponse.getResourceModels()).isNull();
        assertThat(stabilizeResponse.getMessage()).isNull();
        assertThat(stabilizeResponse.getErrorCode()).isNull();

        return executeResponse;
    }

}
