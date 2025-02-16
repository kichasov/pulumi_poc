package myproject;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.apiextensions.CustomResource;
import com.pulumi.kubernetes.apiextensions.CustomResourceArgs;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.core.v1.Secret;
import com.pulumi.kubernetes.helm.v3.Release;
import com.pulumi.kubernetes.helm.v3.ReleaseArgs;
import com.pulumi.kubernetes.helm.v3.inputs.RepositoryOptsArgs;
import com.pulumi.kubernetes.meta.v1.inputs.ObjectMetaArgs;
import com.pulumi.kubernetes.yaml.v2.ConfigFile;
import com.pulumi.kubernetes.yaml.v2.ConfigFileArgs;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.command.local.Command;
import com.pulumi.command.local.CommandArgs;

import java.util.*;

public class App {

    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            var certManager = new Release("certManager", ReleaseArgs.builder()
                    .createNamespace(true)
                    .name("cert-manager")
                    .chart("cert-manager")
                    .namespace("cert-manager")
                    .repositoryOpts(RepositoryOptsArgs.builder()
                            .repo("https://charts.jetstack.io")
                            .build())
                    .version("v1.16.3")
                    .values(Map.of(
                            "prometheus", Map.of(
                                    "enabled", true),
                            "crds", Map.of(
                                    "enabled", true)))
                    .build());


            var cassandraOperator = new Release("cassandraOperator", ReleaseArgs.builder()
                    .createNamespace(true)
                    .name("k8ssandra-operator")
                    .chart("k8ssandra-operator")
                    .namespace("k8ssandra-operator")
                    .repositoryOpts(RepositoryOptsArgs.builder()
                            .repo("https://helm.k8ssandra.io/stable")
                            .build())
                    .version("1.21.0")
                    .build(),
                    CustomResourceOptions.builder()
                            .dependsOn(certManager)
                            .build());

/*            var cassandra = new ConfigFile("cassandra", ConfigFileArgs.builder()
                    .file("crs/cassandra-cr.yaml")
                    .build(),
                    ComponentResourceOptions.builder()
                            .dependsOn(cassandraOperator)
                            .build());*/

            var cassandra = new CustomResource("cassandra", CustomResourceArgs.builder()
                    .apiVersion("k8ssandra.io/v1alpha1")
                    .kind("K8ssandraCluster")
                    .metadata(ObjectMetaArgs.builder()
                            .name("cassandra-cluster")
                            .namespace("k8ssandra-operator")
                            .build())
                    .otherFields(Map.of(
                            "spec", Map.of(
                                    "cassandra", Map.of(
                                            "serverVersion", "4.0.1",
                                            "datacenters", List.of(Map.of(
                                                    "metadata", Map.of(
                                                            "name", "dc1"
                                                    ),
                                                    "size", 1,
                                                    "storageConfig", Map.of(
                                                            "cassandraDataVolumeClaimSpec", Map.of(
                                                                    "storageClassName", "local-path",
                                                                    "accessModes", List.of("ReadWriteOnce"),
                                                                    "resources", Map.of(
                                                                            "requests", Map.of(
                                                                                    "storage", "5Gi"
                                                                            )
                                                                    )
                                                            )
                                                    ),
                                                    "config", Map.of(
                                                            "jvmOptions", Map.of(
                                                                    "heapSize", "512M"
                                                            )
                                                    )
                                            )
                                            )
                                    )
                            )
                    )
                    )
                    .build(),
                    CustomResourceOptions.builder()
                            .dependsOn(cassandraOperator)
                            .build());

            var waitForCassandra = new Command("waitForCassandra", CommandArgs.builder()
                    .create("kubectl wait --for=condition=CassandraInitialized K8ssandraCluster/cassandra-cluster -n k8ssandra-operator --timeout=300s")
                    .build(),
                    CustomResourceOptions.builder()
                            .dependsOn(cassandra)
                            .build());

            var cassandraSecret = Secret.get("cassandraSecret", Output.of("k8ssandra-operator/cassandra-cluster-superuser"),
                    CustomResourceOptions.builder()
                            .dependsOn(waitForCassandra)
                            .build());

            var jaeger = new Release("jaeger", ReleaseArgs.builder()
                    .name("jaeger")
                    .createNamespace(true)
                    .chart("qubership-jaeger")
                    .namespace("jaeger")
                    .repositoryOpts(RepositoryOptsArgs.builder()
                            .repo("https://ildarminaev.github.io/jaeget-helm-test")
                            .build())
                    .version("0.20.2")
                    .values(Map.of(
                            "jaeger", Map.of(
                                    "prometheusMonitoringDashboard", false,
                                    "prometheusMonitoring", false,
                                    "storage", Map.of(
                                            "type", "cassandra")
                            ),
                            "collector", Map.of(
                                    "imagePullSecrets[0]", Map.of("name", "ghcr-io-secret"),
                                    "ingress", Map.of(
                                            "install", true,
                                            "host", "collector.jaeger.localhost.localdomain")
                            ),
                            "query", Map.of(
                                    "ingress", Map.of(
                                            "install", true,
                                            "host", "query.jaeger.localhost.localdomain")),
                            "cassandraSchemaJob", Map.of(
                                    "host", "cassandra-cluster-dc1-service.k8ssandra-operator.svc.cluster.local",
                                    "existingSecret", false,
                                    "datacenter", "dc1",
                                    "username", cassandraSecret.data().applyValue(username -> new String(Base64.getDecoder().decode(username.get("username")))),
                                    "password", cassandraSecret.data().applyValue(password -> new String(Base64.getDecoder().decode(password.get("password"))))),

                            "readinessProbe", Map.of(
                                    "resources", Map.of(
                                            "limits", Map.of(
                                                    "memory", "200Mi",
                                                    "cpu", "200m"),
                                            "requests", Map.of(
                                                    "memory", "100Mi",
                                                    "cpu", "100m")
                                    )
                            )
                    )
                    )
                    .timeout(900)
                    .build(),
                    CustomResourceOptions.builder()
                    .dependsOn(cassandraSecret)
                    .build());

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

            var robotShopNamespace = Namespace.get("robot-shop", Output.of("robot-shop"), null);

            var otelCollector = new ConfigFile("otel-collector", ConfigFileArgs.builder()
                    .file("crs/otel-collector-cr.yaml")
                    .build(),
                    ComponentResourceOptions.builder()
                            .dependsOn(Arrays.asList(jaeger, openTelemetry, robotShopNamespace))
                            .build());

            var otelInstrumentation = new ConfigFile("otel-instrumentation", ConfigFileArgs.builder()
                    .file("crs/otel-instrumentation-cr.yaml")
                    .build(),
                    ComponentResourceOptions.builder()
                            .dependsOn(Arrays.asList(jaeger, openTelemetry, robotShopNamespace))
                            .build());
        });
    }
}
