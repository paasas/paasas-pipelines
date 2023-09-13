package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
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
@Entity(name = "FirebaseTestReport")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseTestReportEntity {
	@EmbeddedId
	DeploymentKey key;

	@Embedded
	DeploymentInfoEntity testInfo;

	@Embedded
	GitRevisionEntity gitRevision;

	String reportUrl;
	String testBranch;
	String testCommit;
	String testCommitAuthor;
	String testPath;
	String testRepository;
	String testTag;

	public TestReport to() {
		return TestReport.builder()
				.buildUrl(testInfo.getUrl())
				.projectId(testInfo.getProjectId())
				.reportUrl(reportUrl)
				.build();
	}

	public static FirebaseTestReportEntity from(RegisterFirebaseAppTestReport request) {
		return FirebaseTestReportEntity.builder()
				.gitRevision(GitRevisionEntity.from(request.getGitRevision()))
				.key(DeploymentKey.from(request.getJobInfo()))
				.reportUrl(request.getReportUrl())
				.testBranch(request.getTestGitRevision().getBranch())
				.testCommit(request.getTestGitRevision().getCommit())
				.testCommitAuthor(request.getTestGitRevision().getCommitAuthor())
				.testInfo(DeploymentInfoEntity.from(request.getJobInfo()))
				.testPath(request.getTestGitRevision().getPath())
				.testRepository(request.getTestGitRevision().getRepository())
				.testTag(request.getTestGitRevision().getTag())
				.build();
	}
}