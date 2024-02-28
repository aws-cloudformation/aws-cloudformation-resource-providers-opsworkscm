package software.amazon.opsworkscm.server;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersRequest;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Server;
import software.amazon.awssdk.services.opsworkscm.model.TagResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.TagResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.UntagResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.UntagResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.UpdateServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.UpdateServerResponse;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    UpdateHandler handler;
    CallbackContext callbackContext;
    ResourceModel model;
    ResourceModel oldModel;
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
        handler = new UpdateHandler();

        callbackContext = CallbackContext.builder()
                .stabilizationRetryTimes(0)
                .stabilizationStarted(false)
                .build();

        model = ResourceModel.builder()
                .backupRetentionCount(3)
                .tags(ImmutableList.of(Tag.builder().key("kitne").value("murgiyaa").build()))
                .build();

        oldModel = ResourceModel.builder()
                .backupRetentionCount(4)
                .tags(ImmutableList.of(Tag.builder().key("chjcken").value("kurry").build()))
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(oldModel)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        mockApiCalls();
    }

    private void mockApiCalls() {
        lenient().doReturn(getDescribeServerResponse("HEALTHY"))
                .when(proxy).injectCredentialsAndInvokeV2(any(DescribeServersRequest.class), any());
        lenient().doReturn(UpdateServerResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(UpdateServerRequest.class), any());
        lenient().doReturn(TagResourceResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(TagResourceRequest.class), any());
        lenient().doReturn(UntagResourceResponse.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(UntagResourceRequest.class), any());
    }

    @Test
    public void simpleUpdate() {
        assertStabilizeSuccess(request);
    }

    @Test
    public void updateNoData() {
        request.setDesiredResourceState(ResourceModel.builder().build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void updateNoPreviousData() {
        request.setPreviousResourceState(ResourceModel.builder().build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void tagResourceResourceNotFound() {
        String exceptionMessage = "Batan!";
        callbackContext.setUpdateTagComplete(false);
        callbackContext.setUpdateServerComplete(true);
        doThrow(ResourceNotFoundException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(TagResourceRequest.class), any());
        assertResourceNotFound();
    }

    @Test
    public void untagResourceResourceNotFound() {
        String exceptionMessage = "Batan!";
        callbackContext.setUpdateTagComplete(false);
        callbackContext.setUpdateServerComplete(true);
        doThrow(ResourceNotFoundException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(UntagResourceRequest.class), any());
        assertResourceNotFound();
    }

    @Test
    public void describeServersResourceNotFound() {
        String exceptionMessage = "Batan!";
        callbackContext.setUpdateTagComplete(false);
        callbackContext.setUpdateServerComplete(false);
        doThrow(ResourceNotFoundException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(DescribeServersRequest.class), any());
        assertResourceNotFound();
    }

    @Test
    public void updateServerResourceNotFound() {
        String exceptionMessage = "Batan!";
        callbackContext.setUpdateTagComplete(true);
        callbackContext.setUpdateServerComplete(false);
        doThrow(ResourceNotFoundException.builder().message(exceptionMessage).build()).when(proxy).injectCredentialsAndInvokeV2(any(UpdateServerRequest.class), any());
        assertResourceNotFound();
    }

    @Test
    public void updateServerSomeExceptionNotForwarded() {
        String exceptionMessage = "Batan!";
        callbackContext.setUpdateTagComplete(true);
        callbackContext.setUpdateServerComplete(false);
        doThrow(new RuntimeException(exceptionMessage)).when(proxy).injectCredentialsAndInvokeV2(any(UpdateServerRequest.class), any());
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnInternalFailureException e) {
            assertThat(e.getMessage()).isEqualTo("Internal error occurred.");
        }
    }

    @Test
    public void testUpdateSimpleServerAllCreateOptions() {
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
        final ResourceModel oldModel = ResourceModel.builder()
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
                .preferredBackupWindow("11window24")
                .securityGroupIds(Collections.singletonList("sg-123"))
                .disableAutomatedBackup(true)
                .endpoint("asd.com")
                .tags(Collections.singletonList(Tag.builder().key("11k1").value("11v1").build()))
                .engineAttributes(Collections.singletonList(EngineAttribute.builder().name("N").value("V").build()))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(oldModel)
                .logicalResourceIdentifier(RESOURCE_IDENTIFIER)
                .clientRequestToken(UUID.randomUUID().toString())
                .region(REGION)
                .build();

        assertStabilizeSuccess(request);
    }

    @Test
    public void testNoTags() {
        request.setDesiredResourceState(ResourceModel.builder().backupRetentionCount(5).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void testNoTagsPreviousModel() {
        request.setPreviousResourceState(ResourceModel.builder().backupRetentionCount(5).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void testOnlyTags() {
        request.setDesiredResourceState(ResourceModel.builder().tags(Collections.singletonList(Tag.builder().key(" ").value("").build())).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void testOnlyTagsPreviousModel() {
        request.setPreviousResourceState(ResourceModel.builder().tags(Collections.singletonList(Tag.builder().key(" ").value("").build())).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void testEmptyTags() {
        request.setDesiredResourceState(ResourceModel.builder().tags(Collections.emptyList()).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void testEmptyTagsPreviousModel() {
        request.setPreviousResourceState(ResourceModel.builder().tags(Collections.emptyList()).build());
        assertStabilizeSuccess(request);
    }

    @Test
    public void tagsAreDiffedOnUntag() {
        verifyUntagDiff(ImmutableList.of("keepMe", "keepMe2"), ImmutableList.of("removeMe", "removeMe2"));
    }

    @Test
    public void noUntagOnNoPreviousTags() {
        List<Tag> newTags = new ArrayList<>();
        List<Tag> oldTags = new ArrayList<>();
        newTags.add(Tag.builder().key("keepMe").value("").build());
        request.setDesiredResourceState(ResourceModel.builder().tags(newTags).build());
        request.setPreviousResourceState(ResourceModel.builder().tags(oldTags).build());
        assertStabilizeSuccess(request);
        ArgumentCaptor<AwsRequest> accountArgumentCaptor = forClass(AwsRequest.class);
        verify(proxy, atLeastOnce()).injectCredentialsAndInvokeV2(accountArgumentCaptor.capture(), any());
        Optional<AwsRequest> request = accountArgumentCaptor.getAllValues().stream().filter(s -> s.getClass().equals(UntagResourceRequest.class)).findFirst();
        assertThat(request.isPresent()).isFalse();
    }

    @Test
    public void noUntagOnNoTagsAtAll() {
        List<Tag> newTags = new ArrayList<>();
        List<Tag> oldTags = new ArrayList<>();
        request.setDesiredResourceState(ResourceModel.builder().tags(newTags).build());
        request.setPreviousResourceState(ResourceModel.builder().tags(oldTags).build());
        assertStabilizeSuccess(request);
        ArgumentCaptor<AwsRequest> accountArgumentCaptor = forClass(AwsRequest.class);
        verify(proxy, atLeastOnce()).injectCredentialsAndInvokeV2(accountArgumentCaptor.capture(), any());
        Optional<AwsRequest> request = accountArgumentCaptor.getAllValues().stream().filter(s -> s.getClass().equals(UntagResourceRequest.class)).findFirst();
        assertThat(request.isPresent()).isFalse();
    }

    @Test
    public void tagsAreDiffedOnUntagNoNewTags() {
        verifyUntagDiff(Collections.emptyList(), ImmutableList.of("removeMe", "removeMe2"));
    }

    @Test
    public void newTagsAreApplied() {
        List<Tag> newTags = ImmutableList.of(
                Tag.builder().key("hello").value("world").build(),
                Tag.builder().key("hello2").value("the_world").build()
        );
        List<Tag> oldTags = ImmutableList.of(
                Tag.builder().key("doesnot").value("matter").build(),
                Tag.builder().key("sekaiwa").value("zenbudame").build()
        );
        verifytagsApplied(newTags, oldTags);
    }

    @Test
    public void newTagsAreAppliedNoPreviousTags() {
        List<Tag> newTags = ImmutableList.of(
                Tag.builder().key("hello").value("world").build(),
                Tag.builder().key("hello2").value("the_world").build()
        );
        List<Tag> oldTags = Collections.emptyList();
        verifytagsApplied(newTags, oldTags);
    }

    @Test
    public void newTagsAreAppliedNoNewTags() {
        List<Tag> newTags = Collections.emptyList();
        List<Tag> oldTags = ImmutableList.of(
                Tag.builder().key("doesnot").value("matter").build(),
                Tag.builder().key("sekaiwa").value("zenbudame").build()
        );
        verifytagsApplied(newTags, oldTags);
    }

    private ProgressEvent<ResourceModel, CallbackContext> assertStabilizeSuccess(ResourceHandlerRequest<ResourceModel> request) {
        ProgressEvent<ResourceModel, CallbackContext> response;

        response = handler.handleRequest(proxy, request, callbackContext, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        response = handler.handleRequest(proxy, request, response.getCallbackContext(), logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().isUpdateServerComplete()).isTrue();
        assertThat(response.getCallbackContext().isUpdateTagComplete()).isTrue();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        response = handler.handleRequest(proxy, request, response.getCallbackContext(), logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getEndpoint()).isEqualTo(ENDPOINT);
        assertThat(response.getResourceModel().getArn()).isEqualTo(SERVER_ARN);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        return response;
    }

    private void assertResourceNotFound() {
        try {
            handler.handleRequest(proxy, request, callbackContext, logger);
        } catch (CfnNotFoundException e) {
            assertThat(e.getMessage()).startsWith("Resource of type 'OpsWorksCM::Server' with identifier '");
            assertThat(e.getMessage()).endsWith("' was not found.");
        }
    }

    private void verifyUntagDiff(List<String> keepKeys, List<String> removeKeys) {
        List<Tag> newTags = new ArrayList<>();
        List<Tag> oldTags = new ArrayList<>();
        removeKeys.forEach(k -> oldTags.add(Tag.builder().key(k).value("").build()));
        keepKeys.forEach(k -> {
            oldTags.add(Tag.builder().key(k).value("").build());
            newTags.add(Tag.builder().key(k).value("").build());
        });
        request.setDesiredResourceState(ResourceModel.builder().tags(newTags).build());
        request.setPreviousResourceState(ResourceModel.builder().tags(oldTags).build());
        assertStabilizeSuccess(request);
        ArgumentCaptor<AwsRequest> accountArgumentCaptor = forClass(AwsRequest.class);
        verify(proxy, atLeastOnce()).injectCredentialsAndInvokeV2(accountArgumentCaptor.capture(), any());
        Optional<AwsRequest> request = accountArgumentCaptor.getAllValues().stream().filter(s -> s.getClass().equals(UntagResourceRequest.class)).findFirst();
        assertThat(request.isPresent());
        UntagResourceRequest untagRequest = (UntagResourceRequest) request.get();
        removeKeys.forEach(k -> assertThat(untagRequest.tagKeys()).contains(k));
        keepKeys.forEach(k -> assertThat(untagRequest.tagKeys()).doesNotContain(k));
    }

    private void verifytagsApplied(List<Tag> newTags, List<Tag> oldTags) {
        request.setDesiredResourceState(ResourceModel.builder().tags(newTags).build());
        request.setPreviousResourceState(ResourceModel.builder().tags(oldTags).build());
        assertStabilizeSuccess(request);
        ArgumentCaptor<AwsRequest> accountArgumentCaptor = forClass(AwsRequest.class);
        verify(proxy, atLeastOnce()).injectCredentialsAndInvokeV2(accountArgumentCaptor.capture(), any());
        Optional<AwsRequest> request = accountArgumentCaptor.getAllValues().stream().filter(s -> s.getClass().equals(TagResourceRequest.class)).findFirst();
        if (!newTags.isEmpty()) {
            assertThat(request.isPresent()).isTrue();
            TagResourceRequest tagRequest = (TagResourceRequest) request.get();
            List<String> newTagKeys = newTags.stream().map(Tag::getKey).collect(Collectors.toList());
            List<String> newTagValues = newTags.stream().map(Tag::getValue).collect(Collectors.toList());
            List<String> actualTagKeys = tagRequest.tags().stream().map(software.amazon.awssdk.services.opsworkscm.model.Tag::key).collect(Collectors.toList());
            List<String> actualTagValues = tagRequest.tags().stream().map(software.amazon.awssdk.services.opsworkscm.model.Tag::value).collect(Collectors.toList());
            assertThat(newTagKeys.equals(actualTagKeys));
            assertThat(newTagValues.equals(actualTagValues));
        }
    }

    private DescribeServersResponse getDescribeServerResponse(String status) {
        ResourceModel requestModel = request.getDesiredResourceState();
        return DescribeServersResponse.builder().servers(Server.builder()
                .status(status)
                .serverName(requestModel.getServerName())
                .endpoint(ENDPOINT)
                .serverArn(SERVER_ARN)
                .build()).build();
    }
}
