package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisKey;

public interface PullRequestAnalysisJpaRepository
		extends JpaRepository<PullRequestAnalysisEntity, PullRequestAnalysisKey> {

	@Modifying
	@Query(("""
			UPDATE
				PullRequestAnalysis
			SET
				commentId = :commentId
			WHERE
				key.number = :pullRequestNumber AND
				key.repository = :repository AND
				key.projectId = :projectId
			"""))
	void updateCommentId(int commentId, int pullRequestNumber, String repository, String projectId);

	Optional<PullRequestAnalysisEntity> findByKeyNumberAndKeyRepositoryAndKeyProjectId(
			int pullRequestNumber,
			String repository,
			String projectId);

}
