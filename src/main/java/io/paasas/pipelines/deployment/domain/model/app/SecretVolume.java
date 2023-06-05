package io.paasas.pipelines.deployment.domain.model.app;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class SecretVolume {
	String volumeName;
	String mountPath;
	String secretName;
	List<String> paths;
}
