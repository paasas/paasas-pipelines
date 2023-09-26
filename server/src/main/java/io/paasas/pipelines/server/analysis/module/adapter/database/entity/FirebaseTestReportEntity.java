package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Entity(name = "FirebaseTestReport")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseTestReportEntity {
	@EmbeddedId
	DeploymentKey key;

	@Embedded
	GitRevisionEntity gitRevision;

	boolean successful;

	@Embedded
	DeploymentInfoEntity testInfo;

	@Column(length = 65535)
	String reportUrl;

	@Embedded
	TestGitRevisionEntity testGitRevision;

	public TestReport to() {
		return TestReport.builder()
				.buildName(key.getBuild())
				.buildUrl(testInfo.getUrl())
				.projectId(testInfo.getProjectId())
				.reportUrl(reportUrl)
				.successful(successful)
				.timestamp(testInfo.getTimestamp())
				.build();
	}

	public static FirebaseTestReportEntity from(RegisterFirebaseAppTestReport request) {
		return FirebaseTestReportEntity.builder()
				.gitRevision(GitRevisionEntity.from(request.getGitRevision()))
				.key(DeploymentKey.from(request.getJobInfo()))
				.reportUrl(request.getReportUrl())
				.successful(request.isSuccessful())
				.testGitRevision(TestGitRevisionEntity.from(request.getTestGitRevision()))
				.testInfo(DeploymentInfoEntity.from(request.getJobInfo()))
				.build();
	}
}