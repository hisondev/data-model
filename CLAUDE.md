# data-model — hisondev 데이터 전송/관리 라이브러리

hisondev 생태계의 핵심 Java artifact. DataWrapper/DataModel로 프런트(hisonjs)–서버 간 데이터 교환을 정형화.
hisonjv 통합 artifact에 포함되며, 단독 사용도 가능 (`io.github.hisondev:data-model`).

## 구조 (중첩 디렉토리 주의)

```
jv/data-model/          ← git 저장소 루트 (README, LICENSE)
└─ data-model/          ← 실제 Maven 프로젝트 루트 (pom.xml)
   └─ src/main/java/io/github/hison/data/
      ├─ wrapper/    DataWrapper (+Serializer/Deserializer)
      ├─ model/      DataModel 3,150줄 (+Serializer/Deserializer)
      ├─ condition/  Condition (HashMap 상속, 검색 조건)
      ├─ converter/  DataConverter 인터페이스, DataConverterDefault, DataConverterFactory, LocalDateTime(De)Serializer
      └─ exception/  DataException (RuntimeException 상속)
```

## 핵심 사실

- **Maven**: `io.github.hisondev:data-model:2.0.0` / Java 21 / jackson-databind 2.17.2 + jakarta.servlet-api 6.0.0(둘 다 provided·optional) / MIT
- **패키지는 `io.github.hison.data.*`** (groupId·README 표기와 다름 — 혼동 주의)
- 버전 정책: 1.x = Spring Boot 2.7(javax) / 2.x = Spring Boot 3+(jakarta)
- **DataWrapper**: 값은 String/DataModel/null만 허용(그 외 DataException). DataModel은 put/get 시 깊은 복사. 생성 시 `"DATAWRAPPER":"TRUE"` 검증 키 자동 삽입. addImmutableKey로 키 불변화. 체이닝 지원
- **DataModel**: LinkedHashSet 컬럼 + ArrayList<HashMap> 로우. 생성자가 ResultSet/JsonNode/엔티티/List/HttpSession/Object[]+컬럼명 지원. 검색(Condition·AND·equals)/필터(Predicate)/정렬/검증/freeze/strictColumnType/엔티티 변환(getConvertedEntities) 제공
- **값 정규화**: 기본 컨버터가 원시 래퍼(Integer 등)를 **문자열로 저장**, LocalDateTime → `yyyy-MM-dd HH:mm:ss` 문자열. 커스터마이징은 DataConverterDefault 상속 + `DataConverterFactory.setCustomConverter()`
- **메모리 가드(v2 신규)**: 기본 on, 한도 32MB, 초과 시 DataException. setMaxPayloadBytes/setMemoryGuardEnabled/setDataSizeEstimator
- **JSON 직렬화**: DataModel은 로우 객체 배열 `[{"col":"val"},...]`, DataWrapper는 단순 객체. (사이트의 columns/rows 분리 예시는 오류)

## 상세 문서

- 가이드: `../../../md/hisondev-data-model.md` (소스 검증 완료)
- 전체 메서드 표: `../../../md/hisondev-hisonjv.md`의 DataModel 섹션 (일부 누락 있음 — 가이드의 '알려진 이슈' 참조)
- 생태계 전체: `../../../md/hisondev-ecosystem.md`

## 알려진 이슈 (수정 금지 — 추후 소유자와 재정리 예정)

1. README 예제 import가 구버전 `io.github.hisondev.datamodel.*` (실제: `io.github.hison.data.*`), "JDK 8+" 표기 잔존
2. 공식 사이트 API 표 누락: DataWrapper.remove/addImmutableKey, DataModel 메모리 가드·isFreeze·insert·strictColumnType 계열. `setFreezeDataModel`(표) ≠ `setFreeze`(실제)
3. 사이트 getting-started의 직렬화 예시(`{"columns":[...],"rows":[[...]]}`)는 실제 형식과 다름
4. DataException javadoc의 "from Spring framework" 서술 오류

## 작업 규칙

- 이 저장소의 소스/README 수정은 사용자의 명시적 지시가 있을 때만 진행 (프로젝트 루트 CLAUDE.md 규칙 준수)
