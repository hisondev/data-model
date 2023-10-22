package io.github.hison0319.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DataModelBaseSerializer extends JsonSerializer<DataModelBase> {

    @Override
    public void serialize(DataModelBase dataModelBase, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeTree(dataModelBase.getConvertJson());
    }
}
