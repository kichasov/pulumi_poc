package myproject;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.core.v1.Secret;
import com.pulumi.kubernetes.helm.v3.Release;
import com.pulumi.kubernetes.helm.v3.ReleaseArgs;
import com.pulumi.kubernetes.helm.v3.inputs.RepositoryOptsArgs;
import com.pulumi.resources.CustomResourceOptions;

import java.util.Base64;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            var cassandraSecret = Secret.get("cassandraSecret", Output.of("k8ssandra-operator/cassandra-cluster-superuser"), null);

            var jaeger = new Release("jaeger", ReleaseArgs.builder()
                    .name("jaeger")
                    .createNamespace(true)
                    .chart("qubership-jaeger")
                    .namespace("jaeger")
                    .repositoryOpts(RepositoryOptsArgs.builder().repo("https://ildarminaev.github.io/jaeget-helm-test").build())
                    .version("0.20.2")
                    .values(new HashMap<>() {{

                        put("jaeger", new HashMap<>() {{
                            put("prometheusMonitoringDashboard", false);
                            put("prometheusMonitoring", false);
                            put("storage", Map.of("type", "cassandra"));
                        }});

                        put("collector", Map.of("imagePullSecrets[0]", Map.of("name", "ghcr-io-secret"), "ingress", Map.of("install", true, "host", "collector.jaeger.localhost.localdomain")));

                        put("query", Map.of("ingress", Map.of("install", true, "host", "query.jaeger.localhost.localdomain")));

                        put("cassandraSchemaJob", Map.of("host", "cassandra-cluster-dc1-service.k8ssandra-operator.svc.cluster.local", "existingSecret", false, "datacenter", "dc1", "username", cassandraSecret.data().applyValue(username -> new String(Base64.getDecoder().decode(username.get("username")))), "password", cassandraSecret.data().applyValue(password -> new String(Base64.getDecoder().decode(password.get("password"))))));

                        put("readinessProbe", Map.of("resources", Map.of("limits", Map.of("memory", "200Mi", "cpu", "200m"), "requests", Map.of("memory", "100Mi", "cpu", "100m"))));

                    }})
                    .build()
                    , CustomResourceOptions.builder()
                    .dependsOn(cassandraSecret)
                    .build());
        });
    }
}
