package io.paasas.pipelines.server.github.domain.model.pull;

import lombok.Value;

@Value
public class Comment {
	String path;
	Integer position;
	String body;
	Integer line;
	String side;
	Integer startLine;
	Integer startSide;
}
