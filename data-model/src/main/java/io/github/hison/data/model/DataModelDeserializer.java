package io.github.hison.data.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * @author Hani son
 * @version 1.0.4
 */
public class DataModelDeserializer extends JsonDeserializer<DataModel> {

    @Override
    public DataModel deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException{
        DataModel dataModel = new DataModel();

        if (jp.isExpectedStartArrayToken()) {
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                JsonNode arrayNode = jp.readValueAsTree();
                dataModel.addRows(arrayNode);
            }
        } else if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            JsonNode objectNode = jp.readValueAsTree();
            dataModel.addRows(objectNode);
        }

        return dataModel;
    }
}