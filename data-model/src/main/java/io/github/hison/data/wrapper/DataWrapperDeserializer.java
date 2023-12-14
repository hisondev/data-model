package io.github.hison.data.wrapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import io.github.hison.data.model.DataModel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class DataWrapperDeserializer extends JsonDeserializer<DataWrapper> {

    @Override
    public DataWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        DataWrapper dataWrapper = new DataWrapper();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();

            if (valueNode.isObject() || valueNode.isArray()) {
                DataModel dataModel = new DataModel(valueNode);
                dataWrapper.putDataModel(key, dataModel);
            } else if (valueNode.isNull() || valueNode instanceof NullNode) {
                dataWrapper.put(key, null);
            } else {
                dataWrapper.putString(key, valueNode.asText());
            }
        }

        return dataWrapper;
    }
}