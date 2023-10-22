package io.github.hison0319.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class DataModelBaseDeserializer extends JsonDeserializer<DataModelBase> {

    @Override
    public DataModelBase deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        DataModelBase dataModelBase = new DataModelBase(node);
        
        return dataModelBase;
    }
}
