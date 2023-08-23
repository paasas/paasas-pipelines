package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

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
				gitRevision.path = :path AND
				CONCAT('git@github.com:', gitRevision.repositoryOwner, '/', gitRevision.repository, '.git') = :uri AND
				gitRevision.tag = :tag""")
	List<TerraformDeploymentEntity> find(String path, String uri, String tag);
}
