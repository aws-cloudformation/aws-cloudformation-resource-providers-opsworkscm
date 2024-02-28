package software.amazon.opsworkscm.server;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerResponse;
import software.amazon.awssdk.services.opsworkscm.model.DeleteServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.DeleteServerResponse;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersRequest;
import software.amazon.awssdk.services.opsworkscm.model.DescribeServersResponse;
import software.amazon.awssdk.services.opsworkscm.model.EngineAttribute;
import software.amazon.awssdk.services.opsworkscm.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.ResourceNotFoundException;
import software.amazon.awssdk.services.opsworkscm.model.Tag;
import software.amazon.awssdk.services.opsworkscm.model.TagResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.TagResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.UntagResourceRequest;
import software.amazon.awssdk.services.opsworkscm.model.UntagResourceResponse;
import software.amazon.awssdk.services.opsworkscm.model.UpdateServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.UpdateServerResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.opsworkscm.server.utils.LoggerWrapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.opsworkscm.server.ResourceModel.IDENTIFIER_KEY_SERVERNAME;

@AllArgsConstructor
public class ClientWrapper {

    OpsWorksCmClient client;
    ResourceModel model;
    ResourceModel oldModel;
    AmazonWebServicesClientProxy proxy;
    LoggerWrapper log;

    public DescribeServersResponse describeServer(String serverName) {
        return proxy.injectCredentialsAndInvokeV2(buildDescribeServerRequest(serverName), client::describeServers);
    }

    public ListTagsForResourceResponse listServerTags(String resourceArn) {
        return proxy.injectCredentialsAndInvokeV2(buildListTagsForResourceRequest(resourceArn), client::listTagsForResource);
    }

    public DescribeServersResponse describeAllServers() {
        return proxy.injectCredentialsAndInvokeV2(buildDescribeAllServersRequest(), client::describeServers);
    }

    public DeleteServerResponse deleteServer() {
        return proxy.injectCredentialsAndInvokeV2(buildDeleteServerRequest(), client::deleteServer);
    }

    public CreateServerResponse createServer() {
        return proxy.injectCredentialsAndInvokeV2(buildCreateServerRequest(), client::createServer);
    }

    public TagResourceResponse tagServer() {
        TagResourceRequest request = buildTagResourceRequest();
        if (request != null) {
            return proxy.injectCredentialsAndInvokeV2(request, client::tagResource);
        }
        return null;
    }

    public UntagResourceResponse untagServer() {
        UntagResourceRequest request = buildUntagResourceRequest();
        if (request != null) {
            return proxy.injectCredentialsAndInvokeV2(request, client::untagResource);
        }
        return null;
    }

    public UpdateServerResponse updateServer() {
        return proxy.injectCredentialsAndInvokeV2(buildUpdateServerRequest(), client::updateServer);
    }

    private DescribeServersRequest buildDescribeServerRequest(String serverName) {
        return DescribeServersRequest.builder()
                .serverName(serverName)
                .build();
    }

    private ListTagsForResourceRequest buildListTagsForResourceRequest(String resourceArn) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .build();
    }
    private DescribeServersRequest buildDescribeAllServersRequest() {
        return DescribeServersRequest.builder().build();
    }

    private DeleteServerRequest buildDeleteServerRequest() {
        return DeleteServerRequest.builder()
                .serverName(model.getPrimaryIdentifier().get(IDENTIFIER_KEY_SERVERNAME).toString())
                .build();
    }

    private CreateServerRequest buildCreateServerRequest() {
        final List<EngineAttribute> engineAttributes;
        final List<Tag> tags;
        if (model.getEngineAttributes() != null) {
            engineAttributes = model.getEngineAttributes()
                    .stream().map(attr -> EngineAttribute.builder().name(attr.getName()).value(attr.getValue()).build())
                    .collect(Collectors.toList());
        } else {
            engineAttributes = Collections.emptyList();
        }
        if (model.getTags() != null) {
            tags = model.getTags()
                    .stream().map(t -> Tag.builder().key(t.getKey()).value(t.getValue()).build())
                    .collect(Collectors.toList());
        } else {
            tags = Collections.emptyList();
        }

        return CreateServerRequest.builder()
                .backupId(model.getBackupId())
                .backupRetentionCount(model.getBackupRetentionCount())
                .customDomain(model.getCustomDomain())
                .customCertificate(model.getCustomCertificate())
                .customPrivateKey(model.getCustomPrivateKey())
                .disableAutomatedBackup(model.getDisableAutomatedBackup())
                .associatePublicIpAddress(model.getAssociatePublicIpAddress())
                .engine(model.getEngine())
                .engineAttributes(engineAttributes)
                .engineModel(model.getEngineModel())
                .engineVersion(model.getEngineVersion())
                .instanceProfileArn(model.getInstanceProfileArn())
                .instanceType(model.getInstanceType())
                .tags(tags)
                .keyPair(model.getKeyPair())
                .preferredBackupWindow(model.getPreferredBackupWindow())
                .preferredMaintenanceWindow(model.getPreferredMaintenanceWindow())
                .securityGroupIds(model.getSecurityGroupIds())
                .serverName(model.getServerName())
                .serviceRoleArn(model.getServiceRoleArn())
                .subnetIds(model.getSubnetIds())
                .build();
    }

    private UpdateServerRequest buildUpdateServerRequest() {
        return UpdateServerRequest.builder()
                .disableAutomatedBackup(model.getDisableAutomatedBackup())
                .preferredBackupWindow(model.getPreferredBackupWindow())
                .backupRetentionCount(model.getBackupRetentionCount())
                .serverName(model.getServerName())
                .preferredMaintenanceWindow(model.getPreferredMaintenanceWindow())
                .build();
    }

    private TagResourceRequest buildTagResourceRequest() {
        if (model.getTags() == null) {
            return null;
        }
        List<Tag> tagList = model.getTags().stream().map(t -> Tag.builder().key(t.getKey()).value(t.getValue()).build()).collect(Collectors.toList());

        if (tagList.size() == 0) {
            return null;
        }

        return TagResourceRequest.builder()
                .tags(tagList)
                .resourceArn(getResourceArn())
                .build();
    }

    private UntagResourceRequest buildUntagResourceRequest() {
        List<String> tagKeyDiff;
        Stream<String> oldTags;
        if (oldModel.getTags() == null) {
            return null;
        }

        oldTags = oldModel.getTags().stream().map(t -> t.getKey());
        if (model.getTags() == null) {
            tagKeyDiff = oldTags.collect(Collectors.toList());
        } else {
            List<String> newTagKeys = model.getTags().stream().map(t -> t.getKey()).collect(Collectors.toList());
            tagKeyDiff = oldTags.filter(k -> !newTagKeys.contains(k)).collect(Collectors.toList());
        }

        if (tagKeyDiff.size() == 0) {
            return null;
        }

        return UntagResourceRequest.builder()
                .tagKeys(tagKeyDiff)
                .resourceArn(getResourceArn())
                .build();
    }

    private String getResourceArn() {
        DescribeServersResponse describeServersResponse = describeServer(model.getServerName());
        if (describeServersResponse != null && describeServersResponse.hasServers()) {
            return describeServer(model.getServerName()).servers().get(0).serverArn();
        }
        throw ResourceNotFoundException.builder().message(String.format("Server with name %s does not exist.", model.getServerName())).build();
    }

}
