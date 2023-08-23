package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseAppDeploymentEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseFirebaseAppDeploymentRepository implements FirebaseAppDeploymentRepository {
	FirebaseAppDeploymentJpaRepository repository;

	@Override
	public List<FirebaseAppDeployment> find(FindDeploymentRequest findRequest) {
		return repository.find(findRequest.gitPath(), findRequest.gitUri(), findRequest.gitTag())
				.stream()
				.map(FirebaseAppDeploymentEntity::to)
				.toList();
	}

	@Override
	public void registerDeployment(RegisterFirebaseAppDeployment registerFirebaseAppDeployment) {
		repository.save(FirebaseAppDeploymentEntity.from(registerFirebaseAppDeployment));
	}

}
