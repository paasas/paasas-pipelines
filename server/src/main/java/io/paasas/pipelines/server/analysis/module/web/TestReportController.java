package io.paasas.pipelines.server.analysis.module.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
import io.paasas.pipelines.server.analysis.domain.port.api.TestReportDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/ci/test-report")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestReportController {
	TestReportDomain testReportDomain;

	@PostMapping("/cloud-run")
	public void registerCloudRunTestReport(@RequestBody RegisterCloudRunTestReport request) {
		testReportDomain.registerCloudRunTestReport(request);
	}

	@PostMapping("/firebase")
	public void registerFirebaseTestReport(@RequestBody RegisterFirebaseAppTestReport request) {
		testReportDomain.registerFirebaseTestReport(request);
	}
}
