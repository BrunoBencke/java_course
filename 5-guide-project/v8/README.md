# Cerne · milestone v8 (starting point)

Java 8 · Java EE 7 (`javax.*`, `provided` scope) · JUnit 4.12 · Lombok 1.16.20 · XStream 1.4.9.
This is the system "as it is in production". Sessions 1 and 2 work here; nothing has been fixed.

## Build and test

Run from `5-guide-project/`:

```sh
export JAVA_HOME=~/.sdkman/candidates/java/8.0.432-amzn
mvn -q -f v8/pom.xml test          # silence = success
mvn -q -f v8/pom.xml package       # produces v8/target/cerne-v8.jar and v8/target/lib/ (dependencies)
```

## What this milestone demonstrates

The seven planted smells, one `// SMELL N:` comment each (`grep -rn "SMELL" v8/src` lists all seven):

| Smell | Class |
|---|---|
| 1. Cascading nulls, `return null` | `cerne.statements.StatementService` |
| 2. Giant utility class, static `SimpleDateFormat`, mixed-in business rules, `sun.misc.BASE64Encoder` | `cerne.util.CerneUtil` |
| 3. `java.util.Date`, `double amount`, `String type` | `cerne.entries.Entry` |
| 4. Quadratic nested loops and `String +=` | `cerne.entries.EntryReport` |
| 5. ~150 lines of chained `if`s | `cerne.transfers.TransferValidator` |
| 6. Inheritance as reuse | `cerne.entries.CreditEntry` |
| 7. `HttpURLConnection` with no timeout | `cerne.externallookup.CpfLookupClient` |

The three "pattern-named" classes of `cerne.util` (`EntryFactory`, `ServiceDecorator`, `StatementVisitor`), read in session 7, are also here. The guide project's `CerneUtil` is about 200 lines long; the comment at the end of the class stands for the rest.

## Commands used in the sessions

**Internal APIs (session 1).** JDK 8's `jdeps` uses `-jdkinternals` (with one hyphen). The `--jdk-internals` form only exists from JDK 9 onwards.

```sh
$JAVA_HOME/bin/jdeps -jdkinternals v8/target/cerne-v8.jar
```

Expected output: `CpfLookupClient -> sun.misc.BASE64Encoder/BASE64Decoder` and `CerneUtil -> sun.misc.BASE64Encoder`, with the suggestion `Use java.util.Base64 @since 1.8`. To analyze the dependencies too, use `v8/target/lib/`.

**Report measurement (session 2).** The `ReportBenchmark` class is not in this milestone: it depends on `LegacyEntryReport`, which the students create during the session. Both are ready in `v11`. After creating them:

```sh
mvn -q -f v8/pom.xml compile exec:java -Dexec.mainClass=cerne.measurement.ReportBenchmark
```

`Stopwatch` and `DatasetGenerator` (`members(20_000)`, `entries(members, 200_000)`, `reconciliation(50_000)`) are already in `cerne.measurement`.

**Entry processing and GC (session 6).** The arguments are `[warmups] [runs] [entries]`, defaulting to `3 5 1000000`. To reduce the load on a slow machine, pass the third argument with the same value in all four milestones.

```sh
~/.sdkman/candidates/java/8.0.432-amzn/bin/java -Xms1g -Xmx1g \
  -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:gc-v8.log \
  -cp v8/target/classes cerne.measurement.EntryProcessing 3 5
```

JFR recording `cerne-v8.jfr`. Corretto 8.0.432 already ships JFR, without `UnlockCommercialFeatures`:

```sh
~/.sdkman/candidates/java/8.0.432-amzn/bin/java -Xms1g -Xmx1g \
  -XX:StartFlightRecording=filename=cerne-v8.jfr,settings=profile \
  -cp v8/target/classes cerne.measurement.EntryProcessing 3 5
```

On macOS ARM, JDK 8 may print `Thread method sampler crashed`. The recording is written anyway, just without the method samples. Read it with JDK 21's `jfr`.

## Difference from the previous milestone

None: this is the starting point.
