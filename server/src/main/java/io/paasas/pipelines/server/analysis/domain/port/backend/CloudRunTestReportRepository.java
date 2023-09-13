package io.paasas.pipelines.server.analysis.domain.port.backend;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;

public interface CloudRunTestReportRepository {
	void registerCloudRunTestReport(RegisterCloudRunTestReport request);
}
