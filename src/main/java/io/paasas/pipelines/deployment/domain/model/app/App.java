package io.paasas.pipelines.deployment.domain.model.app;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class App {
	Map<String, String> annotations;
	Map<String, String> env;
	String image;
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
}
