package io.paasas.pipelines.server.analysis.domain.port.api;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;

public interface TestReportDomain {
	void registerCloudRunTestReport(RegisterCloudRunTestReport request);

	void registerFirebaseTestReport(RegisterFirebaseAppTestReport request);
}
