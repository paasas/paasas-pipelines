package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.time.LocalDateTime;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import io.paasas.pipelines.server.analysis.domain.model.DeploymentInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeploymentInfoEntity {
	String projectId;
	LocalDateTime timestamp;

	@Column(length = 65535)
	String url;

	public DeploymentInfo to(DeploymentKey key) {
		return DeploymentInfo.builder()
				.build(key.getBuild())
				.job(key.getJob())
				.pipeline(key.getPipeline())
				.projectId(projectId)
				.team(key.getTeam())
				.timestamp(timestamp)
				.url(url)
				.build();
	}

	public static DeploymentInfoEntity from(JobInfo jobInfo) {
		return DeploymentInfoEntity.builder()
				.projectId(jobInfo.getProjectId())
				.timestamp(LocalDateTime.now())
				.url(jobInfo.getUrl())
				.build();
	}
}
