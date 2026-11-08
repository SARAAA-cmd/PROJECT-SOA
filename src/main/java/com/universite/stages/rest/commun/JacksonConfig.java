package com.universite.stages.rest.commun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universite.stages.common.Json;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/** Fournit l'ObjectMapper partagé à Jersey. */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return Json.MAPPER;
    }
}
