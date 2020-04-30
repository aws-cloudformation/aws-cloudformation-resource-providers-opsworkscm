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
import software.amazon.awssdk.services.opsworkscm.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ClientWrapper {

    OpsWorksCmClient client;
    ResourceModel model;
    AmazonWebServicesClientProxy proxy;
    Logger logger;

    public DescribeServersResponse describeServer() {
        return proxy.injectCredentialsAndInvokeV2(buildDescribeServerRequest(), client::describeServers);
    }

    public DeleteServerResponse deleteServer() {
        return proxy.injectCredentialsAndInvokeV2(buildDeleteServerRequest(), client::deleteServer);
    }

    public CreateServerResponse createServer() {
        return proxy.injectCredentialsAndInvokeV2(buildCreateServerRequest(), client::createServer);
    }

    private DescribeServersRequest buildDescribeServerRequest() {
        return DescribeServersRequest.builder()
                .serverName(model.getServerName())
                .build();
    }

    private DeleteServerRequest buildDeleteServerRequest() {
        return DeleteServerRequest.builder()
                .serverName(model.getServerName())
                .build();
    }

    private CreateServerRequest buildCreateServerRequest() {
        final List<EngineAttribute> engineAttributes;
        final List<Tag> tags;
        if(model.getEngineAttributes() != null) {
            engineAttributes = model.getEngineAttributes()
                    .stream().map(attr -> EngineAttribute.builder().name(attr.getName()).value(attr.getValue()).build())
                    .collect(Collectors.toList());
        } else {
            engineAttributes = Collections.emptyList();
        }
        if(model.getTags() != null) {
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

}
