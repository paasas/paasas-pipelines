package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.TerraformDeploymentEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseTerraformDeploymentRepository implements TerraformDeploymentRepository {
	TerraformDeploymentJpaRepository repository;

	@Override
	public List<TerraformDeployment> find(FindDeploymentRequest findRequest) {
		return repository.find(findRequest.gitPath(), findRequest.gitUri(), findRequest.gitTag())
				.stream()
				.map(TerraformDeploymentEntity::to)
				.toList();
	}

	@Override
	public void registerDeployment(RegisterTerraformDeployment registerTerraformDeployment) {
		repository.save(TerraformDeploymentEntity.from(registerTerraformDeployment));
	}

}
