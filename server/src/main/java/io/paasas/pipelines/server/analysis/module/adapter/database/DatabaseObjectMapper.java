package io.paasas.pipelines.server.analysis.module.adapter.database;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class DatabaseObjectMapper {
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
