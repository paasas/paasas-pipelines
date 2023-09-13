package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
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
@Entity(name = "CloudRunTestReport")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudRunTestReportEntity {
	@EmbeddedId
	DeploymentKey key;

	@Embedded
	DeploymentInfoEntity testInfo;

	String image;
	String reportUrl;
	String tag;

	public TestReport to() {
		return TestReport.builder()
				.buildUrl(testInfo.getUrl())
				.projectId(testInfo.getProjectId())
				.reportUrl(reportUrl)
				.build();
	}

	public static CloudRunTestReportEntity from(RegisterCloudRunTestReport request) {
		return CloudRunTestReportEntity.builder()
				.image(request.getImage())
				.key(DeploymentKey.from(request.getJobInfo()))
				.reportUrl(request.getReportUrl())
				.tag(request.getTag())
				.testInfo(DeploymentInfoEntity.from(request.getJobInfo()))
				.build();
	}
}