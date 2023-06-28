package io.paasas.pipelines.deployment.domain.model.app;

import java.util.List;
import java.util.Map;

import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class App {
	List<String> cloudSqlInstances;
	Map<String, String> env;
	String image;
	Integer maxReplicas;
	Integer minReplicas;
	String name;
	Integer port;
	RegistryType registryType;
	Resources resources;
	Map<String, String> secretEnv;
	List<SecretVolume> secretVolumes;
	String serviceAccount;
	Probe startupProbe;
	List<String> subdomains;
	String tag;
	GitWatcher tests;
	String vpcAccessConnector;
}
