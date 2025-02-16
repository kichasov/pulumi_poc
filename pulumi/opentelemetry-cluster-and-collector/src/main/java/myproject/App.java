package myproject;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.core.v1.Namespace;
import com.pulumi.kubernetes.helm.v3.Release;
import com.pulumi.kubernetes.yaml.v2.ConfigFile;
import com.pulumi.kubernetes.yaml.v2.ConfigFileArgs;
import com.pulumi.resources.ComponentResourceOptions;

import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

            var jaeger = Release.get("jaeger", Output.of("jaeger/jaeger"), null);
            var opentelemetry = Release.get("opentelemetry", Output.of("opentelemetry-operator-system/opentelemetry-operator"), null);
            var robotShopNamespace = Namespace.get("robot-shop", Output.of("robot-shop"), null);

            var otelCollector = new ConfigFile("otel-collector", ConfigFileArgs.builder()
                    .file("crs/otel-collector-cr.yaml")
                    .build(),
                    ComponentResourceOptions.builder()
                            .dependsOn(Arrays.asList(jaeger, opentelemetry, robotShopNamespace))
                            .build());

            var otelInstrumentation = new ConfigFile("otel-instrumentation", ConfigFileArgs.builder()
                    .file("crs/otel-instrumentation-cr.yaml")
                    .build(),
                    ComponentResourceOptions.builder()
                            .dependsOn(otelCollector)
                            .build());
        });
    }
}
