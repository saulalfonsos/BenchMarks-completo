package gal.usc.etse.es.utils.patch;

import gal.usc.etse.es.utils.patch.exceptions.*;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public class JsonPatch {
    public static JsonNode apply(List<JsonPatchOperation> patchOperations, JsonNode rootDocument) throws JsonPatchFailedException {
        for (JsonPatchOperation instruction : patchOperations) {
            if (instruction.operation() == null || instruction.path() == null || instruction.path().matches()) {
                throw new JsonPatchFailedException("Each operation needs a valid 'op' and a non-empty 'path'");
            }
            switch (instruction.operation()) {
                case REMOVE, REPLACE -> requireExisting(instruction.path(), rootDocument);
                case COPY, MOVE -> {
                    if (instruction.from() == null) throw new JsonPatchFailedException("'" + instruction.operation() + "' needs 'from'");
                    requireExisting(instruction.from(), rootDocument);
                }
                case ADD -> requireExisting(instruction.path().head(), rootDocument);
            }
            rootDocument = switch (instruction.operation()) {
                case ADD -> add(instruction.path(), instruction.value(), rootDocument);
                case REMOVE -> remove(instruction.path(), rootDocument);
                case REPLACE -> replace(instruction.path(), instruction.value(), rootDocument);
                case COPY -> copy(instruction.path(), instruction.from(), rootDocument);
                case MOVE -> move(instruction.path(), instruction.from(), rootDocument);
            };
        }
        return rootDocument;
    }

    /** RFC 6902: as operacións que len ou substitúen un valor fallan se a ruta non existe. */
    private static void requireExisting(JsonPointer path, JsonNode document) {
        if (path == null || document.at(path).isMissingNode()) {
            throw new JsonPatchFailedException("Path '" + path + "' does not exist in the resource");
        }
    }

    public static JsonNode add(JsonPointer path, JsonNode value, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(value, path, document);
    }

    public static JsonNode copy(JsonPointer path, JsonPointer from, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(JsonPatchUtil.at(document, from), path, document);
    }

    public static JsonNode move(JsonPointer path, JsonPointer from, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.remove(from, JsonPatchUtil.put(JsonPatchUtil.at(document, from), path, document));
    }

    public static JsonNode remove(JsonPointer path, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.remove(path, document);
    }

    public static JsonNode replace(JsonPointer path, JsonNode value, JsonNode document) throws JsonPatchFailedException {
        return JsonPatchUtil.put(value, path, JsonPatchUtil.remove(path, document));
    }
}