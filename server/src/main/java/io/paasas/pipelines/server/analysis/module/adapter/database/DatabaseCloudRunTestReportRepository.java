package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunTestReport;
import io.paasas.pipelines.server.analysis.domain.model.TestReport;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunTestReportRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunTestReportEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseCloudRunTestReportRepository implements CloudRunTestReportRepository {
	CloudRunTestReportJpaRepository repository;

	@Override
	public List<TestReport> findByImageAndTag(String image, String tag, Sort sort) {
		return repository.findByImageAndTag(image, tag, sort)
				.stream()
				.map(CloudRunTestReportEntity::to)
				.toList();
	}

	@Override
	@Transactional
	public void registerCloudRunTestReport(RegisterCloudRunTestReport request) {
		repository.save(CloudRunTestReportEntity.from(request));
	}

}
