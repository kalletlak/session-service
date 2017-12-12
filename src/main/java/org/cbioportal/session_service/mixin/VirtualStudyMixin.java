package org.cbioportal.session_service.mixin;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class VirtualStudyMixin {

    @JsonIgnore
	private Long created;

    @JsonIgnore
	private Set<String> users;
}
