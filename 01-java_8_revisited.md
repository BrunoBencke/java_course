# Java 8 revisited

Cerne, the core system of a credit union, has been running on Java 8 for ten years. It processes entries, transfers between members and to other banks, calculates fees, generates statements and reconciles the day's activity with the central credit union. Nobody complains: the system "works". The question that opens this module has two parts: why migrate a system that works, and what exactly, in today's code, is going to hurt once the migration starts?

This session does not teach Java 8. It revisits Java 8 through the eyes of someone about to leave it behind: which features the version brought, which gaps it left, and which code smells those gaps induced over a decade of maintenance. By the end, you will have a concrete list of places in Cerne that need attention before any `pom.xml` changes version.

## Cerne at the starting point

The `v8` milestone of the repository is the system as it runs in production. The stack is the 2014 one: Java 8, a Java EE 7 application server, `javax.*` everywhere, EJB for transactions, CDI for injection, JPA 2.1 for persistence and JUnit 4 for tests. The code is organized by feature, with names taken from the business vocabulary:

```text
cerne/
├── members/          Member, Account, Branch, MemberRepository, MemberService
├── entries/          Entry, EntryType, EntryRepository, EntryReport
├── transfers/        Transfer, TransferService, TransferValidator, TransferReconciliation
├── fees/             Fee, FeeCalculator
├── statements/       StatementService, StatementGenerator
├── externallookup/   CpfLookupClient
└── util/             CerneUtil  (1,900 lines)
```

These conventions apply to the whole module and to all the code you will write in the challenges:

| Convention | How it applies | Why it matters in the migration |
|---|---|---|
| Business-vocabulary names | `Entry`, `TransferService`, `calculateFee()`, `activeMember` | Names mirror the credit union's domain language (entry, member, fee, statement); generic technical names (`Record`, `Processor`, `doCalc()`) blur meaning and pull the code away from the vocabulary the business actually uses |
| One responsibility per class | A class does one thing and has one reason to change | Each version jump touches one API at a time; cohesive classes isolate the impact of each change |
| Packages by feature | `cerne.entries`, not `cerne.services` + `cerne.repositories` | With the module system (session 3), the package boundary becomes the export boundary |

> 🎬 **Picture this:** the credit union's security team receives an audit report pointing out that the application server and the JDK in production have not received security fixes in years. The IT manager asks: "how long will it take to upgrade?". Nobody can answer, because nobody knows what is going to break. This module exists so that, by the end, you can answer that question with an inventory in hand, not with a hallway guess.

## What Java 8 brought and what it left missing

Java 8 was the biggest change to the language since generics in Java 5. Lambdas, Streams, `Optional`, `java.time`, `default` methods in interfaces and the removal of PermGen changed the way Java is written. At the same time, Java 8 dates from 2014, and much of what we now consider basic did not exist yet.

| What Java 8 has | What is still missing in 8 | Arrives in |
|---|---|---|
| Lambdas and method references | Local variable type inference (`var`) | 10 |
| Streams and Collectors | `Collectors.toList()` directly on the Stream, `takeWhile`, `teeing` | 9, 12, 16 |
| `Optional` | `ifPresentOrElse`, `or`, no-argument `orElseThrow()` | 9, 10 |
| `java.time` | Nothing missing here; the problem is the code that still uses `java.util.Date` | — |
| `default` and `static` methods in interfaces | Private methods in interfaces | 9 |
| `String.join`, `StringJoiner` | `isBlank`, `strip`, `lines`, `repeat`, text blocks | 11, 15 |
| Anonymous classes and inner classes | Records, sealed classes, pattern matching | 16, 17, 21 |
| `switch` on `String` | Switch expressions, pattern matching for `switch` | 14, 21 |
| `HttpURLConnection` | Modern HTTP Client | 11 |
| Parallel GC as the default collector | G1 as the default; ZGC | 9, 15 |
| Flat classpath | Module system (JPMS) and strong encapsulation | 9, 16 |
| JAXB, JAX-WS and CORBA inside the JDK | These modules are removed from the JDK; they become dependencies | 11 |

Look at the last row. It is not a gap: it is a Java 8 feature that is going to disappear. Cerne uses JAXB to serialize the reconciliation file sent to the central credit union. That code compiles today without any extra dependency and will stop compiling on Java 11. Write that down; it comes back in session 3.

> 💭 **Stop and think:** look at your system's `pom.xml` (or `build.gradle`). How many dependencies have had their version "frozen" since 2015 or 2016? For how many of them can you say whether they still publish versions compatible with Java 8? If the answer is "I don't know", this session's individual research is for you.

## Timeline: from 8 to 21

Up to Java 8, a new version shipped when it was ready: 7 took five years, 8 took three. In 2017, Oracle changed the policy: one release every six months (March and September), and one of them, every few years, gets long-term support, LTS (*Long-Term Support*). Non-LTS releases are superseded six months later and stop receiving updates.

| Version | Release | LTS | End of public support (Oracle Premier Support) | Extended support (Oracle) |
|---|---|---|---|---|
| 8 | March 2014 | Yes | March 2022 | December 2030 |
| 9 | September 2017 | No | March 2018 (superseded by 10) | — |
| 10 | March 2018 | No | September 2018 | — |
| 11 | September 2018 | Yes | September 2023 | January 2032 |
| 12 to 16 | 2019 to 2021 | No | six months after each release | — |
| 17 | September 2021 | Yes | September 2026 | September 2029 |
| 18 to 20 | 2022 to 2023 | No | six months after each release | — |
| 21 | September 2023 | Yes | September 2028 | September 2031 |

Three remarks about the table:

1. **The dates are Oracle's.** Other OpenJDK distributions (Eclipse Temurin, Amazon Corretto, Azul Zulu, Red Hat) have their own calendars, usually similar. Check the source of the distribution you run in production; this session's individual research asks for exactly that.
2. **"Extended support" costs money.** Java 8 gets fixes until 2030, but only for those who pay for a subscription. Free public updates of Oracle JDK 8 for commercial use ended in January 2019.
3. **The cycle goes on.** The interval between LTS releases dropped from three years (11 → 17) to two (17 → 21), and the next LTS after 21 kept the same pace. This module stops at 21 because it is the LTS that the ecosystem of libraries and application servers adopted as its target; the migration method you are going to practice works for the following jumps.

### Why 11, 17 and 21 are the milestones

The migration does not go from 8 straight to 21 in a single step because each LTS concentrates one kind of breakage:

| Jump | What really changes | Where Cerne is going to hurt |
|---|---|---|
| 8 → 11 | JPMS, removal of JAXB/JAX-WS/CORBA, encapsulation of internal APIs starts to warn | Reconciliation file (JAXB), `sun.misc` in the CPF lookup client |
| 11 → 17 | Strong encapsulation (reflection on internal `java.*` blocked), Jakarta EE 9/10 switches `javax` → `jakarta`, JUnit 5 | Every `import javax.*`, every JUnit 4 test, libraries that open JDK classes via reflection |
| 17 → 21 | Few breakages; many opportunities (virtual threads, pattern matching for `switch`) | Little pain; a lot of gain |

The pattern is: **the pain concentrates in the first two jumps, the gain in the third**. That is why sessions 3 and 4 are the densest in gotchas, and session 5 is the lightest.

### The cost of falling behind

"It works" is a strong argument until the day it stops being one. Three costs grow silently while the system stays on Java 8:

- **Security.** Vulnerabilities in the JDK and in the application server are only fixed in supported versions. A JDK 8 without a paid subscription has not received fixes since 2019.
- **Libraries.** The ecosystem has dropped Java 8 as the minimum target. Jakarta EE 10 requires Java 11 or later (with Java 17 supported); Spring Framework 6 and Spring Boot 3 require Java 17; Hibernate 6 requires Java 11. Each library Cerne uses will, one by one, stop publishing a version compatible with 8. You get stuck on the last version that worked, with whatever bugs it had.
- **Hiring and retention.** Developers entering the job market today learned Java with records, `var` and switch expressions. Keeping a system on Java 8 means keeping a system that the new team considers archaic and that the old team is tired of maintaining.

> ⚠️ **Migration gotcha:** "let's upgrade just the JDK and leave the libraries as they are" is everybody's first idea, and the first to fail. Old libraries that use reflection on internal JDK classes (object mappers, mocking frameworks, serialization libraries) break at runtime on Java 17 with `InaccessibleObjectException`, even if Cerne's code never touches those classes. The symptom shows up far from the cause: a statement screen returns a 500 error because the 2016 JSON serializer tried to open `java.util.Date` via reflection. The fix is to upgrade the library, not your code. That is why the dependency inventory comes before any version change.

> ✍️ **Try it now:** on your machine, with Maven, run the command below on the project you maintain (or on Cerne `v8`) and count how many dependencies have a newer version available.
>
> ```sh
> mvn versions:display-dependency-updates
> ```
>
> Split the result into three groups: (a) has a newer version that still supports Java 8, (b) the newer version requires Java 11 or later, (c) no new release in more than three years.
>
> <details><summary>Show hint</summary>
> Group (c) is the most dangerous: a library with no release in three years has probably not been tested on Java 17 or 21, and you will find that out at runtime. For each item in group (c), check the project page for a maintained replacement. This survey is the core of this session's challenge.
> </details>

## Legacy code smells

Ten years of maintenance leave marks. None of the smells below is exclusive to Java 8, but each of them was, in part, induced by a gap in the language at the time, and each will charge its price during the migration.

| Smell | Symptom in the code | Why it hurts in the migration | What replaces it |
|---|---|---|---|
| Cascading nulls | `if (a != null && a.getB() != null && a.getB().getC() != null)`; methods that return `null` for "not found" | Records do not embrace the idea of "optional field via `null`" without explicit validation; pattern matching does not match `null`; every `NullPointerException` in production becomes a stalled migration | `Optional` as a return type; validation in the constructor (compact, in records); `Objects.requireNonNull` at the boundary |
| Giant utility classes | `CerneUtil` with 1,900 lines, all `static`, business rules mixed with formatting | Every module depends on it; under JPMS it has to be exported to everyone; impossible to migrate piecemeal; a shared static `SimpleDateFormat` is a latent bug | Methods on the type that knows the data (`Cpf.format()`, `Fee.calculate()`); `java.time` and `NumberFormat` per call; packages by feature |
| Inheritance as reuse | `FeeEntry extends Entry extends BaseEntity extends AuditableObject` | Records are `final`; sealed classes require you to decide who may extend; deep hierarchies hide which behavior comes from where | Composition; interfaces with `default` methods; sealed for closed domain hierarchies |
| `java.util.Date` and `Calendar` | `new Date()`, `SimpleDateFormat`, arithmetic with `Calendar.add` | `Date` is mutable, has no time zone and no semantics (is it a date? an instant?); JPA 2.2+ and Jakarta Persistence map `java.time` natively; modern JSON libraries treat `Date` as legacy | `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`, `DateTimeFormatter` |
| Imperative loops for everything | Three nested `for` loops with accumulators; `Collections.sort` with an anonymous `Comparator` | Does not break in the migration, but hides the intent; prevents using `Collectors`, records as DTOs and pattern matching naturally | Streams with `groupingBy` and `reducing` (session 2) |
| Internal JDK APIs | `sun.misc.BASE64Encoder`, `sun.misc.Unsafe`, `com.sun.*` | Warnings in 9, `--add-opens` mandatory in 16, error in 17 | Public equivalents: `java.util.Base64`, `VarHandle`, standard APIs |

The first four smells share a root: **the language offered no cheap way to express intent**. Before `Optional`, `null` was the only way to say "may not exist". Before records, an immutable class cost 40 lines, so nobody wrote one. Before `java.time`, `Date` was all there was. The smell is the rational 2014 response to a limitation that no longer exists.

> 💭 **Stop and think:** what is the longest class in the system you maintain? If its name ends in `Util`, `Helper`, `Manager` or `Commons`, how many different responsibilities does it mix? How many packages depend on it?

## Guided reading: three excerpts from Cerne

The three excerpts below were taken from the `v8` milestone with their smells preserved. Read each one before opening the answer and write down what bothers you. The order is deliberate: what you spot on your own sticks; what the instructor names for you, you forget.

### Excerpt 1: `StatementService`

```java
package cerne.statements;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import cerne.members.Branch;
import cerne.members.Account;
import cerne.members.Member;
import cerne.members.MemberRepository;
import cerne.entries.Entry;
import cerne.entries.EntryRepository;

@Stateless
public class StatementService {

    @Inject
    private MemberRepository members;

    @Inject
    private EntryRepository entries;

    public String statementHeader(String cpf) {
        Member member = members.findByCpf(cpf);
        String branchName = "";
        if (member != null) {
            Account account = member.getMainAccount();
            if (account != null) {
                Branch branch = account.getBranch();
                if (branch != null) {
                    if (branch.getName() != null) {
                        branchName = branch.getName();
                    }
                }
            }
        }
        if (branchName.equals("")) {
            branchName = "BRANCH NOT PROVIDED";
        }
        String memberName = member == null ? "" : member.getName();
        return "STATEMENT - " + branchName + " - " + memberName;
    }

    public List<Entry> entriesForPeriod(String cpf, Date start, Date end) {
        Member member = members.findByCpf(cpf);
        if (member == null) {
            return null;
        }
        List<Entry> result = entries.findByAccount(
                member.getMainAccount().getNumber(), start, end);
        if (result == null) {
            return null;
        }
        return result;
    }
}
```

> ✍️ **Try it now:** in two minutes, list everything that bothers you in this excerpt. Don't fix anything yet; just list.
>
> <details><summary>Show answer</summary>
>
> - **Cascading nulls** in `statementHeader`: four levels of `if` to reach a name. The pyramid hides the business rule ("the header shows branch and member, with a default value when there is no branch").
> - **`null` as a business result** in `entriesForPeriod`: the caller needs to know that `null` means "member does not exist" and that an empty list means "exists, but had no activity". That distinction lives nowhere except in the head of whoever wrote it.
> - **Inconsistency**: `statementHeader` guards `getMainAccount()` against `null`; `entriesForPeriod` does not. One of the two is wrong, and only production will tell which.
> - **`java.util.Date` in the parameters**: `start` and `end` are calendar dates (statement from the 1st to the 30th), not instants; `Date` carries a time and a time zone that nobody wants.
> - **`"BRANCH NOT PROVIDED"` in plain uppercase ASCII**: in the original system this label is shown to members in Portuguese, and its accents had been stripped (`AGÊNCIA NÃO INFORMADA` became `AGENCIA NAO INFORMADA`), a sign that at some point the encoding broke and someone "fixed" it by removing the accents. It shows up again in session 3, when the JVM's default encoding changes to UTF-8 (Java 18, inherited by 21).
>
> The version fixed with `Optional` is left for session 2. Here, what matters is seeing.
> </details>

### Excerpt 2: `CerneUtil`

```java
package cerne.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public final class CerneUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private CerneUtil() {}

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static Date parseDate(String text) throws Exception {
        return DATE_FORMAT.parse(text);
    }

    public static boolean isValidCpf(String cpf) {
        // 40 lines of check-digit calculation
        return true;
    }

    public static String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
                + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatAmount(BigDecimal amount) {
        return "R$ " + amount.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString().replace(".", ",");
    }

    public static BigDecimal calculateFee(String transferType, BigDecimal amount) {
        if ("INTERNAL".equals(transferType)) {
            return BigDecimal.ZERO;
        } else if ("BETWEEN_CREDIT_UNIONS".equals(transferType)) {
            return new BigDecimal("1.50");
        } else if ("OTHER_BANK".equals(transferType)) {
            return amount.multiply(new BigDecimal("0.005")).max(new BigDecimal("3.00"));
        }
        return BigDecimal.ZERO;
    }

    public static boolean isBusinessHours(Date instant) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(instant);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 8 && hour < 18;
    }

    public static String generateTransferIdentifier() {
        return "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static void log(String message) {
        System.out.println(DATE_TIME_FORMAT.format(new Date()) + " " + message);
    }

    // ... 1,800 more lines: branch validation, balance calculation,
    // building the reconciliation XML, reading properties, etc.
}
```

> 💭 **Stop and think:** `calculateFee` is a credit union business rule. `formatCpf` is presentation. `log` is infrastructure. `parseDate` is conversion. What do these four things have in common to live in the same class? The honest answer is usually "nothing, but it was the easiest place to put them".

The excerpt has a latent bug that is not about organization but about concurrency: `SimpleDateFormat` **is not thread-safe**, and the `DATE_FORMAT` constant is shared by every thread of the application server. Under load, two simultaneous calls to `formatDate` can produce swapped dates or throw `NumberFormatException` from inside `parse`. This bug exists today, in production, and shows up as "an intermittent error nobody can reproduce". The replacement is `DateTimeFormatter`, which is immutable and thread-safe, and which only makes sense once `Date` leaves the system.

There is also `BigDecimal.ROUND_HALF_EVEN`: this integer constant has been deprecated since Java 9 in favor of `RoundingMode.HALF_EVEN`. It still compiles on 21, with a warning, but it is the kind of detail that piles up into hundreds of warnings and hides the ones that matter.

### Excerpt 3: `Entry`

```java
package cerne.entries;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;

@Entity
public class Entry {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    private String type; // "CREDIT", "DEBIT", "FEE"

    private double amount;

    private String description;

    public boolean isInCurrentMonth() {
        Calendar today = Calendar.getInstance();
        Calendar entryDate = Calendar.getInstance();
        entryDate.setTime(date);
        return entryDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && entryDate.get(Calendar.YEAR) == today.get(Calendar.YEAR);
    }

    public boolean isCredit() {
        return "CREDIT".equals(type);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    // remaining getters and setters
}
```

> ✍️ **Try it now:** this excerpt has three problems that are not about style but about correctness. Find them.
>
> <details><summary>Show answer</summary>
>
> 1. **`double amount` for money.** `0.1 + 0.2` in `double` is not `0.3`. In a system that adds up thousands of entries a day, the difference shows up in reconciliation as an "extra R$ 0.01" nobody can explain. The right type is `BigDecimal` (or `long` in cents). This is not a Java 8 smell; it is a timeless one. But the migration to records will force you to decide the type, so decide now.
> 2. **`getDate()` returns the internal `Date`, which is mutable.** Any code that does `entry.getDate().setTime(...)` changes the entity without going through the setter and without JPA noticing. With `LocalDateTime`, which is immutable, the problem disappears by construction.
> 3. **`isInCurrentMonth()` depends on the JVM's default time zone.** `Calendar.getInstance()` uses the machine's time zone. An entry made at 11:30 PM on March 31 on a server configured in UTC may be "from April" on another. `YearMonth.from(date)` with an explicit time zone solves it, and the unit test becomes possible without tampering with the machine's clock.
>
> Bonus: `String type` with magic values. An `enum EntryType` would cost five lines and eliminate the `"CREDIT".equals(type)` scattered across the system. In session 4, that `enum` becomes the basis of a `sealed` hierarchy.
> </details>

The three excerpts show a pattern: **smells accumulate at the boundaries**. Where data comes in (the `Date` parameter), where data goes out (`return null`), where data is stored (`double`, `String type`). The migration will force decisions at each boundary, and it is better to make them while the system still compiles on Java 8 than in the middle of a version jump.

> 🎬 **Picture this:** the business area asks for the statement to show the day's balance on every line. A two-hour task. The developer opens `StatementService`, sees the `null` pyramid, opens `CerneUtil` to find out how to format the date, finds three methods that do it in slightly different ways, and discovers that `Entry.amount` is a `double`. The two hours become two days, and the task goes into the backlog labeled "needs refactoring first". That backlog is the real cost of legacy, and it does not appear in any report.

## Worked exercise: inventory of internal JDK APIs

**Problem.** Before any migration, we need to know whether Cerne uses internal JDK APIs (`sun.*`, `com.sun.*`, `jdk.internal.*`). These APIs were never public, but Java 8's flat classpath let you use them. From Java 9 on they produce a warning; from 16 on, an error without `--add-opens`; on 17, strong encapsulation by default.

**Step 1: use the tool that already ships with JDK 8.** `jdeps` has existed since Java 8 and has the `-jdkinternals` option, which lists exactly the dependencies on internal APIs. Build Cerne and point `jdeps` at the artifact:

```sh
mvn -q package -DskipTests
jdeps -jdkinternals target/cerne-v8.jar
```

**Step 2: read the output.** A typical result on the `v8` milestone:

```text
cerne-v8.jar -> /opt/jdk8/jre/lib/rt.jar
   cerne.externallookup.CpfLookupClient (cerne-v8.jar)
      -> sun.misc.BASE64Decoder                             JDK internal API (rt.jar)
      -> sun.misc.BASE64Encoder                             JDK internal API (rt.jar)
   cerne.util.CerneUtil (cerne-v8.jar)
      -> sun.misc.BASE64Encoder                             JDK internal API (rt.jar)

Warning: JDK internal APIs are unsupported and private to JDK implementation that are
subject to be removed or changed incompatibly and could break your application.
Please modify your code to eliminate dependence on any JDK internal APIs.

JDK Internal API                         Suggested Replacement
----------------                         ---------------------
sun.misc.BASE64Encoder                   Use java.util.Base64 @since 1.8
sun.misc.BASE64Decoder                   Use java.util.Base64 @since 1.8
```

`jdeps` itself suggests the replacement. `sun.misc.BASE64Encoder` was removed in Java 9; `java.util.Base64` has existed since 8. In other words: the fix can be made now, without changing versions.

**Step 3: locate the usage.** The external CPF lookup client builds the HTTP Basic authentication header:

```java
// before (Java 8, internal API)
import sun.misc.BASE64Encoder;

String credentials = user + ":" + password;
String header = "Basic " + new BASE64Encoder().encode(credentials.getBytes("UTF-8"));
```

**Step 4: replace it with the public API.**

```java
// after (Java 8, public API)
import java.nio.charset.StandardCharsets;
import java.util.Base64;

String credentials = user + ":" + password;
String header = "Basic " + Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
```

Notice two details: `StandardCharsets.UTF_8` replaces the `"UTF-8"` `String` and eliminates the checked `UnsupportedEncodingException`; and `BASE64Encoder.encode` inserted line breaks every 76 characters, while `Base64.getEncoder()` does not. For HTTP Basic, the new behavior is the correct one; the old one worked by luck with short credentials.

**Step 5: run the tests and record it.** With JUnit 4, the lookup client's test stays green. Record it in the inventory:

| File | Internal API | Replacement | Can be fixed on 8? | Fixed |
|---|---|---|---|---|
| `CpfLookupClient` | `sun.misc.BASE64Encoder/Decoder` | `java.util.Base64` | Yes | Yes |

This is the inventory format the challenge asks for: one item per pain point, with the "can be fixed on 8?" column filled in. Everything that can be fixed while still on Java 8 should be, before the jump. Migrating with the system as clean as possible reduces the number of things that break at the same time.

> ⚠️ **Migration gotcha:** `jdeps -jdkinternals` analyzes your artifact, not your dependencies. A library in the `pom.xml` that uses `sun.misc.Unsafe` internally does not appear in this output. To include the dependencies, point `jdeps` at the directory containing all the jars (`target/lib/`, generated by `mvn dependency:copy-dependencies`) or, on newer JDK versions, use `--multi-release 8 --class-path 'target/lib/*'`. The output gets long; filter by `JDK internal API` and take notes library by library.

## Hands-on challenge

**Assignment.** Clone the Cerne repository at the `v8` milestone, compile it and run the test suite on Java 8. Then produce a migration inventory in Markdown (`INVENTORY.md` at the project root) with two sections:

1. **Dependencies**: for each dependency in the `pom.xml`, the current version, the latest available version, the minimum Java version required by the latest one, and the status (upgrade now; upgrade in the jump to 11; replace; no new release in more than three years).
2. **Code hotspots**: every use of an internal JDK API (`sun.*`, `com.sun.*`), of `java.util.Date`/`Calendar`/`SimpleDateFormat` and of JAXB/JAX-WS, with file, line, what replaces it and whether the fix can still be made on Java 8.

**Acceptance criteria.**

- The project compiles and all tests pass on Java 8 on your machine, with the command used recorded in the inventory.
- The dependencies section covers 100% of the direct dependencies, with a source (URL of the project page or of the Maven Central repository) for the minimum Java version.
- The code hotspots section was produced with the help of `jdeps -jdkinternals` and text search (`grep` or the IDE), and lists at least the hotspots in `CpfLookupClient`, `CerneUtil`, `Entry` and in the reconciliation file generator.
- Each hotspot has the "can be fixed on 8?" column filled in with yes or no and a one-line justification.
- The `Entry` class was fixed while still on Java 8 and submitted as a separate commit, with green tests: `amount` becomes `BigDecimal`, `date` becomes `LocalDate` and `type` becomes the `enum EntryType`. Session 2 starts from that version.

*Walkthrough of the solution at the beginning of the next session.*

## Conclusion

Java 8 brought lambdas, Streams, `Optional` and `java.time`, but left gaps (no `var`, no records, no modules, no HTTP Client) that, over ten years, turned into smells: cascading nulls, giant utility classes, inheritance as reuse, `Date` everywhere and imperative loops for everything. The timeline shows why 11, 17 and 21 are the milestones and why the pain of the migration concentrates in the first two jumps. The guided reading of the three Cerne excerpts gave us the method: spot the smell before naming it, fix on Java 8 whatever can be fixed on Java 8, and keep an inventory per pain point. In the next session, the per-member entry report, currently 40 lines of nested loops, will be rewritten with Streams, `Collectors` and `Optional`, and we will measure whether it became more readable without becoming slower.

## References

- Bloch, J. (2018). *Effective Java*. 3rd edition. Addison-Wesley. Items 54 (return empty collections, not `null`), 55 (return `Optional` judiciously) and 62 (avoid `String` where other types are more appropriate).
- Urma, R.-G.; Fusco, M.; Mycroft, A. (2019). *Modern Java in Action*. 2nd edition. Manning. Chapters 1 and 12 (`java.time`).
- Horstmann, C. S. (2022). *Core Java Volume I: Fundamentals*. 12th edition. Oracle Press. Chapter 6 (interfaces and lambdas).
- Oracle. *Oracle Java SE Support Roadmap*. https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- Oracle. *Java Platform, Standard Edition Migration Guide*, Java SE 21. https://docs.oracle.com/en/java/javase/21/migrate/
- Oracle. *jdeps: Java Class Dependency Analyzer*, Java SE 8 Tools Reference. https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jdeps.html
- OpenJDK. JEP 261: Module System. https://openjdk.org/jeps/261
- OpenJDK. JEP 320: Remove the Java EE and CORBA Modules. https://openjdk.org/jeps/320
- OpenJDK. JEP 396: Strongly Encapsulate JDK Internals by Default. https://openjdk.org/jeps/396
- OpenJDK. JEP 403: Strongly Encapsulate JDK Internals. https://openjdk.org/jeps/403
- Eclipse Foundation. *Jakarta EE 10 Platform Specification*. https://jakarta.ee/specifications/platform/10/
