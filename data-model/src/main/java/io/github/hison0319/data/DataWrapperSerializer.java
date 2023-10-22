package io.github.hison0319.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class DataWrapperSerializer extends JsonSerializer<DataWrapper> {

    @Override
    public void serialize(DataWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        for (Map.Entry<String, String> entry : value.getStrings().entrySet()) {
            if (entry.getValue() != null) {
                gen.writeStringField(entry.getKey(), entry.getValue());
            } else {
                gen.writeNullField(entry.getKey());
            }
        }

        for (Map.Entry<String, DataModelBase> entry : value.getDataModels().entrySet()) {
            gen.writeFieldName(entry.getKey());
            gen.writeTree(entry.getValue().getConvertJson());
        }

        gen.writeEndObject();
    }
}
