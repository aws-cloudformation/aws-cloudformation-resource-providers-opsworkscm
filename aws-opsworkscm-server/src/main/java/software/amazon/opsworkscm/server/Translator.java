package software.amazon.opsworkscm.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.opsworkscm.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerRequest;
import software.amazon.awssdk.services.opsworkscm.model.CreateServerResponse;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
final class Translator {

    static CreateServerRequest createServerRequest(final ResourceModel model) {
        return CreateServerRequest.builder()
                .backupId(model.getBackupId())
                .backupRetentionCount(model.getBackupRetentionCount())
                .customDomain(model.getCustomDomain())
                .customCertificate(model.getCustomCertificate())
                .customPrivateKey(model.getCustomPrivateKey())
                .disableAutomatedBackup(model.getDisableAutomatedBackup())
                .associatePublicIpAddress(model.getAssociatePublicIpAddress())
                .engine(model.getEngine())
                // .engineAttributes(model.getEngineAttributes())
                .engineModel(model.getEngineModel())
                .engineVersion(model.getEngineVersion())
                .instanceProfileArn(model.getInstanceProfileArn())
                .instanceType(model.getInstanceType())
                // .tags(model.getTags())
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