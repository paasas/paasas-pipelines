package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
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
@Entity(name = "CloudRunTestReport")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudRunTestReportEntity {
	@EmbeddedId
	DeploymentKey key;

	String image;

	@Lob
	@Column(length = 16_777_216)
	String reportUrl;
	
	String tag;

	@Embedded
	TestGitRevisionEntity testGitRevision;

	@Embedded
	DeploymentInfoEntity testInfo;

	public TestReport to() {
		return TestReport.builder()
				.buildName(key.getBuild())
				.buildUrl(testInfo.getUrl())
				.projectId(testInfo.getProjectId())
				.reportUrl(reportUrl)
				.timestamp(testInfo.getTimestamp())
				.build();
	}

	public static CloudRunTestReportEntity from(RegisterCloudRunTestReport request) {
		return CloudRunTestReportEntity.builder()
				.image(request.getImage())
				.key(DeploymentKey.from(request.getJobInfo()))
				.reportUrl(request.getReportUrl())
				.tag(request.getTag())
				.testGitRevision(TestGitRevisionEntity.from(request.getTestGitRevision()))
				.testInfo(DeploymentInfoEntity.from(request.getJobInfo()))
				.build();
	}
}