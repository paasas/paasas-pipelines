package io.paasas.pipelines.deployment.domain.model.composer;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FlexTemplate {
	String gcsPath;
	String image;
	String metadataFile;
}
