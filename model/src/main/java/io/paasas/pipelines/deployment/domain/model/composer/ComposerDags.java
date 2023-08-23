package io.paasas.pipelines.deployment.domain.model.composer;

import java.util.List;

import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class ComposerDags {
	GitWatcher git;
	List<FlexTemplate> flexTemplates;
}
