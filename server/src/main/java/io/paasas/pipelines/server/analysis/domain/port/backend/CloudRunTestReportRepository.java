package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import org.springframework.data.domain.Sort;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;

public interface CloudRunTestReportRepository {
	List<TestReport> findByImageAndTag(String image, String tag, Sort sort);

	void registerCloudRunTestReport(RegisterCloudRunTestReport request);
}
