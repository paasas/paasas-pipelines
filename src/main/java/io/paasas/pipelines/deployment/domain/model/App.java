package io.paasas.pipelines.deployment.domain.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class App {
	String name;
	String image;
	Map<String, String> env;
	Integer port;
	Map<String, String> secretEnv;
	List<String> subdomains;
}
