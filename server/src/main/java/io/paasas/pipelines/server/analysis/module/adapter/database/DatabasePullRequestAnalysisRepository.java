package io.paasas.pipelines.server.analysis.module.adapter.database;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.port.backend.PullRequestAnalysisRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabasePullRequestAnalysisRepository implements PullRequestAnalysisRepository {
	PullRequestAnalysisJpaRepository repository;

	@Override
	@Transactional
	public void save(PullRequestAnalysis pullRequestAnalysis) {
		repository.save(PullRequestAnalysisEntity.from(pullRequestAnalysis));
	}

}
