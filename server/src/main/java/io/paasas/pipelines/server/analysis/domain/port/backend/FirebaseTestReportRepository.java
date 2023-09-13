package io.paasas.pipelines.server.analysis.domain.port.backend;

import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;

public interface FirebaseTestReportRepository {
	void registerFirebaseTestReport(RegisterFirebaseAppTestReport request);
}
