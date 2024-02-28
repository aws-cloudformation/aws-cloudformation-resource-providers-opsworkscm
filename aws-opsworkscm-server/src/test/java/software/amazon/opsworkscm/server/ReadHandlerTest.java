package software.amazon.opsworkscm.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersRequest;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.OpsWorksCmException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;


@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Captor
    private ArgumentCaptor<AwsRequest> requestCaptor;

    @Mock
    private Logger logger;

    ReadHandler handler;
    CallbackContext callbackContext;
    ResourceModel responseModel;
    Server server;

    private static final String REGION = "us-east-1";
    private static final String RESOURCE_IDENTIFIER = "OpsWorksCMServerHasAVeryLongResourceId";
    private static final String RESOURCE_IDENTIFIER_WITH_DIGIT = "01234";
    private static final String SERVER_NAME = "ServerName";
    private static final String ENGINE = "Chef";
    private static final String ENGINE_VERSION = "12";
    private static final String ENGINE_MODEL = "Single";
    private static final String INSTANCE_TYPE = "m5.xlarge";
    private static final String INSTANCE_PROFILE = "arn:aws:iam::012345678912:instance-profile/aws-opsworks-cm-ec2-role";
    private static final String SERVICE_ROLE = "arn:aws:iam::012345678912:role/service-role/aws-opsworks-cm-service-role";
    private static final String ENDPOINT = "myendpoint.com";
    private static final String SERVER_ARN = "arn:aws:opsworks-cm:us-east-1:123123123123:server/ServerName";
    private static final List<Tag> TAGS = Arrays.asList(new Tag("value", "key"), new Tag("value2", "key2"));

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new ReadHandler();

        callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(false)
                .build();

        server = Server.builder()
                .serverName(SERVER_NAME).status("HEALTHY").serverName(SERVER_NAME)
                .engine(ENGINE)
                .engineVersion(ENGINE_VERSION)
                .engineModel(ENGINE_MODEL)
                .instanceProfileArn(INSTANCE_PROFILE)
                .serviceRoleArn(SERVICE_ROLE)
                .instanceType(INSTANCE_TYPE)
                .serverArn(SERVER_ARN)
                .securityGroupIds("security-group-1", "security-group-2")
                .endpoint(ENDPOINT).build();

        responseModel = ResourceModel.builder()
                .backupRetentionCount(server.backupRetentionCount())
                .disableAutomatedBackup(server.disableAutomatedBackup())
                .associatePublicIpAddress(server.associatePublicIpAddress())
                .engine(server.engine())
                .engineModel(server.engineModel())
                .instanceProfileArn(server.instanceProfileArn())
                .instanceType(server.instanceType())
                .preferredBackupWindow(server.preferredBackupWindow())
                .preferredMaintenanceWindow(server.preferredMaintenanceWindow())
                .serverName(server.serverName())
                .serviceRoleArn(server.serviceRoleArn())
                .subnetIds(server.subnetIds())
                .securityGroupIds(server.securityGroupIds())
                .endpoint(server.endpoint())
                .arn(server.serverArn())
                .tags(TAGS)
                .build();

        final DescribeServersResponse describeServersResponse = DescribeServersResponse.builder().servers(server).build();

        lenient().doReturn(describeServersResponse).when(proxy).injectCredentialsAndInvokeV2(any(DescribeServersRequest.class), any());
    }

    public ResourceHandlerRequest<ResourceModel> createRequest(String serverName) {
        ResourceModel model = ResourceModel.builder()
                .serverName(serverName)
                .tags(TAGS)
                .build();

        return ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();
    }

    @Test
    public void testDescribeSimpleSuccess() {
        ResourceHandlerRequest<ResourceModel> request = createRequest(SERVER_NAME);
        String serverNameInRequest = assertDescribeSuccess(request);
        assertThat(serverNameInRequest).isEqualTo(SERVER_NAME);
    }

    @Test
    public void testDescribeSimpleServerNoName() {
        ResourceHandlerRequest<ResourceModel> request = createRequest(null);
        String serverNameInRequest = assertDescribeSuccess(request);
        assertThat(serverNameInRequest).startsWith(RESOURCE_IDENTIFIER.substring(0, 27) + "-");
    }

    @Test
    public void testDescribeSimpleServerNoName_WithNumericLogicalId() {
        ResourceHandlerRequest<ResourceModel> request = createRequest(null);
        request.setLogicalResourceIdentifier(RESOURCE_IDENTIFIER_WITH_DIGIT);
        String serverNameInRequest = assertDescribeSuccess(request);
        assertThat(serverNameInRequest).startsWith(SERVER_NAME_PREFIX + RESOURCE_IDENTIFIER_WITH_DIGIT + "-");
    }

    @Test
    public void testDescribeSimpleServerNoName_WithEmptyLogicalId() {
        ResourceHandlerRequest<ResourceModel> request = createRequest(null);
        request.setLogicalResourceIdentifier("");
        String serverNameInRequest = assertDescribeSuccess(request);
        assertThat(serverNameInRequest).startsWith(SERVER_NAME_PREFIX);
    }

    @Test
    public void testDescribeSimpleServerLongName() {
        ResourceHandlerRequest<ResourceModel> request = createRequest("OpsWorksCMServerHasAVeryLongServerNameInFactItIsTooLongBecauseTheMaxIsFourty");
        String serverNameInRequest = assertDescribeSuccess(request);
        assertThat(serverNameInRequest).isEqualTo("OpsWorksCMServerHasAVeryLongServerNameIn");
    }

    @Test
    public void testDescribeServerNotFound() {
        String exceptionMessage = "Nani??!";
        software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException opsWorksCmResourceNotFoundException = software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException.builder().message(exceptionMessage).build();

        doThrow(opsWorksCmResourceNotFoundException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        ResourceHandlerRequest<ResourceModel> request = createRequest(SERVER_NAME);
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotFoundException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(exceptionMessage);
        }
    }

    @Test
    public void testOtherException() {
        String exceptionMessage = "Cross-account pass role is not allowed.";
        OpsWorksCmException myException = (OpsWorksCmException) OpsWorksCmException.builder().message(exceptionMessage).build();
        ResourceHandlerRequest<ResourceModel> request = createRequest(SERVER_NAME);

        doThrow(myException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnGeneralServiceException e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred during operation '" + exceptionMessage + "'.");
        }
    }

    private String assertDescribeSuccess(ResourceHandlerRequest<ResourceModel> request) {
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(responseModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        assertThat(requestCaptor.getAllValues().get(0)).isInstanceOf(DescribeServersRequest.class);
        return ((DescribeServersRequest) requestCaptor.getAllValues().get(0)).serverName();
    }
}
