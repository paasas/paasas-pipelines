package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.time.LocalDateTime;

import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysisJobInfo;
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
public class PullRequestAnalysisJobInfoEntity {
	String build;
	String job;
	String pipeline;
	String team;
	LocalDateTime timestamp;

	@Lob
	@Column(length = 16_777_216)
	String url;

	public PullRequestAnalysisJobInfo to() {
		return PullRequestAnalysisJobInfo.builder()
				.build(build)
				.job(job)
				.pipeline(pipeline)
				.team(team)
				.timestamp(timestamp)
				.url(url)
				.build();
	}

	public static PullRequestAnalysisJobInfoEntity from(PullRequestAnalysisJobInfo jobInfo) {
		return PullRequestAnalysisJobInfoEntity.builder()
				.build(jobInfo.getBuild())
				.job(jobInfo.getJob())
				.pipeline(jobInfo.getPipeline())
				.team(jobInfo.getTeam())
				.timestamp(jobInfo.getTimestamp())
				.url(jobInfo.getUrl())
				.build();
	}
}
