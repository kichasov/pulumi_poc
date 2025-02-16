package myproject;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.helm.v3.Release;
import com.pulumi.kubernetes.helm.v3.ReleaseArgs;
import com.pulumi.kubernetes.helm.v3.inputs.RepositoryOptsArgs;
import com.pulumi.resources.CustomResourceOptions;

import java.util.List;
import java.util.Map;


public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            var certManager = Release.get("cert-manager", Output.of("cert-manager/cert-manager"), null);

            var openTelemetry = new Release("opentelemetry-operator", ReleaseArgs.builder()
                    .name("opentelemetry-operator")
                    .namespace("opentelemetry-operator-system")
                    .createNamespace(true)
                    .chart("opentelemetry-operator")
                    .repositoryOpts(RepositoryOptsArgs.builder().repo("https://open-telemetry.github.io/opentelemetry-helm-charts").build())
                    .version("0.79.0")
                    .values(Map.of(
                                    "manager", Map.of(
                                            "collectorImage", Map.of(
                                                    "repository", "otel/opentelemetry-collector-contrib"),
                                            "extraArgs", List.of("--enable-go-instrumentation=true", "--enable-nginx-instrumentation=true")
                                    )
                            )
                    )
                    .build(),
                    CustomResourceOptions.builder()
                    .dependsOn(certManager)
                    .build());

        });
    }
}