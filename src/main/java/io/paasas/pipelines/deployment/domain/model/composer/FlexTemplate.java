package io.paasas.pipelines.deployment.domain.model.composer;

import io.paasas.pipelines.deployment.domain.model.app.RegistryType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FlexTemplate {
	String name;
	String gcsPath;
	String image;
	String imageTag;
	String metadataFile;
	RegistryType imageRegistryType;
}
