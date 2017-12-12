package org.cbioportal.session_service.mixin;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SessionMixin {

    @JsonIgnore
    private String checksum;
    
}
