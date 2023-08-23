package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.time.LocalDateTime;

import io.paasas.pipelines.server.analysis.domain.model.DeploymentInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
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

	@Lob
	@Column(length = 16_777_216)
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

	public static DeploymentInfoEntity from(DeploymentInfo deploymentInfo) {
		return DeploymentInfoEntity.builder()
				.projectId(deploymentInfo.getProjectId())
				.timestamp(deploymentInfo.getTimestamp())
				.url(deploymentInfo.getUrl())
				.build();
	}
}
