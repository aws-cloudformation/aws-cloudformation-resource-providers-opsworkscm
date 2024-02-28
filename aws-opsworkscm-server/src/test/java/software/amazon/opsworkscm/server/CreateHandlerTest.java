package software.amazon.opsworkscm.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerResponse;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.InvalidStateException;
import software.amazon.awssdk.services.opsworkscm.model.LimitExceededException;
import software.amazon.awssdk.services.opsworkscm.model.OpsWorksCmException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.awssdk.services.opsworkscm.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    CreateHandler handler;
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
    private static final String ENDPOINT = "myendpoint.com";
    private static final String SERVER_ARN = "arn:aws:opsworks-cm:us-east-1:123123123123:server/ServerName";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new CreateHandler();

        callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(false)
                .build();

        model = ResourceModel.builder()
                .serverName(SERVER_NAME)
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();
    }

    @Test
    public void testCreateSimpleServer() {
        assertStabilizeSuccess(request);
    }

    @Test
    public void testCreateSimpleServerAllOptions() {
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
    public void testCreateSimpleServerNoName() {
        final String longResourceIdentifier = "OpsWorksCMServerHasAVeryLongResourceId";
        final ResourceModel model = ResourceModel.builder()
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
                .build();

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
    public void testCreateSimpleServerLongName() {
        final ResourceModel model = ResourceModel.builder()
                .serverName("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty")
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
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

    @Test
    public void testCreateServerAlreadyExists() {
        ResourceAlreadyExistsException myException = ResourceAlreadyExistsException.builder().message("Whatever API says").build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnAlreadyExistsException e) {
            assertThat(e.getMessage()).isEqualTo("Resource of type 'OpsWorksCM::Server' with identifier 'ServerName' already exists.");
        }
    }

    @Test
    public void testCreateServerExecuteHandlerFailure() {
        String exceptionMessage = "Handler already worked 8 hours. Nap time!";
        NullPointerException myException = new NullPointerException(exceptionMessage);

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInternalFailureException e) {
            assert (e.getCause() == null || !e.getCause().getMessage().equals(exceptionMessage));
        }
    }

    @Test
    public void testCreateServerStabilizeHandlerFailure() {
        String exceptionMessage = "Handler already worked 8 hours. Nap time!";
        NullPointerException myException = new NullPointerException(exceptionMessage);

        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInternalFailureException e) {
            assert (e.getCause() == null || !e.getCause().getMessage().equals(exceptionMessage));
        }
    }

    @Test
    public void testCreateServerForwardsValidationException() {
        String exceptionMessage = "BadRequest";
        ValidationException myException = ValidationException.builder().message(exceptionMessage).build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: " + exceptionMessage);
        }
    }

    @Test
    public void testCreateServerForwardsOpsWorksCmException() {
        String exceptionMessage = "Cross-account pass role is not allowed.";
        OpsWorksCmException myException = (OpsWorksCmException) OpsWorksCmException.builder().message(exceptionMessage).build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: " + exceptionMessage);
        }
    }

    @Test
    public void testCreateServerForwardsValidationExceptionStabilize() {
        String exceptionMessage = "BadRequest";
        ValidationException myException = ValidationException.builder().message(exceptionMessage).build();

        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: " + exceptionMessage);
        }
    }

    @Test
    public void testCreateServerForwardsLimitExceededException() {
        String exceptionMessage = "Only allowed 1337 servers";
        LimitExceededException myException = LimitExceededException.builder().message(exceptionMessage).build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnServiceLimitExceededException e) {
            assertThat(e.getMessage()).isEqualTo("Limit exceeded for resource of type 'OpsWorksCM::Server'. Reason: " + exceptionMessage);
        }
    }

    @Test
    public void testCreateServerForwardsLimitExceededExceptionStabilize() {
        String exceptionMessage = "Only allowed 1337 servers";
        LimitExceededException myException = LimitExceededException.builder().message(exceptionMessage).build();

        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnServiceLimitExceededException e) {
            assertThat(e.getMessage()).isEqualTo("Limit exceeded for resource of type 'OpsWorksCM::Server'. Reason: " + exceptionMessage);
        }
    }

    @Test
    public void testCreateServerExecuteServiceFailure() {
        InvalidStateException myException = InvalidStateException.builder().message("Service is done for!!1").build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotStabilizedException e) {
            assertThat(e.getMessage()).isEqualTo("Resource of type 'OpsWorksCM::Server' with identifier 'ServerName' did not stabilize.");
        }
    }

    @Test
    public void testCreateServerStabilizeServiceFailure() {
        InvalidStateException myException = InvalidStateException.builder().message("Service is done for!!1").build();

        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotStabilizedException e) {
            assertThat(e.getMessage()).isEqualTo("Resource of type 'OpsWorksCM::Server' with identifier 'ServerName' did not stabilize.");
        }
    }

    @Test
    public void testCreateStabilizeServerNotFound() {
        ResourceNotFoundException myException = ResourceNotFoundException.builder().message("Nani??!").build();

        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).isEqualTo(String.format("Server %s was deleted.", SERVER_NAME));
    }

    @Test
    public void testCreateStabilizeEmptyDescribe() {
        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doReturn(DescribeServersResponse.builder().servers(Collections.emptyList()).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).isEqualTo(String.format("Server %s was deleted.", SERVER_NAME));
    }

    @Test
    public void testCreateStabilizeRetriesOnTransientState() {
        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        String[] transientStates = new String[]{"BACKING_UP", "MODIFYING", "RESTORING", "UNDER_MAINTENANCE", "CREATING"};
        for (String state : transientStates) {
            doReturn(DescribeServersResponse.builder().servers(Server.builder().serverName(SERVER_NAME).status(state).build()).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
            final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);
            assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
            assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }
    }

    @Test
    public void testCreateStabilizeFailsOnNonTransientFailureStates() {
        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        String[] transientStates = new String[]{"UNHEALTHY", "CONNECTION_LOST", "TERMINATED"};
        for (String state : transientStates) {
            String statusReason = "some made up reason";
            doReturn(DescribeServersResponse.builder().servers(
                    Server.builder().serverName(SERVER_NAME).status(state).statusReason(statusReason).build()
            ).build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
            final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);

            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
            assertThat(response.getCallbackContext()).isNotNull();
            assertThat(response.getCallbackContext().isStabilizationStarted()).isTrue();
            assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
            assertThat(response.getMessage()).isEqualTo(String.format("Server %s creation has failed. Server should be HEALTHY, but is %s. With reason: %s",
                    SERVER_NAME, state, statusReason));
            assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotStabilized);
        }
    }

    @Test
    public void testCreateStabilizeRetriesImmediatelyOnFailedDescribe() {
        CallbackContext callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(true)
                .build();

        doReturn(null).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().getStabilizationRetryTimes()).isEqualTo(1);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    }

    private ProgressEvent<ResourceModel, CallbackContext> assertStabilizeSuccess(ResourceHandlerRequest<ResourceModel> request) {
        doReturn(CreateServerResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());
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

        doReturn(getDescribeServerResponse("CREATING")).when(proxy).injectCredentialsAndInvokeV2(any(), any());
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

        doReturn(getDescribeServerResponse("HEALTHY")).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        stabilizeResponse = handler.handleRequest(proxy, request, executeResponse.getCallbackContext(), logger);
        assertThat(stabilizeResponse).isNotNull();
        assertThat(stabilizeResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(stabilizeResponse.getResourceModel().getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(stabilizeResponse.getResourceModel().getArn()).isEqualTo(SERVER_ARN);
        assertThat(stabilizeResponse.getResourceModels()).isNull();
        assertThat(stabilizeResponse.getMessage()).isNull();
        assertThat(stabilizeResponse.getErrorCode()).isNull();

        return executeResponse;
    }

    private DescribeServersResponse getDescribeServerResponse(String status) {
        return DescribeServersResponse.builder().servers(Server.builder()
                .status(status)
                .serverName(SERVER_NAME)
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
                .serverArn(SERVER_ARN)
                .endpoint(ENDPOINT)
                .build()).build();
    }
}
