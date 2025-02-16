package myproject;

import com.pulumi.Pulumi;
import com.pulumi.kubernetes.yaml.v2.ConfigFile;
import com.pulumi.kubernetes.yaml.v2.ConfigFileArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {

                var cassandra = new ConfigFile("cassandra", ConfigFileArgs.builder()
                        .file("crs/cassandra-cr.yaml")
                        .build());

        });
    }
}
