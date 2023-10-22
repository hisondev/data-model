package io.github.hison0319.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class DataWrapperSerializer extends JsonSerializer<DataWrapper> {

    @Override
    public void serialize(DataWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject(); // JSON의 시작 { 를 작성

        // String 형태의 데이터를 JSON에 쓰기
        for (Map.Entry<String, String> entry : value.getStrings().entrySet()) {
            if (entry.getValue() != null) {
                gen.writeStringField(entry.getKey(), entry.getValue());
            } else {
                gen.writeNullField(entry.getKey());
            }
        }

        // DataModel 형태의 데이터를 JSON에 쓰기
        for (Map.Entry<String, DataModelBase> entry : value.getDataModels().entrySet()) {
            gen.writeFieldName(entry.getKey());

            // DataModel의 getConvertJson 메서드를 사용하여 직렬화합니다.
            gen.writeTree(entry.getValue().getConvertJson());
        }

        gen.writeEndObject(); // JSON의 종료 } 를 작성
    }
}
