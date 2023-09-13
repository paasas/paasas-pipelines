package io.paasas.pipelines.server.analysis.module.adapter.database;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppTestReport;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseTestReportRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.FirebaseTestReportEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseFirebaseTestReportRepository implements FirebaseTestReportRepository {
	FirebaseTestReportJpaRepository repository;

	@Override
	@Transactional
	public void registerFirebaseTestReport(RegisterFirebaseAppTestReport request) {
		repository.save(FirebaseTestReportEntity.from(request));
	}
}
