package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.paasas.pipelines.deployment.domain.model.firebase.Npm;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;
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
@Entity(name = "FirebaseAppDeployment")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseAppDeploymentEntity {
	@EmbeddedId
	DeploymentKey key;

	@Lob
	@Column(length = 16_777_216)
	String config;

	@Embedded
	DeploymentInfoEntity deploymentInfo;

	@Embedded
	GitRevisionEntity gitRevision;

	@Lob
	@Column(length = 16_777_216)
	String npm;

	public FirebaseAppDeployment to() {
		try {
			return FirebaseAppDeployment.builder()
					.config(config)
					.deploymentInfo(deploymentInfo.to(key))
					.gitRevision(gitRevision.to())
					.npm(DatabaseObjectMapper.OBJECT_MAPPER.readValue(npm, Npm.class))
					.build();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static FirebaseAppDeploymentEntity from(RegisterFirebaseAppDeployment registerFirebaseAppDeployment) {
		try {
			return FirebaseAppDeploymentEntity.builder()
					.config(registerFirebaseAppDeployment.getConfig())
					.deploymentInfo(DeploymentInfoEntity.from(registerFirebaseAppDeployment.getJobInfo()))
					.gitRevision(GitRevisionEntity.from(registerFirebaseAppDeployment.getGitRevision()))
					.key(DeploymentKey.from(registerFirebaseAppDeployment.getJobInfo()))
					.npm(DatabaseObjectMapper.OBJECT_MAPPER.writeValueAsString(registerFirebaseAppDeployment.getNpm()))
					.build();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
