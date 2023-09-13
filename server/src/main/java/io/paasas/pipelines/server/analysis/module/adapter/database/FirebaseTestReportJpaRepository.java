package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.paasas.pipelines.server.analysis.module.adapter.database.entity.DeploymentKey;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseTestReportEntity;

public interface FirebaseTestReportJpaRepository extends JpaRepository<FirebaseTestReportEntity, DeploymentKey> {
	@Query("""
			SELECT
				firebaseTestReport
			FROM
				FirebaseTestReport firebaseTestReport
			WHERE
				( (gitRevision.path IS NULL AND :path IS NULL) OR (gitRevision.path = :path)) AND
				gitRevision.repository = :repository AND
				( (gitRevision.tag IS NULL AND :tag IS NULL) OR (gitRevision.tag = :tag)) AND
				( (gitRevision.commit IS NULL AND :commit IS NULL) OR (gitRevision.commit = :commit))""")
	List<FirebaseTestReportEntity> find(String path, String repository, String tag, String commit);
}
