# Migrating 8 → 11: the build broke in twenty places

On Friday someone switched the continuous integration server's JDK to 11 "just to see what happens". The result was a compilation report with twenty errors in Cerne: `package javax.xml.bind does not exist` in the statement exporter, `cannot find symbol: class BASE64Encoder` in the reconciliation file signer, an illegal reflective access warning from Lombok at the top of the log followed by a string of `cannot find symbol` errors for getters nobody deleted and, when someone forced the build through, the application server's JVM refused to start because of a garbage collector flag that no longer exists. None of this is a Java 11 bug. It is the bill for three versions (9, 10 and 11) arriving all at once, and there is a right order in which to pay it.

This session delivers two things: a **migration roadmap** that works for any version jump (and will be reused for the 11 → 17 and 17 → 21 jumps) and the map of what actually changed between 8 and 11, separating language gains (`var`, immutable collections, HTTP Client, new `String` methods) from structural gotchas (module system, APIs removed from the JDK, JVM flags, old libraries).

## Why 11, and not 9 or 10

Java 9 (2017) and 10 (2018) were short-cycle releases, without extended support. 11 (September 2018) is the first LTS after 8, and therefore the first sensible destination for a migration: the OpenJDK distributions (Temurin, Corretto, Zulu, Microsoft Build) keep shipping security updates for 11 for years, and application servers certified versions for it. Migrating to 9 or 10 would mean paying the cost and stopping on an unsupported step.

The catch is that, by jumping from 8 to 11, you inherit everything 9 and 10 introduced. The table below is this session's map.

| Version | What came in | What went out or changed |
|---|---|---|
| 9 (2017) | Module system (JPMS, JEP 261); `List.of`, `Set.of`, `Map.of`; `Optional.ifPresentOrElse`, `Stream.takeWhile`; `jdeps` and `jlink`; unified logging (`-Xlog`) | G1 becomes the default GC (it was Parallel on 8); internal `sun.*` APIs encapsulated; `-Xbootclasspath/p` and old GC logging flags removed |
| 10 (2018) | `var` for local variables (JEP 286); `List.copyOf`, `Collectors.toUnmodifiableList` | `-XX:+UseParNewGC` removed; `-d64` removed |
| 11 (2018) | Standard HTTP Client (JEP 321); `String.isBlank`, `strip`, `lines`, `repeat`; `Files.readString`; `Predicate.not`; `Optional.isEmpty` | JAXB, JAX-WS, CORBA, JTA and Common Annotations removed from the JDK (JEP 320); JavaFX leaves Oracle's JDK; `Thread.destroy` and `Thread.stop(Throwable)` removed |

> 💭 **Stop and think:** in the system you maintain, how many of these rows can you map off the top of your head to a concrete file? If the answer is "none", the inventory from the Session 1 challenge (dependencies, internal APIs and legacy dates) is the first artifact to open today. If the answer is "several", you already know where the build is going to break.

## The step-by-step migration roadmap

The temptation is to open the `pom.xml`, change `1.8` to `11` and go fixing error after error. It works, but it mixes four kinds of problem (code that does not compile, tests that break, a server that does not start, behavior that changed) and you lose track of which change caused what. The roadmap below separates those kinds and defines an exit criterion for each stage. Oracle's Migration Guide recommends the same order, with a step zero that many people skip.

| Stage | What to do | Exit criterion |
|---|---|---|
| 0. Inventory | Run `jdeps -jdkinternals` on the current artifact (compiled with 8); list dependencies and their versions; read the libraries' release notes | List of internal APIs used and of libraries without support for 11 |
| 1. Compile | Upgrade `maven-compiler-plugin` and switch to `<release>11</release>`; upgrade the libraries on the list; restore the modules removed from the JDK; fix compilation errors | `mvn compile` without errors; no `sun.*` in the code |
| 2. Test | Upgrade `surefire`; run the whole suite; fix illegal reflective access warnings that turn into failures | `mvn verify` green, with no `WARNING: An illegal reflective access` you cannot explain |
| 3. Run | Start the application server on a version certified for JDK 11; review the JVM flags; execute the critical flow (transfer, statement, reconciliation) in the staging environment | Application deployed and critical flow executed with no errors in the logs |
| 4. Measure | Repeat the same load used on the `v8` milestone (same volume of entries, same machine, warmed-up JVM); compare time, heap and GC pauses | `v8` × `v11` comparison table; `v11` milestone in the repository |

Stage 4 is the one that gives the business area the argument for the next migrations, and it is only honest if stage 0 was done: without an inventory you don't know whether the measured gain came from the new GC or from a library you swapped along the way. Session 6 goes deeper into measurement; here it is enough to record the numbers.

### Step zero in practice: `jdeps`

`jdeps` has existed since Java 8, and its `-jdkinternals` option is worth its weight in gold during a migration. On JDK 8 it only exists with that spelling; from 9 on `jdeps` also accepts `--jdk-internals`, so `-jdkinternals` works on both sides of the jump. Point it at the Cerne JAR compiled with 8, with the dependencies on the class path.

```sh
jdeps -jdkinternals --class-path 'target/lib/*' target/cerne-v8.jar
```

The output lists every class in the system that touches an internal API and, at the bottom, the suggested replacement.

```
cerne.jar -> JDK removed internal API
cerne.jar -> jdk.unsupported
   cerne.reconciliation.FileSigner         -> sun.misc.BASE64Encoder   JDK internal API (JDK removed internal API)
   cerne.statements.PdfStatementGenerator  -> sun.misc.Unsafe          JDK internal API (jdk.unsupported)

Warning: JDK internal APIs are unsupported and private to JDK implementation that are
subject to be removed or changed incompatibly and could break your application.

JDK Internal API                         Suggested Replacement
----------------                         ---------------------
sun.misc.BASE64Encoder                   Use java.util.Base64 @since 1.8
```

Two lines of output, two different fates: `BASE64Encoder` was **removed** (the code does not compile on 11), while `sun.misc.Unsafe` lives in the `jdk.unsupported` module and remains accessible (the code compiles, but the PDF library that uses it is a candidate for an upgrade). `jdeps` makes that distinction for you.

### The minimal Maven configuration

```xml
<properties>
    <maven.compiler.release>11</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

Use `release`, not the `source`/`target` pair. With `release` the compiler validates the code against the exact public API of Java 11; with `source`/`target` it compiles against the classes of whatever JDK is installed, and a method that only exists in 17 would slip through until the staging server, which runs 11, throws `NoSuchMethodError`.

> 🎬 **Picture this:** the person who made the first migration attempt fixed the twenty errors on Monday, but on Wednesday the main branch build broke again with three others: a colleague merged a new feature, still written in Java 8, that used `sun.misc.BASE64Encoder` because it was copied from a neighboring class. An automated inventory stage (`jdeps` running in the pipeline, failing the build if it finds `sun.*`) would have blocked the merge. Migration is not an event; it is a quality rule that stays.

## What the language gained: `var`

Local variable type inference (JEP 286, Java 10) is the most visible change and the most misused one. The rule is simple: `var` only applies to **local variables with an initializer**, and the type is fixed by the compiler at the point of declaration. There is no dynamic typing; `var` is syntactic sugar over a static type.

```java
// Java 8
Map<String, List<Entry>> entriesByAccount = repository.findByPeriod(start, end);
for (Map.Entry<String, List<Entry>> mapEntry : entriesByAccount.entrySet()) {
    BigDecimal total = sum(mapEntry.getValue());
    ...
}

// Java 11
var entriesByAccount = repository.findByPeriod(start, end);
for (var mapEntry : entriesByAccount.entrySet()) {
    var total = sum(mapEntry.getValue());
    ...
}
```

The second version eliminates the repetition of the generic type, which was pure noise. But note that the type of `total` became invisible: the reader needs to know (or trust) that `sum` returns a `BigDecimal`. That is the cost, and the table below summarizes where it pays off.

| Situation | `var`? | Reason |
|---|---|---|
| Long generic type already visible on the right-hand side (`new HashMap<String, List<Entry>>()`) | Yes | Eliminates repetition without losing information |
| Return of a method whose name makes the type obvious (`var member = repository.findByCpf(cpf)`) | Yes | The variable name carries the type |
| Return of a generic or utility method (`var result = process(x)`) | Avoid | The reader doesn't know what `result` is without opening `process` |
| Numeric literals (`var fee = 2.50`) | Avoid | Infers `double`; in Cerne, money is `BigDecimal` |
| Variable that needs the interface type (`List<Entry> list = new ArrayList<>()`) | No | `var` would infer `ArrayList`, coupling the code to the implementation |
| Field, parameter, return type, `var x = null`, lambda without a target (`var f = x -> x`) | Does not compile | Outside the scope of JEP 286 |

Since Java 11 (JEP 323), `var` is also accepted as the type of a lambda parameter, which serves only to allow annotations on the parameter: `(@NonNull var amount) -> ...`. Rarely used.

> ✍️ **Try it now:** decide, for each line below, whether `var` makes it better, worse or does not compile.
>
> ```java
> var cpf = "12345678901";
> var balance = account.getBalance();
> var fees = new ArrayList<Fee>();
> var limit = null;
> var statement = generate(account, month);
> ```
>
> <details><summary>Show answer</summary>
>
> `cpf`: a small improvement, but harmless (a `String` literal). `balance`: acceptable, the name `getBalance` says what it is, but check that it really is a `BigDecimal`. `fees`: careful, the inferred type is `ArrayList<Fee>`, not `List<Fee>`; if the variable is passed along or reassigned with another implementation, declare the interface. `limit`: does not compile (`var` requires a non-null initializer). `statement`: worse, `generate` is too generic for the reader to know whether it returns a `Statement`, a `String` or a `byte[]`.
>
> </details>

## Immutable collections: `List.of`, `Set.of`, `Map.of`

In Java 8, building a fixed list required `Arrays.asList` (which allows `set` but not `add`) or `Collections.unmodifiableList(new ArrayList<>(...))` (two lines, and the original list stays mutable underneath). Java 9 brought static factories that return **truly immutable** collections, compact in memory and that reject `null`.

```java
// Java 8
private static final List<String> EXEMPT_TYPES =
        Collections.unmodifiableList(Arrays.asList("PIX", "TED_SAME_HOLDER"));

private static final Map<String, BigDecimal> FEE_BY_TYPE;
static {
    Map<String, BigDecimal> m = new HashMap<>();
    m.put("TED", new BigDecimal("8.50"));
    m.put("DOC", new BigDecimal("6.00"));
    m.put("BETWEEN_CREDIT_UNIONS", new BigDecimal("2.50"));
    FEE_BY_TYPE = Collections.unmodifiableMap(m);
}

// Java 11
private static final List<String> EXEMPT_TYPES = List.of("PIX", "TED_SAME_HOLDER");

private static final Map<String, BigDecimal> FEE_BY_TYPE = Map.of(
        "TED", new BigDecimal("8.50"),
        "DOC", new BigDecimal("6.00"),
        "BETWEEN_CREDIT_UNIONS", new BigDecimal("2.50"));
```

`Map.of` accepts up to ten pairs; beyond that use `Map.ofEntries(Map.entry(k, v), ...)`. Java 10 added `List.copyOf`, `Set.copyOf` and `Map.copyOf`, which make an immutable copy of an existing collection (and return the instance itself if it is already immutable of that type), ideal for defensive copies in constructors.

| Factory | Allows `add`/`remove`? | Allows `set`? | Accepts `null`? | Iteration order |
|---|---|---|---|---|
| `new ArrayList<>(...)` | Yes | Yes | Yes | Insertion |
| `Arrays.asList(...)` | No (`UnsupportedOperationException`) | Yes (writes to the backing array) | Yes | Insertion |
| `Collections.unmodifiableList(l)` | No | No | Yes | That of the original list, which stays mutable |
| `List.of(...)` / `List.copyOf(c)` | No | No | No (`NullPointerException`) | Insertion |
| `Set.of(...)` / `Map.of(...)` | No | No | No | **Undefined and varies between runs** |

> ⚠️ **Migration gotcha:** the immutability gotcha has two symptoms. The first is an `UnsupportedOperationException` in a place that never failed: someone replaced `Arrays.asList(...)` with `List.of(...)` in a method that returned the list to a caller that did `list.add(...)` three classes later. The fix is not to go back to `Arrays.asList`; it is to decide who owns the list and return `new ArrayList<>(...)` if the caller really needs to mutate it. The second symptom is subtler: a test that depended on the order of a `HashSet` (which was stable in practice) starts failing randomly after becoming `Set.of`, because the iteration order of `Set.of` and `Map.of` is deliberately shuffled on each JVM run. A test that depends on the order of a `Set` was already wrong; Java 9 just made the error visible.

## New `String` methods and other conveniences

None of these methods changes the architecture of anything, but together they eliminate dozens of homemade `StringUtils` helpers and libraries imported just for one function.

| Method | Version | Replaces, in Cerne |
|---|---|---|
| `String.isBlank()` | 11 | `s == null \|\| s.trim().isEmpty()` (the `null` is still on you) |
| `String.strip()`, `stripLeading()`, `stripTrailing()` | 11 | `trim()`, which only removes characters up to ` `; `strip` uses the Unicode definition of whitespace and removes the non-breaking space that comes from spreadsheets |
| `String.lines()` | 11 | `split("\r?\n")` when reading the reconciliation file; returns a `Stream<String>` |
| `String.repeat(n)` | 11 | The loop that built the separator line of the plain-text statement |
| `Files.readString(path)`, `Files.writeString(path, s)` | 11 | `new String(Files.readAllBytes(path), UTF_8)` |
| `Path.of(...)` | 11 | `Paths.get(...)` (equivalent; the new name follows the factory convention) |
| `Predicate.not(...)` | 11 | `s -> !s.isBlank()` becomes `Predicate.not(String::isBlank)` |
| `Optional.isEmpty()`, `Optional.or(...)`, `Optional.ifPresentOrElse(...)` | 11 / 9 / 9 | `!opt.isPresent()` and the `if`s around `Optional` |
| `Stream.takeWhile`, `dropWhile`, `ofNullable` | 9 | Loops with `break` over sorted lists |
| `Collectors.toUnmodifiableList()` | 10 | `Collectors.collectingAndThen(toList(), Collections::unmodifiableList)` |
| `InputStream.transferTo(out)` | 9 | The byte-copying loop with a buffer |

A condensed example: the reader of the reconciliation file that the other credit union sends via SFTP, with a header, entry lines and a trailer with totals.

```java
// Java 8
List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
List<ReconciliationLine> records = new ArrayList<>();
for (String line : lines) {
    if (line.trim().isEmpty() || line.startsWith("#")) {
        continue;
    }
    if (line.startsWith("TOTAL")) {
        break;
    }
    records.add(ReconciliationLine.parse(line.trim()));
}

// Java 11
var records = Files.readString(file).lines()
        .map(String::strip)
        .filter(Predicate.not(String::isBlank))
        .filter(Predicate.not(line -> line.startsWith("#")))
        .takeWhile(Predicate.not(line -> line.startsWith("TOTAL")))
        .map(ReconciliationLine::parse)
        .collect(Collectors.toUnmodifiableList());
```

`takeWhile` plays the role of `break`: it stops at the trailer without processing the rest. Notice that the pipeline returns an immutable list; if the caller needs to add records, that decision becomes explicit again.

## HTTP Client instead of `HttpURLConnection`

Cerne looks up the registration status of a CPF in an external service before opening an account or releasing a transfer above the limit. In Java 8 the class uses `HttpURLConnection`, an API from 1997: no HTTP/2, no asynchronous calls, no per-request `timeout`, and the ritual of reading the body through a `BufferedReader`.

```java
// Java 8
package cerne.members;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CpfLookupClient {

    private static final String BASE_URL = "https://cpf-lookup.example.coop/v1/cpf/";

    public RegistrationStatus lookUp(String cpf) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + cpf).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(5000);
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new IOException("CPF lookup failed with status " + status);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            return RegistrationStatus.fromJson(body.toString());
        } finally {
            connection.disconnect();
        }
    }
}
```

The Java 11 HTTP Client (JEP 321, package `java.net.http`) separates three objects: the **client** (connection configuration, reusable and thread-safe), the **request** (immutable, built with a builder) and the **response** (typed by the chosen `BodyHandler`).

```java
// Java 11
package cerne.members;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class CpfLookupClient {

    private static final URI BASE_URL = URI.create("https://cpf-lookup.example.coop/v1/cpf/");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_2)
            .build();

    public RegistrationStatus lookUp(String cpf) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(BASE_URL.resolve(cpf))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("CPF lookup failed with status " + response.statusCode());
        }
        return RegistrationStatus.fromJson(response.body());
    }
}
```

Three details that matter in the migration:

- **One client per application.** `HttpClient` keeps a connection pool; creating one per call throws the pool away. In Cerne, the class is an `@ApplicationScoped` bean, and the client is a field.
- **`InterruptedException` enters the signature.** `send` is blocking and interruptible. Either propagate it or handle it; don't swallow it.
- **Asynchronous for free.** `client.sendAsync(request, BodyHandlers.ofString()).thenApply(HttpResponse::body)` returns a `CompletableFuture<String>`, useful when the CPF lookup and the limit lookup can run in parallel. No thread pool of your own needed.

> 💭 **Stop and think:** how many different HTTP clients does your system have today? `HttpURLConnection` in old code, Apache HttpClient in one integration, the JAX-RS client in another, maybe OkHttp brought in by some library. `java.net.http` does not force you to unify anything, but it is the first time the JDK offers a client good enough to be the default for new integrations without an external dependency.

## JPMS explained through its impact

The module system (JEP 261, Java 9) is the topic that scares people the most and demands the least from those migrating. The practical question is just one: **do I need to write a `module-info.java` to run Cerne on 11?** No. All code loaded from the class path goes into the **unnamed module**, which reads every JDK module and has access to every exported package. A Java EE 7 system deployed on an application server keeps working without a single line of module code.

What changes is what the unnamed module **no longer** sees: the JDK's internal packages (`sun.*`, `com.sun.*`, `jdk.internal.*`) are no longer exported. That has two effects, of different severity.

| Kind of access | Example | On Java 8 | On Java 11 |
|---|---|---|---|
| Compiling against an internal API | `import sun.misc.BASE64Encoder` | Compiles with a warning | Compilation error (class removed or package not exported) |
| Reflection on a public API | `Member.class.getDeclaredFields()` | Works | Works the same |
| Deep reflection on a JDK class | `setAccessible(true)` on a private field of `java.lang.String` | Works | Works, with `WARNING: An illegal reflective access operation has occurred` on the first occurrence; **from 16 on it fails** (Session 4) |

This is the warning, and you will see it in the server log right after the migration:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.thoughtworks.xstream.core.util.Fields (file:/opt/server/lib/xstream-1.4.9.jar) to field java.util.TreeMap.comparator
WARNING: Please consider reporting this to the maintainers of com.thoughtworks.xstream.core.util.Fields
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

The warning tells you who (the library), what (the field) and where (the JAR). The right answer on 11 is to upgrade the library. The emergency answer, for what has no new version, is to open the package explicitly to the unnamed module with a JVM flag:

```sh
--add-opens java.base/java.util=ALL-UNNAMED
```

`--add-opens` allows deep reflection (`setAccessible`) on the package; `--add-exports` only allows compile-time access/direct use of types. When in doubt, `--add-opens` covers both. The flag goes in the application server's JVM options file, in surefire's `argLine` for the tests and in `JAVA_TOOL_OPTIONS` for processes you don't control. Write down every one you add: on 17 they stop being optional, and the list you build today is the inventory for the next session.

> 🎬 **Picture this:** Cerne's audit service serializes every transfer to XML with XStream 1.4.9 (from 2016). On 11 everything works, but the staging log gained 400 lines of `WARNING` per hour and the operations team opened a ticket thinking it was an error. Switching to XStream 1.4.20 eliminates the warnings without changing a line of code. It is the kind of fix that looks cosmetic until the day the server moves to 17 and the "warning" turns into an `InaccessibleObjectException` at three in the morning.

## Modules removed from the JDK (JEP 320)

Here is the origin of a good share of the twenty errors. Java 9 marked as deprecated, and 11 **removed**, the modules that brought Java EE APIs inside the JDK. The rationale: these APIs evolve in Java EE (now Jakarta EE), not in the JDK; whoever needs them declares the dependency. On a full Java EE application server, JAXB, JAX-WS, JTA and the common annotations are still **provided by the server**; the pain shows up in unit tests, batch processes and anything that runs outside the server.

| Removed module | Packages | What Cerne used it for | Maven dependency that restores it (`javax` namespace, compatible with 11) |
|---|---|---|---|
| `java.xml.bind` (JAXB) | `javax.xml.bind` | XML statement export; reading the other credit union's file | `jakarta.xml.bind:jakarta.xml.bind-api:2.3.3` + `org.glassfish.jaxb:jaxb-runtime:2.3.3` (`runtime` scope) |
| `java.xml.ws` (JAX-WS) | `javax.xml.ws`, `javax.jws` | SOAP client for the interbank clearing service | `jakarta.xml.ws:jakarta.xml.ws-api:2.3.3` + `com.sun.xml.ws:jaxws-rt:2.3.3` |
| `java.xml.ws.annotation` (Common Annotations) | `javax.annotation` (`@PostConstruct`, `@Resource`) | CDI beans of the fee service | `jakarta.annotation:jakarta.annotation-api:1.3.5` |
| `java.activation` (JAF) | `javax.activation` | Statement email attachments | `jakarta.activation:jakarta.activation-api:1.2.2` (comes transitively with `jaxb-runtime`) |
| `java.transaction` (JTA) | `javax.transaction` | `@Transactional` outside the server, in the tests | `jakarta.transaction:jakarta.transaction-api:1.3.3` |
| `java.corba` | `org.omg.*`, `javax.rmi` | Nothing in Cerne | No official replacement; `org.glassfish.corba:glassfish-corba-orb` maintains the ORB; reassess the integration |
| `java.se.ee` (aggregator) | — | Nothing | None; remove it from `module-info` if present |
| JavaFX (`javafx.*`) | `javafx.*` | Nothing in Cerne (no desktop UI) | Was not part of JEP 320, but left Oracle's JDK in 11: `org.openjfx:javafx-controls` |

Watch the versions: the `2.3.x` artifacts of the `jakarta.*` APIs **still use the `javax.*` packages**. The package switch happens in versions `3.x`/`4.x`, the subject of Session 4. In the migration to 11 you want exactly `2.3.x`, so you don't change imports now.

The `xjc` and `wsimport` tools, which generated classes from XSD and WSDL, also left the JDK. The way forward is the corresponding Maven plugin (`jaxb2-maven-plugin` with `org.glassfish.jaxb:jaxb-xjc`; `jaxws-maven-plugin` with `com.sun.xml.ws:jaxws-tools`), with the same XSD/WSDL as before.

```xml
<!-- Restores JAXB for the statement exporter and the reconciliation reader -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>2.3.3</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.3</version>
    <scope>runtime</scope>
</dependency>
```

Without `jaxb-runtime` the code compiles (the API is there) and fails at runtime with `javax.xml.bind.JAXBException: Implementation of JAXB-API has not been found on module path or classpath`. It is a two-stage gotcha: the compilation error goes away, and the runtime error only shows up when someone generates a statement.

## 8 → 11 migration gotchas

### Internal `sun.*` APIs

| Usage on Java 8 | What happens on 11 | Replacement |
|---|---|---|
| `sun.misc.BASE64Encoder` / `BASE64Decoder` | Removed: compilation error | `java.util.Base64` (exists since 8) |
| `sun.reflect.Reflection.getCallerClass()` | Removed | `StackWalker` (Java 9) |
| `com.sun.image.codec.jpeg.*` | Removed | `javax.imageio.ImageIO` |
| `sun.security.x509.*` | Not exported: compilation error | Bouncy Castle or `java.security.cert` |
| `sun.misc.Unsafe` | Still in `jdk.unsupported`, accessible | None yet; upgrade the library that uses it |
| `sun.nio.ch.DirectBuffer` (`cleaner()`) | Not exported | Let the GC take care of it, or `--add-exports java.base/sun.nio.ch=ALL-UNNAMED` |

### Deep reflection

Deep reflection is `setAccessible(true)` on private members of **JDK** classes (not of your own classes; on yours it is still unrestricted). On 11 it produces the warning shown above; on 16 it starts failing. Serialization libraries (XStream, old Gson, old Jackson with `afterburner`), mocking libraries (PowerMock, old Mockito) and bytecode libraries (cglib, old Javassist) are the usual suspects. Use the warning to identify the library and upgrade it; use `--add-opens` as a last, documented resort.

### Removed JVM flags

An unknown flag prevents the JVM from **starting**, and the message appears on the process's standard error, not in the application log, which confuses anyone who only looks at the log.

```
Unrecognized VM option 'UseParNewGC'
Error: Could not create the Java Virtual Machine.
Error: A fatal exception has occurred. Program will exit.
```

| Common flag on Java 8 servers | Status on 11 | What to use |
|---|---|---|
| `-XX:PermSize`, `-XX:MaxPermSize` | Ignored with a warning since 8 (PermGen is gone) | `-XX:MaxMetaspaceSize`, if you need a ceiling |
| `-XX:+UseParNewGC` | Removed in 10; the JVM does not start | Remove it; if you stay on CMS, ParNew is implicit |
| `-XX:+UseConcMarkSweepGC` | Deprecated in 9, works on 11 with a warning; removed in 14 | G1 (the default) |
| `-XX:+PrintGCTimeStamps`, `-XX:+PrintGCDateStamps` | Removed in 9; the JVM does not start | `-Xlog:gc*:file=gc.log:time,uptime` |
| `-XX:+PrintGCDetails`, `-Xloggc:file` | Deprecated in 9, still accepted on 11 | `-Xlog:gc*:file=gc.log` (JEP 271, unified logging) |
| `-Xbootclasspath/p:` | Removed in 9 | `--patch-module`, or rethink the reason |
| `-d64` | Removed in 10 | Remove it |
| `-XX:+AggressiveOpts` | Deprecated in 11, removed in 12 | Remove it |
| `-Xverify:none`, `-noverify` | Accepted on 11; deprecated in 13 | Remove them; the startup time gain is negligible |

Note that G1 became the default collector in 9. A Java 8 server with no GC flag ran Parallel; the same server on 11 with no flag runs G1. Shorter pauses, slightly lower throughput. That is one of the reasons stage 4 of the roadmap exists: part of the difference you are going to measure between `v8` and `v11` comes from there, not from the code.

### Incompatible libraries

Libraries that generate or inspect bytecode read the version number of the `.class` file (52 on Java 8, 55 on 11) and reject what they don't know. Libraries that use internal compiler APIs or deep reflection break for the reason in the previous section.

| Library | Symptom on 11 | Minimum version (approximate; check the release notes) |
|---|---|---|
| Lombok | Warning `Illegal reflective access by lombok.javac.apt.LombokProcessor` and, right after, dozens of `cannot find symbol` errors for getters, setters and constructors that Lombok stopped generating. On 17, the same version brings compilation down with `ExceptionInInitializerError: Unable to make field private ... accessible` | 1.18.4 (for 17, 1.18.22) |
| ASM (directly or via another library) | `IllegalArgumentException: Unsupported class file major version 55` | 7.0 |
| Hibernate ORM | Proxy failure via old Javassist | 5.4 (uses Byte Buddy) |
| Javassist | `Unsupported class file major version` | 3.24 |
| Mockito | Fails to create mocks (old Byte Buddy) | 2.23 |
| Spring Framework (if present) | Assorted proxy and reflection errors | 5.1 |
| cglib | Unmaintained; reflection warnings, fails on 16+ | Replace it with Byte Buddy (most libraries already have) |
| PowerMock | Fails to instrument classes | 2.0; consider removing it, it is the classic case of a test coupled to the implementation |
| JaCoCo | `Unsupported class file major version` | 0.8.3 |

> ⚠️ **Migration gotcha:** on 11, old Lombok does not fail. It emits a reflective access `WARNING` at the top of the log and simply does not generate the code, so the compiler reports `cannot find symbol: method getAmount()` in classes nobody touched. Anyone who does not know the symptom spends hours looking for the problem in Cerne when it is in the `pom.xml`. Rule of thumb: if the log mentions `lombok.javac.apt.LombokProcessor` or a `com.sun.tools.javac` package, the problem is an outdated annotation processor (Lombok, MapStruct, AutoValue). Upgrade it before reading any other error. On 17 the same version no longer warns: it brings compilation down with `ExceptionInInitializerError`, which at least makes the cause explicit.

> ✍️ **Try it now:** given the log excerpt below, captured when starting Cerne on 11, state in one sentence each fix that is needed.
>
> ```
> Unrecognized VM option 'PrintGCDateStamps'
> ```
>
> And, after fixing that:
>
> ```
> WARNING: Illegal reflective access by javassist.util.proxy.SecurityActions (file:/opt/server/lib/javassist-3.20.0-GA.jar) to method java.lang.ClassLoader.defineClass(...)
> javax.xml.bind.JAXBException: Implementation of JAXB-API has not been found on module path or classpath
> ```
>
> <details><summary>Show answer</summary>
>
> 1. Replace `-XX:+PrintGCDateStamps` (and probably its sibling flags) with `-Xlog:gc*:file=gc.log:time,uptime` in the server's JVM options file. 2. Upgrade Javassist to 3.24+ (or, more likely, upgrade Hibernate, which brings it in transitively and has used Byte Buddy since 5.4). 3. `jaxb-runtime` in `runtime` scope is missing; only the API was added. None of the three fixes touches Java code.
>
> </details>

## Worked exercise: the first four build errors

**Situation.** Cerne's `pom.xml` moved to `<release>11</release>`. First `mvn clean verify`:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by lombok.javac.apt.LombokProcessor to field
         com.sun.tools.javac.processing.JavacProcessingEnvironment.discoveredProcs
...
[ERROR] /cerne/src/main/java/cerne/statements/XmlStatementExporter.java:[3,22] package javax.xml.bind does not exist
[ERROR] /cerne/src/main/java/cerne/reconciliation/FileSigner.java:[5,16] cannot find symbol
  symbol:   class BASE64Encoder
  location: package sun.misc
... (more errors in XmlStatementExporter, ReconciliationFileReader and SoapClearingClient)
```

`javac` stops at the first batch of symbol errors. Once those are fixed, the second round reveals the next batch:

```
[ERROR] /cerne/src/main/java/cerne/statements/StatementService.java:[40,36] cannot find symbol
  symbol:   method getAccounts()
  location: variable member of type cerne.members.Member
... (dozens of getters and setters in classes nobody changed)
```

**Step 1: annotation processor.** The warning at the top mentions `lombok.javac.apt.LombokProcessor`, and the getters that "disappeared" are precisely the ones Lombok generates. Lombok 1.16.20 → 1.18.30, before reading any other error. The getter `cannot find symbol` errors go away.

**Step 2: removed modules.** Most of the remaining errors are missing `javax.xml.bind` and `javax.xml.ws`. In come the dependencies `jakarta.xml.bind-api:2.3.3` + `jaxb-runtime:2.3.3` and `jakarta.xml.ws-api:2.3.3` + `jaxws-rt:2.3.3`. No import changes.

> ⚠️ **Migration gotcha:** if the project compiles against the Java EE API (`javax:javaee-api:7.0` or `8.0` with `provided` scope, common in systems that run on an application server), the JAXB API comes in that artifact and the compilation error **does not appear**. The project compiles, and the failure shows up at runtime: `ClassNotFoundException: com.sun.xml.internal.bind.v2.ContextFactory` when the code builds a `JAXBContext`. The API is there; the implementation that lived in the JDK is gone. In tests, add `jaxb-runtime:2.3.3` with `runtime` scope; on the server, check whether it already provides the implementation. Only a test that really uses JAXB reveals this before staging.

**Step 3: internal API.** The two `BASE64Encoder` errors remain. The class signs the reconciliation file with HMAC and encodes it in Base64:

```java
// Java 8
import sun.misc.BASE64Encoder;
...
String signature = new BASE64Encoder().encode(mac.doFinal(content));

// Java 11
import java.util.Base64;
...
String signature = Base64.getEncoder().encodeToString(mac.doFinal(content));
```

Beware of a behavioral difference: `BASE64Encoder.encode` inserted a line break every 76 characters; `Base64.getEncoder()` does not. For the signature, what decides is the size of the HMAC, not the size of the file: the HMAC has a fixed size. With HMAC-SHA256, that is 32 bytes, which become 44 characters, below 76, and both classes produce the same text for any file. With HMAC-SHA512, that is 64 bytes and 88 characters; `BASE64Encoder` broke the line after the 76th, and the result now differs. The difference also shows up when encoding long content, such as the file itself. `Base64.getMimeEncoder()` also breaks every 76 characters, but with `\r\n`, whereas `BASE64Encoder` used the system line separator (`\n` on Linux): the result is not identical to the old one; the choice depends on the contract with the partner, and the test that compares against a reference file (*golden file*) is what reveals the difference. Clean compilation.

**Step 4: tests.** `mvn verify` compiles and runs the suite. Two tests fail in `ReconciliationFileReaderTest`: `NullPointerException` in `Set.of(...)`, because a test built the set of expected types with a deliberate `null` to represent "no type". `Set.of` does not accept `null`. Fix: "no type" becomes an `EntryType.UNCLASSIFIED` constant, which is what the domain should already have had. Green suite.

**Step 5: run.** Application server upgraded to the version certified for JDK 11. First startup: `Unrecognized VM option 'PrintGCTimeStamps'`. GC flags replaced with `-Xlog:gc*:file=/var/log/cerne/gc.log:time,uptime,level,tags`. Second startup: application up, with 40 lines of `WARNING: Illegal reflective access` from XStream. XStream 1.4.9 → 1.4.20. Third startup: clean log.

**Step 6: measure and tag.** Processing 500 thousand entries from the Session 2 load file, three runs, same machine: `v8` 41 s of total time and a maximum GC pause of 1.8 s; `v11` 38 s and 190 ms. The pause dropped because the collector changed from Parallel to G1, not thanks to the code. `git tag v11`.

## Hands-on challenge

**Assignment.** At Cerne's `v11` milestone, two classes still carry migration debt:

1. `CpfLookupClient` was migrated minimally (it just compiles). Replace `HttpURLConnection` with the Java 11 HTTP Client, with connection and request timeouts configured, handling of non-200 status codes and of `InterruptedException`, and a single `HttpClient` instance per application.
2. `XmlStatementExporter` depends on JAXB, which was restored through the external dependency. Confirm the migration with a test that generates the statement of an account with three entries and compares the XML with the reference file that already exists in `src/test/resources/statements/`.

**Acceptance criteria.**

- `mvn clean verify` passes with `<release>11</release>` and `jdeps -jdkinternals` reports no class from `cerne.members` or `cerne.statements`.
- `CpfLookupClient` imports nothing from `java.net.HttpURLConnection`; the `HttpClient` is a field of an application-scoped bean.
- There is a test that simulates a slow response (a local HTTP server with a delay longer than the timeout) and verifies that the lookup fails with `HttpTimeoutException` instead of hanging.
- The XML exporter test compares against the reference file and passes; if the output differs in whitespace or attribute order, the difference is documented in the test with the reason.
- The `pom.xml` contains no `jakarta.*` artifact at version `3.x` or later (we are still in the `javax` namespace).

Walkthrough of the solution at the beginning of the next session.

## Conclusion

The 8 → 11 migration is less about language and more about inventory: `jdeps` tells you where the internal APIs are, `jakarta.*` at version `2.3.x` restores what JEP 320 took out of the JDK, `--add-opens` holds up whatever does not yet have an upgraded library, and the JVM flags translate to `-Xlog`. Once that is done, `var`, `List.of`, the new `String` methods and the HTTP Client are the reward: shorter code with no new dependency. The step-by-step roadmap (inventory, compile, test, run, measure) is the same one you will repeat in the next session, when Cerne moves up to 17 and to Jakarta EE 10: there, the `javax` → `jakarta` namespace switch touches every EJB, CDI, JPA and JAX-RS import, the reflection warnings become errors, JUnit goes from 4 to 5, and the domain gains `record` and `sealed`.

## References

- Oracle. *Oracle JDK Migration Guide, Release 21*. docs.oracle.com/en/java/javase/21/migrate/ (sections "Removed APIs", "Removed Tools and Components" and "Strong Encapsulation").
- OpenJDK. *JEP 261: Module System*. openjdk.org/jeps/261.
- OpenJDK. *JEP 286: Local-Variable Type Inference*. openjdk.org/jeps/286.
- OpenJDK. *JEP 320: Remove the Java EE and CORBA Modules*. openjdk.org/jeps/320.
- OpenJDK. *JEP 321: HTTP Client*. openjdk.org/jeps/321.
- OpenJDK. *JEP 271: Unified GC Logging*. openjdk.org/jeps/271.
- Marks, Stuart. *Local Variable Type Inference: Style Guidelines*. openjdk.org/projects/amber/guides/lvti-style-guide.
- Bloch, Joshua. *Effective Java*. 3rd edition. Addison-Wesley, 2018. Items 17 (minimize mutability), 28 (prefer lists to arrays) and 59 (know and use the libraries).
- Horstmann, Cay S. *Core Java Volume II: Advanced Features*. 12th edition. Oracle Press, 2022. Chapters on the HTTP Client and on the module system.
- Urma, Raoul-Gabriel; Fusco, Mario; Mycroft, Alan. *Modern Java in Action*. 2nd edition. Manning, 2019. Chapter 14 (module system).
- Eclipse Foundation. *Jakarta XML Binding 2.3 (namespace `javax.xml.bind`)*. jakarta.ee/specifications/xml-binding/2.3/.
