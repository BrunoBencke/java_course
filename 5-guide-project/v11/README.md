# Cerne · milestone v11 (result of session 3)

Java 11 · Java EE 8 (still `javax.*`) · JUnit 4.12 · Lombok 1.18.30 · external JAXB 2.3.3 · XStream 1.4.9.
Starts from `v8` with the challenges of sessions 1 and 2 already applied and takes Cerne to JDK 11.

## Build and test

```sh
export JAVA_HOME=~/.sdkman/candidates/java/11-open
mvn -q -f v11/pom.xml test
mvn -q -f v11/pom.xml package      # v11/target/cerne-v11.jar + v11/target/lib/
$JAVA_HOME/bin/jdeps --jdk-internals v11/target/cerne-v11.jar   # no output: no internal API
```

## What this milestone demonstrates

- **Session 1 challenge:** `Entry` with `BigDecimal`, `LocalDate` and `enum EntryType`, plus `isBetween(start, end)`.
- **Session 2 and its challenge:**
  - `EntryReport` was rewritten with Streams in three steps: index with `groupingBy`, line per member with `partitioningBy` + `reducing`, final pipeline. `LegacyEntryReport` keeps the loop version, only for the measurement.
  - `StatementService` uses `Optional`: `findByCpf` returns `Optional<Member>` and "nothing" is an empty list.
  - `TransferReconciliation` uses Streams and `Optional`. The index is a `groupingBy`, with the rationale in a comment, and the 14 tests are intact.
- **Session 3:**
  - `pom.xml` with `<maven.compiler.release>11`, compiler 3.11.0 and surefire 3.2.5.
  - Lombok 1.18.30.
  - `jakarta.xml.bind-api:2.3.3` + `jaxb-runtime:2.3.3` (`runtime`), without changing imports.
  - `javaee-api` 8.0.
  - `CerneUtil` without `sun.misc` (uses `java.util.Base64`).
  - `CpfLookupClient` with `java.net.http.HttpClient`, connection timeout (2 s) and request timeout (5 s), non-200 status handled and interruption restored.
  - `var` and `List.of`/`List.copyOf` in the in-memory repositories.
- **Surefire `argLine`:** XStream 1.4.9 does deep reflection on **four** JDK packages. With only `java.util` opened, the warning changes to `java.lang.reflect.Proxy.h`. That is why the `argLine` property in `pom.xml` opens all four. To see the warning, run `mvn -q -f v11/pom.xml test -DargLine=""`.
- **JAXB reference file:** `ReconciliationFileGeneratorTest` compares the output with `src/test/resources/reconciliation/reference-file.xml`. The same file stays green in `v17` and `v21`, already with `jakarta.xml.bind`.

## Measurements

```sh
# session 2: loops × Streams (optional arguments: members entries; default 5000 50000)
mvn -q -f v11/pom.xml compile exec:java -Dexec.mainClass=cerne.measurement.ReportBenchmark

# session 6: the same processing in the four milestones (quotes because of the * in zsh)
$JAVA_HOME/bin/java -Xms1g -Xmx1g "-Xlog:gc*:file=gc-v11.log:uptime,level,tags" \
  -cp v11/target/classes cerne.measurement.EntryProcessing 3 5
```

The `ReportBenchmark` default (5,000 members and 50,000 entries) keeps the legacy version in the range of seconds. With `20000 200000`, the legacy version took about 70 s per round on this reference machine.

## Difference from v8

Smells 1, 2 (the `sun.*` part), 3, 4 and 7 were fixed, and the `// SMELL` comments are gone. Still in the code, without a comment: the `CreditEntry extends Entry` inheritance (session 4), the chained-`if` validator (session 8) and the static `SimpleDateFormat` in `CerneUtil`, which goes away in session 4. Everything that depended on `Entry` now uses `BigDecimal`/`LocalDate`: dataset generator, statements, import, API and the reconciliation file.
