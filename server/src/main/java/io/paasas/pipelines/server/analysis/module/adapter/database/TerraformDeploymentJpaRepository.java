package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.DeploymentKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformDeploymentEntity;

public interface TerraformDeploymentJpaRepository extends JpaRepository<TerraformDeploymentEntity, DeploymentKey> {
	@Query("""
			SELECT
				terraformDeployment
			FROM
				TerraformDeployment terraformDeployment
			WHERE
				( ( gitRevision.path IS NULL AND :path IS NULL) OR gitRevision.path = :path) AND
				CONCAT('git@github.com:', gitRevision.repository, '.git') = :uri AND
				gitRevision.tag = :tag""")
	List<TerraformDeploymentEntity> find(String path, String uri, String tag);

	Page<TerraformDeploymentEntity> findByPackageNameAndDeploymentInfoProjectId(
			String packageName,
			String projectId,
			Pageable pageable);
}
