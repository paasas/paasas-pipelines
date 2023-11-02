package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseAppDeploymentEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseTestReportEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseFirebaseAppDeploymentRepository implements FirebaseAppDeploymentRepository {
	FirebaseAppDeploymentJpaRepository repository;
	FirebaseTestReportJpaRepository testReportRepository;

	@Override
	public List<FirebaseAppDeployment> find(FindDeploymentRequest findRequest, Sort sort) {
		return repository.find(findRequest.gitPath(), findRequest.gitUri(), findRequest.gitTag(), sort)
				.stream()
				.map(this::to)
				.toList();
	}

	@Override
	public void registerDeployment(RegisterFirebaseAppDeployment registerFirebaseAppDeployment) {
		repository.save(FirebaseAppDeploymentEntity.from(registerFirebaseAppDeployment));
	}

	private FirebaseAppDeployment to(FirebaseAppDeploymentEntity deployment) {
		return deployment.to(testReportRepository
				.find(
						deployment.getGitRevision().getPath(),
						deployment.getGitRevision().getRepository(),
						deployment.getGitRevision().getTag(),
						deployment.getGitRevision().getCommit())
				.stream()
				.map(FirebaseTestReportEntity::to)
				.toList());
	}
}
