package io.paasas.pipelines.server.security.module;

import java.util.List;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CiSecurityConfiguration {
	@NotEmpty
	List<CiUser> users;
}
