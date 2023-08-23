package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Embeddable
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformAnalysisKey implements Serializable {
	@ManyToOne
	PullRequestAnalysisEntity pullRequestAnalysis;

	String packageName;
}