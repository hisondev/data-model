package io.github.hison.data.wrapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.github.hison.data.model.DataModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Hani son
 * @version 1.0.4
 */
public class DataWrapperSerializer extends JsonSerializer<DataWrapper> {

    @Override
    public void serialize(DataWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        
        HashMap<String, Object> data = value.getDatas();
        DataModel dataModel = new DataModel();

        Set<String> keys = data.keySet();
        for (String key : keys) {
            if (data.get(key) == null) {
                gen.writeNullField(key);
            } else if (data.get(key) instanceof String) {
                gen.writeStringField(key, (String)data.get(key));
            } else if (data.get(key) instanceof DataModel) {
                gen.writeFieldName(key);
                dataModel = (DataModel)data.get(key);
                dataModel.serialize(dataModel, gen, serializers);
            }
        }

        gen.writeEndObject();
    }
}