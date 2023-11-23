package io.github.hison.data.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DataModelSerializer extends JsonSerializer<DataModel> {

    @Override
    public void serialize(DataModel dataModelBase, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeTree(dataModelBase.getConvertJson());
    }
}
