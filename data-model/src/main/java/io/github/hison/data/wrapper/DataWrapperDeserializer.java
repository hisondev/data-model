package io.github.hison.data.wrapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import io.github.hison.data.model.DataModel;

import java.io.IOException;

public class DataWrapperDeserializer extends JsonDeserializer<DataWrapper> {

    @Override
    public DataWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException{
        DataWrapper dataWrapper = new DataWrapper();

        if (jp.isExpectedStartObjectToken()) {
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String key = jp.getCurrentName();
                jp.nextToken();
                JsonToken currentToken = jp.getCurrentToken();
                if (currentToken == JsonToken.START_OBJECT || currentToken == JsonToken.START_ARRAY) {
                    JsonNode valueNode = jp.readValueAsTree();
                    DataModel dataModel = new DataModel(valueNode);
                    dataWrapper.putDataModel(key, dataModel);
                } else if (currentToken == JsonToken.VALUE_NULL) {
                    dataWrapper.put(key, null);
                } else {
                    String value = jp.getText();
                    dataWrapper.putString(key, value);
                }
            }
        }

        return dataWrapper;
    }
}