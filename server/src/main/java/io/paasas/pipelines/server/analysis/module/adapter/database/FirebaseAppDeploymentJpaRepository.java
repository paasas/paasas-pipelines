package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.DeploymentKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseAppDeploymentEntity;

public interface FirebaseAppDeploymentJpaRepository extends JpaRepository<FirebaseAppDeploymentEntity, DeploymentKey> {
	@Query("""
			SELECT
				firebaseAppDeployment
			FROM
				FirebaseAppDeployment firebaseAppDeployment
			WHERE
				((gitRevision.path is null AND :path is null) OR (gitRevision.path = :path)) AND
				CONCAT('git@github.com:', gitRevision.repositoryOwner, '/', gitRevision.repository, '.git') = :uri AND
				gitRevision.tag = :tag""")
	List<FirebaseAppDeploymentEntity> find(String path, String uri, String tag);
}
