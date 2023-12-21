package io.github.hison.data.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class DataModelDeserializer extends JsonDeserializer<DataModel> {

    @Override
    public DataModel deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException{
        DataModel dataModel = new DataModel();

        if (jp.isExpectedStartArrayToken()) {
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                JsonNode arrayNode = jp.readValueAsTree();
                dataModel.addRow(arrayNode);
            }
        } else if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            JsonNode objectNode = jp.readValueAsTree();
            dataModel.addRow(objectNode);
        }

        return dataModel;
    }
}