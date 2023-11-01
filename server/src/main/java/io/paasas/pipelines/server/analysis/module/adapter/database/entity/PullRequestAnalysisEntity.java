package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString.Exclude;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(name = "PullRequestAnalysis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PullRequestAnalysisEntity {
	@EmbeddedId
	PullRequestAnalysisKey key;

	Integer commentId;
	String commit;
	String commitAuthor;

	@Lob
	@Exclude
	@Column(length = 16_777_216)
	String manifest;

	@Embedded
	PullRequestAnalysisJobInfoEntity jobInfo;

	public PullRequestAnalysis to(
			List<CloudRunAnalysis> cloudRun,
			FirebaseAppAnalysis firebase,
			List<TerraformAnalysis> terraform) {
		return PullRequestAnalysis.builder()
				.cloudRun(cloudRun)
				.commentId(commentId)
				.commit(commit)
				.commitAuthor(commitAuthor)
				.firebase(firebase)
				.jobInfo(jobInfo.to())
				.manifest(manifest)
				.projectId(key.getProjectId())
				.pullRequestNumber(key.getNumber())
				.terraform(terraform)
				.build();
	}

	public static PullRequestAnalysisEntity from(PullRequestAnalysis pullRequestAnalysis) {
		return PullRequestAnalysisEntity.builder()
				.commentId(pullRequestAnalysis.getCommentId())
				.commit(pullRequestAnalysis.getCommit())
				.commitAuthor(pullRequestAnalysis.getCommitAuthor())
				.key(PullRequestAnalysisKey.builder()
						.number(pullRequestAnalysis.getPullRequestNumber())
						.projectId(pullRequestAnalysis.getProjectId())
						.repository(pullRequestAnalysis.getRepository())
						.build())
				.jobInfo(PullRequestAnalysisJobInfoEntity.from(pullRequestAnalysis.getJobInfo()))
				.manifest(pullRequestAnalysis.getManifest())
				.build();
	}
}
