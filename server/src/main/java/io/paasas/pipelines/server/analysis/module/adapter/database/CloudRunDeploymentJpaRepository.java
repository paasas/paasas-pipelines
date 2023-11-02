package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunDeploymentEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.DeploymentKey;

public interface CloudRunDeploymentJpaRepository extends JpaRepository<CloudRunDeploymentEntity, DeploymentKey> {
	Page<CloudRunDeploymentEntity> findByDeploymentInfoProjectIdAndApp(
			String projectId,
			String app,
			Pageable pageable);

	List<CloudRunDeploymentEntity> findByImageAndTag(String image, String tag, Sort sort);
}
