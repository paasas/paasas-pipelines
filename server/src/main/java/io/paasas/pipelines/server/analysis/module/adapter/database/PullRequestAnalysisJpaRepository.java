package io.paasas.pipelines.server.analysis.module.adapter.database;

import org.springframework.data.jpa.repository.JpaRepository;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestAnalysisEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.PullRequestKey;

public interface PullRequestAnalysisJpaRepository extends JpaRepository<PullRequestAnalysisEntity, PullRequestKey> {

}
