package software.amazon.opsworkscm.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.opsworkscm.OpsWorksCmClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {

    static OpsWorksCmClient getClient() {
        return OpsWorksCmClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

    static OpsWorksCmClient getClient(final String region) {
        return OpsWorksCmClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(region))
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .endpointOverride(URI.create(String.format("https://opsworks-cm.%s.amazonaws.com", region)))
                .build();
    }
}
