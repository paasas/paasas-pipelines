package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.DeploymentInfo;
import io.paasas.pipelines.server.analysis.domain.model.JobInfo;
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
public class DeploymentKey {
	String team;
	String pipeline;
	String job;
	String build;

	public DeploymentInfo to(DeploymentInfoEntity entity) {
		return DeploymentInfo.builder()
				.build(build)
				.job(job)
				.pipeline(pipeline)
				.projectId(entity.getProjectId())
				.team(team)
				.timestamp(entity.getTimestamp())
				.url(entity.getUrl())
				.build();
	}

	public static DeploymentKey from(JobInfo jobInfo) {
		return DeploymentKey.builder()
				.build(jobInfo.getBuild())
				.job(jobInfo.getJob())
				.pipeline(jobInfo.getPipeline())
				.team(jobInfo.getTeam())
				.build();
	}

}
