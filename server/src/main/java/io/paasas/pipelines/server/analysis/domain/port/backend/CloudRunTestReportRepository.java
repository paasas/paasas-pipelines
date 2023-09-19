package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;

public interface CloudRunTestReportRepository {
	List<TestReport> findByImageAndTag(String image, String tag);

	void registerCloudRunTestReport(RegisterCloudRunTestReport request);
}
