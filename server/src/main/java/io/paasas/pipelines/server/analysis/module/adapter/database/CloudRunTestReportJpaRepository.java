package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunTestReportEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.DeploymentKey;

public interface CloudRunTestReportJpaRepository extends JpaRepository<CloudRunTestReportEntity, DeploymentKey> {
	List<CloudRunTestReportEntity> findByImageAndTag(String image, String tag, Sort sort);
}
