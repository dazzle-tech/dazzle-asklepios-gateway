package com.dazzle.asklepios.web.filter.requesttrace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Masks sensitive fields before the payload is written to the request trace log.
 */
@Component
public class RequestTracePayloadSanitizer {

	private static final String MASK = "***";
	private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "token", "authorization", "accesstoken", "refreshtoken");

	public JsonNode sanitize(JsonNode payload) {
		if (payload == null || payload.isNull() || payload.isMissingNode()) {
			return NullNode.instance;
		}

		if (payload.isObject()) {
			ObjectNode sanitized = payload.deepCopy();
			Iterator<Map.Entry<String, JsonNode>> fields = sanitized.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				if (isSensitiveField(field.getKey())) {
					sanitized.set(field.getKey(), TextNode.valueOf(MASK));
				} else {
					sanitized.set(field.getKey(), sanitize(field.getValue()));
				}
			}
			return sanitized;
		}

		if (payload.isArray()) {
			ArrayNode sanitized = payload.deepCopy();
			for (int i = 0; i < sanitized.size(); i++) {
				sanitized.set(i, sanitize(sanitized.get(i)));
			}
			return sanitized;
		}

		return payload;
	}

	private boolean isSensitiveField(String fieldName) {
		return fieldName != null && SENSITIVE_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT));
	}
}

