package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.paasas.pipelines.server.analysis.domain.model.RegisterTerraformDeployment;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
import io.paasas.pipelines.server.analysis.module.adapter.database.DatabaseObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(name = "TerraformDeployment")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformDeploymentEntity {
	@EmbeddedId
	DeploymentKey key;

	@Embedded
	DeploymentInfoEntity deploymentInfo;

	@Embedded
	GitRevisionEntity gitRevision;

	String packageName;

	@Lob
	@Column(length = 16_777_216)
	String params;

	public TerraformDeployment to() {
		try {
			return TerraformDeployment.builder()
					.deploymentInfo(deploymentInfo.to(key))
					.gitRevision(gitRevision.to())
					.params(DatabaseObjectMapper.OBJECT_MAPPER.readValue(
							params,
							new TypeReference<Map<String, String>>() {
							}))
					.build();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static TerraformDeploymentEntity from(RegisterTerraformDeployment registerTerraformDeployment) {
		try {
			return TerraformDeploymentEntity.builder()
					.deploymentInfo(DeploymentInfoEntity.from(registerTerraformDeployment.getJobInfo()))
					.key(DeploymentKey.from(registerTerraformDeployment.getJobInfo()))
					.gitRevision(GitRevisionEntity.from(registerTerraformDeployment.getGitRevision()))
					.packageName(registerTerraformDeployment.getPackageName())
					.params(DatabaseObjectMapper.OBJECT_MAPPER
							.writeValueAsString(registerTerraformDeployment.getParams()))
					.build();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
