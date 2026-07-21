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

- **Maven**: `io.github.hisondev:data-model:2.0.2`(2026-07-21 setColumnSameValue 스펙 수정, 배포 대기 — 2.0.1은 배포 완료) / Java 21 / jackson-databind 2.17.2 + jakarta.servlet-api 6.0.0(둘 다 provided·optional) / MIT
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

## 보완 이력 (v2.0.2 — 2026-07-21 완료, 배포 대기)

**`setColumnSameValue` 스펙 정합 수정** (nonoshow 역검증에서 발견 — 상세 = `../../../md/hisondev-data-model.md` 9-1절):
- 🔴 컬럼 부재 시 **조용한 no-op → 자동 컬럼 생성**으로 변경 (hisonjs는 원래 자동 생성이 문서화된 스펙 — JS↔Java 불일치 버그였음. nonoshow ApiHandler actorId 주입 전면 무동작의 원인)
- 컬럼 신설 = 구조 변경이므로 `setFreeze` 체크 추가 (값 변경은 기존대로 `setFreezeValues`)
- 값을 컨버터(`getConvertValueToDataModelRowValue`) 경유로 통일 (기존 raw 삽입 — setValue/addRow와 불일치였음)
- pom 2.0.1→2.0.2, README Changelog, Javadoc 갱신. 커밋 문구(한 줄): `v2.0.2: Auto-add missing column in setColumnSameValue (align with hisonjs spec) + route value through converter`

## 보완 이력 (v2.0.1 — 2026-07-06 완료, 배포 완료)

코드 버그·캡슐화·문서 보완. 상세 = `../../../md/hisondev-data-model.md` 9절 / README Changelog. 스모크 테스트 9종 통과.
- 코드: **freeze 정상화**(filterAndModify 버그 + 구조/값 체크 일관화, 값동결 시 구조변경 허용) / getRows 깊은복사 / DataWrapper.putDataModel clone / equals·hashCode 추가 / 값정규화 BigDecimal·BigInteger·LocalDate·LocalTime / List<T> 조건·containsKey 정리
- 문서: getString·getDataModel javadoc(null 반환), DataException javadoc, README import(`io.github.hison.data.*`)·버전·JDK·Changelog
- ⚠️ 하위호환 유지(버그픽스·additive). getRows/putDataModel은 "반환/저장 시 복사"라 안전성 강화 방향
- ⚠️ **미수정(별도 결정)**: 메모리 가드 O(n²) — estimator가 매번 전체 직렬화, addRows 대량 시 병목. 안전성은 정상. 근본 개선은 회귀 리스크로 논의 대기
- 남은 문서 이슈(사이트 API 표·직렬화 예시·숫자문자열화 명시) → **github.io 단계**

## 작업 규칙

- 이 저장소의 소스/README 수정은 사용자의 명시적 지시가 있을 때만 진행 (프로젝트 루트 CLAUDE.md 규칙 준수)
