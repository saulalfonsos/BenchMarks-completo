package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.utils.patch.JsonPatch;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

/**
 * Aplica unha lista de operacións JSON-Patch (RFC 6902) sobre a representación dun recurso
 * e valida o resultado coas mesmas restricións que se usan nas peticións POST.
 */
@Component
public class Patcher {
    private final JsonMapper mapper;
    private final Validator validator;

    public Patcher(JsonMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public <T> T apply(T original, List<JsonPatchOperation> operations, Class<T> type) {
        JsonNode patched = JsonPatch.apply(operations, mapper.convertValue(original, JsonNode.class));
        T result = mapper.convertValue(patched, type);

        Set<ConstraintViolation<T>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return result;
    }
}
