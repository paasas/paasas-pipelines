package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudRunAnalysisKey implements Serializable {
	@ManyToOne
	PullRequestAnalysisEntity pullRequestAnalysis;

	String serviceName;
}