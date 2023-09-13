package io.paasas.pipelines.server.analysis.domain;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
import io.paasas.pipelines.server.analysis.domain.port.api.TestReportDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunTestReportRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseTestReportRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultTestReportDomain implements TestReportDomain {
	CloudRunTestReportRepository cloudRunTestReportRepository;
	FirebaseTestReportRepository firebaseTestReportRepository;

	@Override
	public void registerCloudRunTestReport(RegisterCloudRunTestReport request) {
		log.info("Registering {}", request);

		cloudRunTestReportRepository.registerCloudRunTestReport(request);
	}

	@Override
	public void registerFirebaseTestReport(RegisterFirebaseAppTestReport request) {
		log.info("Registering {}", request);

		firebaseTestReportRepository.registerFirebaseTestReport(request);
	}
}
