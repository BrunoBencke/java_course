# Functional programming and Streams in depth

The per-member entry report is the screen the credit union's customer service team looks at most, and the method the development team avoids most. It is 40 lines of nested loops, three accumulators, an anonymous `Comparator` at the end and no test covering the middle path. Every time someone asks for "just one more column", somebody opens the method, reads it twice and sends the task back to the backlog. The question for this session: how do we rewrite this in a readable way without losing performance, and how do we prove, with measurements, that we did not lose any?

Cerne is still at the `v8` milestone. Everything shown here compiles on Java 8; where a later API would make things easier, it is marked with the version it arrives in, so you recognize it in session 3. We assume the previous session's challenge has been done: `Entry.amount` is already a `BigDecimal`, `Entry.date` is already a `LocalDate` and `type` is already the `enum EntryType`.

## The report as it stands today

```java
package cerne.entries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cerne.members.Account;
import cerne.members.Member;

public class EntryReport {

    public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                     LocalDate start, LocalDate end) {
        List<ReportLine> lines = new ArrayList<>();
        for (Member member : members) {
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            int count = 0;
            for (Account account : member.getAccounts()) {
                for (Entry entry : entries) {
                    if (entry.getAccountNumber().equals(account.getNumber())) {
                        if (!entry.getDate().isBefore(start) && !entry.getDate().isAfter(end)) {
                            if (entry.getType() == EntryType.CREDIT) {
                                totalCredit = totalCredit.add(entry.getAmount());
                            } else if (entry.getType() == EntryType.DEBIT
                                    || entry.getType() == EntryType.FEE) {
                                totalDebit = totalDebit.add(entry.getAmount());
                            }
                            count++;
                        }
                    }
                }
            }
            if (count > 0) {
                lines.add(new ReportLine(member.getName(), count, totalCredit, totalDebit));
            }
        }
        Collections.sort(lines, new Comparator<ReportLine>() {
            @Override
            public int compare(ReportLine a, ReportLine b) {
                return b.getTotalCredit().compareTo(a.getTotalCredit());
            }
        });
        return lines;
    }
}
```

Before rewriting, it is worth understanding what the method does and what it costs. It answers three questions per member: how many entries in the period, how much came in, how much went out. And it sorts by credit, descending. The cost is the hidden problem: for each account of each member, it walks through the **entire** list of entries. With 20 thousand members, 1.5 accounts per member and 2 million entries in the month, that is 60 billion comparisons. The report is "slow" not because Java 8 is slow, but because the algorithm is quadratic.

> 💭 **Stop and think:** in the system you maintain, which method does everybody avoid opening? How many nested loops does it have? If you had to explain in one sentence what it calculates, could you? If the answer is "no", the problem is not the loop; it is that the intent got lost among the accumulators.

## Lambdas, functional interfaces and method references

A lambda is a nameless function that the compiler converts into an instance of a **functional interface**: an interface with exactly one abstract method. Java 8 already ships with a catalog of them in `java.util.function`. You don't need to memorize the catalog; you need to recognize the six shapes that cover 95% of the cases.

| Interface | Signature | Reads as | Typical use in Cerne |
|---|---|---|---|
| `Predicate<T>` | `T -> boolean` | "is this entry...?" | `e -> e.getAmount().signum() > 0` |
| `Function<T, R>` | `T -> R` | "from this entry, give me..." | `Entry::getAccountNumber` |
| `Consumer<T>` | `T -> void` | "do this with each..." | `line -> printer.print(line)` |
| `Supplier<T>` | `() -> T` | "when needed, produce..." | `() -> new ArrayList<>()` |
| `UnaryOperator<T>` | `T -> T` | "transform, keeping the type" | `amount -> amount.setScale(2, RoundingMode.HALF_EVEN)` |
| `BinaryOperator<T>` | `(T, T) -> T` | "combine two into one" | `BigDecimal::add` |

There are two-parameter variants (`BiFunction`, `BiPredicate`) and primitive variants (`IntPredicate`, `ToLongFunction<T>`, `LongBinaryOperator`) that exist for a performance reason we will see in the section on parallel streams.

When the lambda only calls an existing method, a **method reference** is shorter and says the same thing. There are four forms:

| Form | Example | Equivalent lambda |
|---|---|---|
| Static method | `Objects::nonNull` | `x -> Objects.nonNull(x)` |
| Instance method of a specific object | `calculator::calculateFee` | `t -> calculator.calculateFee(t)` |
| Instance method of a type (the first parameter is the receiver) | `Entry::getAmount` | `e -> e.getAmount()` |
| Constructor | `ReportLine::new` | `(n, c, cr, d) -> new ReportLine(n, c, cr, d)` |

The report's anonymous `Comparator`, for example, becomes a single line:

```java
// before (Java 8, anonymous class)
Collections.sort(lines, new Comparator<ReportLine>() {
    @Override
    public int compare(ReportLine a, ReportLine b) {
        return b.getTotalCredit().compareTo(a.getTotalCredit());
    }
});

// after (Java 8, method reference + combinator)
lines.sort(Comparator.comparing(ReportLine::getTotalCredit).reversed());
```

`Comparator.comparing` takes a `Function` that extracts the key and returns a `Comparator` that can be combined with `reversed()` and `thenComparing(...)`. The rule "sort by credit, descending" is now written out, instead of encoded in `b.compareTo(a)`.

You can also declare your own functional interfaces when the name adds meaning to the domain:

```java
package cerne.fees;

import java.math.BigDecimal;

@FunctionalInterface
public interface FeeRule {
    BigDecimal apply(Transfer transfer);
}
```

`FeeRule` is technically a `Function<Transfer, BigDecimal>`, but the name says what it is. In session 8, this type becomes the basis of a Strategy with lambdas.

> ✍️ **Try it now:** rewrite the three snippets below with method references. One of them cannot be rewritten; identify which one and why.
>
> ```java
> entries.forEach(e -> System.out.println(e));
> entries.removeIf(e -> e.getAmount().signum() == 0);
> accounts.stream().map(a -> a.getNumber());
> ```
>
> <details><summary>Show answer</summary>
>
> ```java
> entries.forEach(System.out::println);
> entries.removeIf(e -> e.getAmount().signum() == 0); // not possible: the lambda calls two methods and compares
> accounts.stream().map(Account::getNumber);
> ```
>
> The second one does not become a method reference because the expression is not "a call to one method with the same arguments". If this rule shows up in more than one place, extract it into `Entry.isEmpty()` and use `Entry::isEmpty`. The criterion is always the same: a method reference is good when the method name already explains the intent.
> </details>

A detail that trips up people coming from loops: lambdas can only capture **effectively final** local variables. The report's accumulator `totalCredit = totalCredit.add(...)` cannot go into a lambda, because it is reassigned. This is not an arbitrary limitation: it is the compiler telling you that accumulating by reassignment is not the way to think in pipelines. The way is `reduce` or a `Collector`.

## Streams as a pipeline

A `Stream` is not a collection. It is a **description of processing** over a data source: the source (`list.stream()`), zero or more intermediate operations (`filter`, `map`, `sorted`) and a terminal operation (`collect`, `reduce`, `forEach`, `count`). Nothing happens until the terminal operation is called; the intermediate ones only assemble the pipeline. This is **lazy evaluation**.

That has a practical consequence that surprises people: elements go through the pipeline **one at a time, vertically**, not stage by stage. Look:

```java
List<Entry> entries = repository.findByDay(LocalDate.now());

List<String> accounts = entries.stream()
        .filter(e -> {
            System.out.println("filter " + e.getId());
            return e.getAmount().compareTo(new BigDecimal("1000")) > 0;
        })
        .map(e -> {
            System.out.println("map " + e.getId());
            return e.getAccountNumber();
        })
        .limit(2)
        .collect(Collectors.toList());
```

With ten entries, of which ids 3 and 5 are the first two above one thousand, the output is:

```text
filter 1
filter 2
filter 3
map 3
filter 4
filter 5
map 5
```

`filter` did not run for all ten and then `map` for the ones that passed. Each element travels down the whole pipeline, and `limit(2)` **stopped** the processing as soon as it had two results. Entries 6 to 10 were never even read. This is what makes the order of operations matter: `filter` before `map` avoids transforming what is going to be discarded; `limit` and `findFirst` before `sorted` don't work the way you expect, because `sorted` needs to see everything before emitting the first element.

The operations you will use in Cerne:

| Operation | Kind | What it does | Example |
|---|---|---|---|
| `filter(Predicate)` | intermediate | Keeps the ones that pass | `.filter(e -> e.getType() == EntryType.FEE)` |
| `map(Function)` | intermediate | Transforms one into one | `.map(Entry::getAmount)` |
| `flatMap(Function<T, Stream<R>>)` | intermediate | Transforms one into many and flattens | `.flatMap(m -> m.getAccounts().stream())` |
| `sorted(Comparator)` | intermediate, stateful | Sorts (needs to see everything) | `.sorted(Comparator.comparing(Entry::getDate))` |
| `distinct()` | intermediate, stateful | Removes duplicates by `equals` | `.map(Entry::getAccountNumber).distinct()` |
| `limit(n)` / `skip(n)` | intermediate | Truncates | `.limit(10)` |
| `reduce(identity, BinaryOperator)` | terminal | Combines everything into one value | `.reduce(BigDecimal.ZERO, BigDecimal::add)` |
| `collect(Collector)` | terminal | Accumulates into a structure | `.collect(Collectors.groupingBy(...))` |
| `findFirst()` / `anyMatch(...)` | terminal, short-circuiting | Stops at the first one | `.anyMatch(e -> e.getAmount().signum() < 0)` |
| `count()` | terminal | Counts | `.count()` |

`flatMap` deserves an example, because it is what solves "a member has accounts, an account has entries, I want the member's entries":

```java
List<Entry> memberEntries = member.getAccounts().stream()
        .flatMap(account -> repository.findByAccount(account.getNumber()).stream())
        .collect(Collectors.toList());
```

Without `flatMap`, `map` would produce a `Stream<List<Entry>>`, a list of lists. `flatMap` "flattens" it into a `Stream<Entry>`.

> ⚠️ **Migration gotcha:** a `Stream` is single-use. The code below is common in hasty refactorings and throws `IllegalStateException: stream has already been operated upon or closed` at runtime, not at compile time:
>
> ```java
> Stream<Entry> credits = entries.stream().filter(Entry::isCredit);
> long count = credits.count();
> BigDecimal total = credits.map(Entry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); // blows up
> ```
>
> The fix is not to keep the `Stream` in a variable: keep the filtered list (`collect`) if you need to go through it twice, or do both calculations in a single `collect` with a composite `Collector`, as we will see next.

> 🎬 **Picture this:** customer service asks for a report of "members who were charged fees above R$ 50 in the month". The developer writes `filter` + `map` + `distinct` in four lines, tests it and delivers it in an hour. The following week, they ask for the same thing, but "only for accounts opened in 2024". She adds a `filter`. The nested-loop report, with the same change, would require figuring out where to fit one more `if` in the middle of three levels. That difference in the cost of change is what justifies the pipeline, not the savings in lines.

## Rewriting the report

Now, the report. We are not going to translate loop by loop; we are going to rewrite it from the intent, in steps.

**Step 1: eliminate the quadratic algorithm.** The performance problem is walking through all the entries for each account. The solution is to index the entries by account **once**. That is `groupingBy`:

```java
Map<String, List<Entry>> entriesByAccount = entries.stream()
        .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
        .collect(Collectors.groupingBy(Entry::getAccountNumber));
```

One pass over 2 million entries, one `Map` with a list per account. From here on, finding an account's entries costs one map lookup.

**Step 2: build one member's line.** With the index, a member's entries are the `flatMap` of their accounts. And the two totals (credit and debit) are a partition:

```java
private ReportLine lineFor(Member member, Map<String, List<Entry>> entriesByAccount) {
    List<Entry> memberEntries = member.getAccounts().stream()
            .map(Account::getNumber)
            .flatMap(number -> entriesByAccount.getOrDefault(number, Collections.emptyList()).stream())
            .collect(Collectors.toList());

    Map<Boolean, BigDecimal> totals = memberEntries.stream()
            .collect(Collectors.partitioningBy(Entry::isCredit,
                    Collectors.reducing(BigDecimal.ZERO, Entry::getAmount, BigDecimal::add)));

    return new ReportLine(member.getName(), memberEntries.size(), totals.get(true), totals.get(false));
}
```

`partitioningBy` splits into two groups (`true` and `false`) and the **downstream collector** `reducing` sums each group. A `Map<Boolean, BigDecimal>` always has both keys, even if one group is empty; that is why `totals.get(false)` is never `null`.

**Step 3: put it all together.**

```java
public List<ReportLine> generate(List<Member> members, List<Entry> entries,
                                 LocalDate start, LocalDate end) {
    Map<String, List<Entry>> entriesByAccount = entries.stream()
            .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
            .collect(Collectors.groupingBy(Entry::getAccountNumber));

    return members.stream()
            .map(member -> lineFor(member, entriesByAccount))
            .filter(line -> line.getCount() > 0)
            .sorted(Comparator.comparing(ReportLine::getTotalCredit).reversed())
            .collect(Collectors.toList());
}
```

Read the `generate` method out loud: "index the period's entries by account; for each member, build the line; discard those with no activity; sort by credit, descending; return the list". That is exactly the report's specification. The 40-line method became two methods of 8 and 10 lines, each with one responsibility, and the cost dropped from quadratic to linear.

A detail about the date `filter`: `!isBefore(start) && !isAfter(end)` is the way to say "between start and end, inclusive". If this expression shows up in more places, extract it into `Entry.isBetween(start, end)`; it becomes readable and testable.

### The collectors you will use

`Collectors` is the toolbox of the terminal operation `collect`. This session's individual research asks you to compare `groupingBy`, `partitioningBy` and `toMap` in the documentation; the table below is the starting point.

| Collector | Result | When to use | Watch out |
|---|---|---|---|
| `toList()`, `toSet()` | `List<T>`, `Set<T>` | Materialize the result | Concrete type not guaranteed (don't count on `ArrayList`) |
| `toMap(key, value)` | `Map<K, V>` | One value per key | Throws `IllegalStateException` on a duplicate key; use the version with a merge `BinaryOperator` |
| `groupingBy(classifier)` | `Map<K, List<T>>` | Group by a key with N values | Unordered `HashMap`; use `groupingBy(f, TreeMap::new, toList())` to sort |
| `groupingBy(classifier, downstream)` | `Map<K, R>` | Group and summarize each group | The downstream can be another `groupingBy` (two-level grouping) |
| `partitioningBy(predicate)` | `Map<Boolean, List<T>>` | Split in two | Always has the keys `true` and `false` |
| `counting()` | `Long` | Count per group | Returns `Long`, not `long` |
| `summingInt/Long/Double` | primitive | Sum per group | There is no `summingBigDecimal`; use `reducing` |
| `reducing(identity, mapper, operator)` | `T` | Reduce per group with an arbitrary type | It is the way to sum `BigDecimal` per group |
| `mapping(f, downstream)` | depends on the downstream | Transform before collecting | `mapping(Entry::getAmount, toList())` |
| `joining(", ")` | `String` | Concatenate | Only accepts `Stream<CharSequence>`; do `map(Object::toString)` first |
| `averagingDouble`, `summarizingLong` | `Double`, `LongSummaryStatistics` | Simple statistics | Work with primitives; not suitable for `BigDecimal` |

Two-level grouping, which solves "total per member and per entry type" in a single expression:

```java
Map<String, Map<EntryType, BigDecimal>> totalByAccountAndType = entries.stream()
        .collect(Collectors.groupingBy(Entry::getAccountNumber,
                Collectors.groupingBy(Entry::getType,
                        Collectors.reducing(BigDecimal.ZERO, Entry::getAmount, BigDecimal::add))));
```

Read it from the inside out: "sum the amounts; group the sums by type; group the maps by account".

> ⚠️ **Migration gotcha:** `Collectors.toMap` without a merge function is the most common source of production errors after a refactoring to Streams. The code below works in the tests (one entry per account) and throws `IllegalStateException: Duplicate key` on the first day with two entries on the same account:
>
> ```java
> Map<String, BigDecimal> balanceByAccount = entries.stream()
>         .collect(Collectors.toMap(Entry::getAccountNumber, Entry::getAmount)); // blows up on a repeated key
> ```
>
> Fix: say what to do with the duplicate. `toMap(Entry::getAccountNumber, Entry::getAmount, BigDecimal::add)` sums. If the intent is "group", `groupingBy` is the right collector from the start.

Some `Collectors` and `Stream` APIs that will show up in session 3 and that you will miss right now: `Stream.toList()` (16) does away with `collect(Collectors.toList())` and returns an unmodifiable list; `Collectors.filtering` and `flatMapping` (9) filter inside a group; `Collectors.teeing` (12) applies two collectors to the same stream and combines the results, which would replace the report's `partitioningBy` + `reducing` with something more direct; `takeWhile` and `dropWhile` (9) cut by condition.

## Optional: the right way and the wrong way

`Optional<T>` is a container that either has a value or doesn't. It exists for one thing: **to be the return type of a method that may have no answer**. `findByCpf` may not find the member; `Optional<Member>` says so in the signature, and the compiler forces the caller to decide what to do. Compare that with the `null` from the previous session, which said nothing in the signature and charged its price in production.

Go back to session 1's `StatementService`, the one with four levels of `if (x != null)` in `statementHeader`, and apply `Optional`:

```java
// after (Java 8, Optional; the repository now returns Optional<Member>)
public String statementHeader(String cpf) {
    Optional<Member> member = members.findByCpf(cpf);
    String branchName = member
            .map(Member::getMainAccount)
            .map(Account::getBranch)
            .map(Branch::getName)
            .filter(name -> !name.isEmpty())
            .orElse("Branch not provided");
    String memberName = member.map(Member::getName).orElse("");
    return "STATEMENT - " + branchName + " - " + memberName;
}
```

`map` on an `Optional` applies the function if there is a value and returns an empty `Optional` if there isn't, or if the function returns `null`. That is why the chain of four `map` calls replaces the four `if`s: each level that does not exist makes the rest of the chain empty, and `orElse` delivers the default value. If `getMainAccount()` starts returning `Optional<Account>` (which is correct, since not every member has a main account), that `map` becomes `flatMap`.

And `entriesForPeriod`, which returned `null` for "member does not exist":

```java
public List<Entry> entriesForPeriod(String cpf, LocalDate start, LocalDate end) {
    return members.findByCpf(cpf)
            .map(member -> entries.findByAccount(member.getMainAccount().getNumber(), start, end))
            .orElse(Collections.emptyList());
}
```

Note that the return type is `List<Entry>`, not `Optional<List<Entry>>`. A collection already has its own representation of "nothing": the empty collection. An `Optional` of a collection is redundant.

| Usage | Right or wrong | Why |
|---|---|---|
| Return type of a method that may have no answer | Right | That is what it exists for; forces the caller to decide |
| `optional.map(...).filter(...).orElse(...)` | Right | Chaining without manual checks |
| `orElseThrow(() -> new MemberNotFoundException(cpf))` | Right | Fails with a domain message, at the right place |
| `orElseGet(() -> expensive())` instead of `orElse(expensive())` | Right | `orElse` always evaluates its argument, even when a value is present; `orElseGet` only when needed |
| `ifPresent(consumer)` | Right | For a side effect with no return value |
| `optional.get()` without checking | Wrong | Throws `NoSuchElementException`; it is `null` coming back under another name |
| `if (optional.isPresent()) { x = optional.get(); }` | Wrong | It is `if (x != null)` with more characters; use `map`/`orElse`/`ifPresent` |
| `Optional` as an entity field | Wrong | Not serializable, JPA does not map it, breaks records' `equals`; the field can be `null` or the getter returns `Optional` |
| `Optional` as a method parameter | Wrong | Forces the caller to wrap; use overloading or a documented parameter that accepts `null` |
| `Optional<List<T>>` or `Optional<Map<K, V>>` | Wrong | An empty collection already means "nothing" |
| `Optional.of(valueThatMayBeNull)` | Wrong | Throws `NullPointerException`; use `Optional.ofNullable` |
| `Optional` in collections (`List<Optional<T>>`) | Wrong | Filter first: `.filter(Optional::isPresent).map(Optional::get)` or, better, don't produce the empty ones |

> ✍️ **Try it now:** this method lives in Cerne's `TransferService`. Rewrite it to eliminate the `isPresent()` + `get()` and the `null` return.
>
> ```java
> public Fee feeFor(Transfer transfer) {
>     Optional<FeeRule> rule = rules.findFor(transfer.getType());
>     if (rule.isPresent()) {
>         return new Fee(rule.get().apply(transfer));
>     }
>     return null;
> }
> ```
>
> <details><summary>Show answer</summary>
>
> It depends on what "no rule" means. If it is a configuration error:
>
> ```java
> public Fee feeFor(Transfer transfer) {
>     return rules.findFor(transfer.getType())
>             .map(rule -> new Fee(rule.apply(transfer)))
>             .orElseThrow(() -> new FeeRuleNotConfiguredException(transfer.getType()));
> }
> ```
>
> If it is legitimate (an internal transfer has no fee):
>
> ```java
> public Fee feeFor(Transfer transfer) {
>     return rules.findFor(transfer.getType())
>             .map(rule -> new Fee(rule.apply(transfer)))
>             .orElse(Fee.EXEMPT);
> }
> ```
>
> In both cases the `null` is gone and the business decision became explicit. What not to do: return `Optional<Fee>` and push the decision onto the caller, if every caller is going to make the same decision.
> </details>

There is an important version detail: in Java 8, `Optional` has `map`, `flatMap`, `filter`, `orElse`, `orElseGet`, `orElseThrow(Supplier)`, `ifPresent`, `isPresent` and `get`. Java 9 brings `ifPresentOrElse`, `or` and `stream()`; 10 brings the no-argument `orElseThrow()`; 11 brings `isEmpty()`. In `v8`, `ifPresentOrElse` does not exist; if you miss it, that is a sign session 3 is going to be productive.

## When a Stream makes the code worse

Streams are not better by definition. They are better when the problem is "transform, filter and summarize a sequence". In some cases, the loop is the right answer, and the module's rubric assesses exactly that: "knows when not to use Streams".

| Situation | Why the Stream gets in the way | What to do |
|---|---|---|
| Checked exception inside the lambda | `Function` does not declare `throws`; you end up with `try/catch` inside the lambda or wrapping in `RuntimeException` | A loop, or extract a method that handles the exception and returns `Optional` |
| You need the element's index | There is no `forEachIndexed`; `IntStream.range(0, n).mapToObj(i -> ...)` works, but it is noise | A loop with an index |
| You need to stop midway with a side effect | There is no `break`; `takeWhile` only from 9 on, and even then it does not cover every case | A loop |
| Accumulating into more than one structure at the same time | Requires a custom `Collector` or two pipelines | A loop, or `teeing` from 12 on |
| Step-by-step debugging | A breakpoint inside the lambda works, but the call stack is unreadable | Extract each lambda into a named method |
| Logic with more than three levels of nested `map`/`flatMap` | Reads worse than the equivalent loop | Break it into intermediate methods |

The rule of thumb: if you needed a comment to explain the pipeline, the pipeline is wrong or too big. Extract parts into methods with domain names (`entriesForPeriod`, `totalsByType`) and the comment disappears.

> 💭 **Stop and think:** look at the last method you wrote with Streams. If the person who maintains it two years from now does not know `Collectors.reducing`, will they understand what the method calculates by reading only the names of the methods you extracted? If not, what is missing is a name, not a collector.

## Parallel streams: when they help and what they cost

`list.parallelStream()` or `.parallel()` makes the pipeline run on several threads. The promise is simple: 8 cores, 8 times faster. Reality has four conditions and two hidden costs.

The conditions for parallel to help:

1. **Large volume.** Below a few hundred thousand elements, the cost of splitting and joining outweighs the gain.
2. **Per-element work that is CPU-expensive, not I/O-bound.** Calling the repository or an external service inside a parallel stream is the worst of both worlds: it blocks threads of the common pool (see below).
3. **A source that is easy to split.** `ArrayList` and arrays split down the middle in constant time; `LinkedList`, `Stream.iterate` and `BufferedReader.lines()` don't.
4. **Stateless, unordered operations.** `sorted`, `distinct`, `limit` and `findFirst` need coordination between threads; `forEachOrdered` serializes back.

The hidden costs:

**Boxing.** A `Stream<Long>` stores each value in a `Long` object. Summing 2 million of them allocates 2 million objects and goes through the GC. `LongStream` works with primitive `long`. Compare:

```java
// with boxing: each getAmountInCents() becomes a Long; reduce unboxes and boxes on every sum
long total = entries.stream()
        .map(Entry::getAmountInCents)
        .reduce(0L, Long::sum);

// without boxing: mapToLong returns a LongStream, sum() accumulates in a long
long total = entries.stream()
        .mapToLong(Entry::getAmountInCents)
        .sum();
```

Sequentially, the difference is measurable; in parallel, it grows, because each thread allocates and the GC has to deal with all of them. The catalog's primitive interfaces (`ToLongFunction`, `LongBinaryOperator`) exist precisely for this. With `BigDecimal` there is no primitive version: the sum allocates a new object on every `add`, sequential or parallel. It is one of the arguments for storing monetary amounts as a `long` of cents when the volume is large.

**Collectors with merging.** `groupingBy` in parallel makes each thread build its own map and then merges them all. With many keys, the merge costs more than the grouping. `groupingByConcurrent` uses a single `ConcurrentHashMap` and avoids the merge, but does not guarantee the order of the elements within each group.

> ⚠️ **Migration gotcha:** parallel streams use `ForkJoinPool.commonPool()`, which is **a single one for the whole JVM** and has `cores - 1` threads. Inside an application server, this pool is shared by every request and by any other library that uses `CompletableFuture` without its own executor. A `parallelStream()` that calls the JPA repository inside `map` blocks those threads on I/O and drags down the performance of **other** features that don't even use streams. The symptom is "the system gets slow in the afternoon, when the report runs", and the cause is three packages away. Furthermore, the EJB/CDI transactional and security context lives on the request thread, not on the pool threads; an `EntityManager` accessed from inside the parallel pipeline throws an error or, worse, works by accident. Rule: no I/O, no container context inside a parallel stream.

For Cerne's report, the only stage that benefits from parallelism is the initial `groupingBy` over the 2 million entries, and even then only if the entries are already in memory (an `ArrayList`) and the server has idle cores. The per-member `map` is cheap and not worth it.

## Measuring before and after

The module's rubric asks for "measures time before and after with the same load". That demands a minimum of method, because the JVM fools anyone who measures carelessly: the JIT compiler optimizes the code during the first runs, the GC may run in the middle of the measurement and the first call pays the cost of loading classes.

The minimum rules:

1. **Same dataset**, generated once and reused for both versions.
2. **Warmup**: run each version a few times before measuring, so the JIT compiles it.
3. **Several repetitions**, and record the median, not the first one nor the average.
4. **Same machine, same moment**, with as little as possible running alongside.
5. **`System.nanoTime()`**, not `currentTimeMillis()`, which has millisecond resolution and can go backwards.

A stopwatch that is good enough for this session:

```java
package cerne.measurement;

import java.util.function.Supplier;

public final class Stopwatch {

    private Stopwatch() {}

    public static <T> T measure(String label, int warmup, int repetitions, Supplier<T> action) {
        for (int i = 0; i < warmup; i++) {
            action.get();
        }
        long[] durations = new long[repetitions];
        T result = null;
        for (int i = 0; i < repetitions; i++) {
            long start = System.nanoTime();
            result = action.get();
            durations[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(durations);
        System.out.printf("%s: median %d ms (min %d, max %d)%n",
                label, durations[repetitions / 2], durations[0], durations[repetitions - 1]);
        return result;
    }
}
```

And the measurement of both versions of the report with the same dataset:

```java
List<Member> members = DatasetGenerator.members(20_000);
List<Entry> entries = DatasetGenerator.entries(members, 2_000_000);
LocalDate start = LocalDate.of(2026, 8, 1);
LocalDate end = LocalDate.of(2026, 8, 31);

LegacyEntryReport legacy = new LegacyEntryReport();
EntryReport current = new EntryReport();

List<ReportLine> a = Stopwatch.measure("nested loops", 2, 5, () -> legacy.generate(members, entries, start, end));
List<ReportLine> b = Stopwatch.measure("streams", 2, 5, () -> current.generate(members, entries, start, end));

if (!a.equals(b)) {
    throw new AssertionError("The two versions disagree; the measurement is worthless");
}
```

The last check is mandatory: **measuring a wrong version is pointless**. If `ReportLine` has a well-defined `equals` (and in session 4 it becomes a record, which has one), the comparison is a single line.

Illustrative results, obtained on an 8-core development machine; your numbers will vary, and what matters is the proportion:

| Version | 20 thousand members, 200 thousand entries | 20 thousand members, 2 million entries |
|---|---|---|
| Nested loops (quadratic) | about 70,000 ms | did not finish in 10 minutes |
| Sequential Streams, `groupingBy` | 95 ms | 780 ms |
| Streams with `parallel()` only on the `groupingBy` | 70 ms | 420 ms |
| Streams with `parallelStream()` everywhere | 110 ms | 610 ms |

Three readings of the table. First: the gain of two to three orders of magnitude did not come from Streams, it came from replacing a quadratic algorithm with an index; Streams made the index the obvious solution. Second: parallelism helped where there was volume (the `groupingBy`) and hurt where there wasn't (the per-member `map`, where merging the results cost more than the work itself). Third: with 200 thousand entries the difference between sequential and parallel is 25 ms, which is irrelevant in a monthly report and does not justify the risk of the common pool on the application server.

For measurements that go into a decision report (session 6), the homemade stopwatch is not enough: use JMH (*Java Microbenchmark Harness*), the OpenJDK benchmarking project, which handles warmup, dead-code elimination and variance correctly. Here, the stopwatch serves this session's goal: proving that the rewrite did not get slower.

> 🎬 **Picture this:** the customer service manager asks "is the new report faster?". Without measurements, the answer is "it should be". With the table above, the answer is "the monthly report went from ten minutes to less than a second, with the same data, and the result is identical line by line". The second answer is the one that unlocks the budget for the rest of the migration.

## Worked exercise: monthly statement with running totals

**Problem.** An account statement needs to show, for each month in the period, the total credits, the total debits and the month's largest entry with its description. The months must come in chronological order. If a month has no entries, it does not appear.

**Step 1: define the result.** One line per month. In Java 8, a simple immutable class (in session 4 it becomes a record):

```java
public final class MonthlyStatementLine {
    private final YearMonth month;
    private final BigDecimal credits;
    private final BigDecimal debits;
    private final Entry largestEntry; // never null: the line only exists if the month has entries
    // constructor, getters, equals, hashCode
}
```

The field is `Entry`, not `Optional<Entry>`: `Optional` as a field goes against the right-and-wrong table. If some day it becomes possible to have no largest entry, the getter starts returning `Optional`; the field does not.

**Step 2: group by month, in order.** `groupingBy` with `TreeMap` as the map factory guarantees chronological order, because `YearMonth` is `Comparable`:

```java
Map<YearMonth, List<Entry>> byMonth = entries.stream()
        .collect(Collectors.groupingBy(e -> YearMonth.from(e.getDate()), TreeMap::new, Collectors.toList()));
```

**Step 3: summarize each month.** For each group, two totals and a maximum. The maximum is an `Optional`, and here it is in the right place, as the return value of `max`:

```java
private MonthlyStatementLine summarize(YearMonth month, List<Entry> monthEntries) {
    BigDecimal credits = monthEntries.stream()
            .filter(Entry::isCredit)
            .map(Entry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal debits = monthEntries.stream()
            .filter(e -> !e.isCredit())
            .map(Entry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Entry largest = monthEntries.stream()
            .max(Comparator.comparing(Entry::getAmount))
            .orElseThrow(() -> new IllegalStateException("A month without entries should not have a group: " + month));
    return new MonthlyStatementLine(month, credits, debits, largest);
}
```

The `orElseThrow` is right here: a `groupingBy` group is never empty, so an empty `Optional` is a bug, and the program should fail with a message that says so. If the list were traversed three times in a month with millions of entries, it would be worth merging the three calculations; in an account statement, with dozens of entries per month, clarity is worth more.

**Step 4: build the statement.**

```java
public List<MonthlyStatementLine> monthlyStatement(List<Entry> entries) {
    return entries.stream()
            .collect(Collectors.groupingBy(e -> YearMonth.from(e.getDate()), TreeMap::new, Collectors.toList()))
            .entrySet().stream()
            .map(mapEntry -> summarize(mapEntry.getKey(), mapEntry.getValue()))
            .collect(Collectors.toList());
}
```

`TreeMap.entrySet()` iterates in key order, and `.stream()` over it preserves that order; that is why the lines come out chronological without `sorted`.

**Step 5: test it.** With JUnit 4, three entries in two months:

```java
@Test
public void groupsByMonthInOrderWithTotalsAndLargest() {
    List<Entry> entries = Arrays.asList(
            credit("0001", LocalDate.of(2026, 9, 3), "150.00", "Deposit"),
            debit("0001", LocalDate.of(2026, 8, 20), "40.00", "Fee"),
            credit("0001", LocalDate.of(2026, 8, 5), "900.00", "Salary"));

    List<MonthlyStatementLine> statement = new StatementGenerator().monthlyStatement(entries);

    assertEquals(2, statement.size());
    assertEquals(YearMonth.of(2026, 8), statement.get(0).getMonth());
    assertEquals(new BigDecimal("900.00"), statement.get(0).getCredits());
    assertEquals(new BigDecimal("40.00"), statement.get(0).getDebits());
    assertEquals("Salary", statement.get(0).getLargestEntry().getDescription());
    assertEquals(YearMonth.of(2026, 9), statement.get(1).getMonth());
}
```

The test is possible because the method takes the list and returns the list, without touching a repository, a clock or a container. That is the trait of functional code that pays off most in the migration: **tests that do not depend on the application server survive the switch from `javax` to `jakarta` and from JUnit 4 to 5 without changing a single line of logic**.

## Hands-on challenge

**Assignment.** The `TransferReconciliation` class, in the `cerne.transfers` package, compares the transfers recorded in Cerne with the daily file received from the central credit union and produces three lists: transfers that match, transfers that exist only in Cerne and transfers that exist only in the file. Today it has 120 lines with nested loops, `null` for "not found" and a `Map` filled in by hand. Refactor it to Streams and `Optional`, keeping the existing test suite (JUnit 4, 14 tests) green, and record the time before and after with the dataset from `DatasetGenerator.reconciliation(50_000)`.

**Acceptance criteria.**

- The 14 existing tests keep passing with no changes to the asserts; new tests may be added.
- No method in the class returns `null`; lookups that may find nothing return `Optional`; empty lists represent "no results".
- Matching a Cerne transfer with a file transfer uses an index (`toMap` or `groupingBy`), not a linear search inside a loop; justify the choice of collector in a one-line comment.
- No `Optional.get()` without a check and no `isPresent()` followed by `get()`.
- The time of both versions, with warmup and the median of at least five runs, is recorded in `MEASUREMENTS.md` at the project root, along with confirmation that the results are identical.

*Walkthrough of the solution at the beginning of the next session.*

## Conclusion

Lambdas and method references are the way to pass behavior; functional interfaces are the type of that behavior, and naming them (`FeeRule`) is part of the domain. Streams are lazy pipelines, traversed vertically, where the order of operations matters and the `Collector` is the piece that turns the sequence into a result: `groupingBy`, `partitioningBy` and `toMap` cover almost everything, each with its own gotcha. `Optional` is a return type, chained with `map`, `filter` and `orElse`, and it is not a field, a parameter or a substitute for `if != null` with `isPresent`. Parallel streams help with volume, CPU-bound work and a splittable source, and they charge for it in boxing, merging and the common pool shared with the application server. And every rewrite is proven with measurement: same dataset, warmup, median, identical result. In the next session, the real migration begins: the jump from 8 to 11, with `var`, the HTTP Client, the new `String` and `Collectors` methods, the module system and the first real breakage, the removal of JAXB from the JDK.

## References

- Urma, R.-G.; Fusco, M.; Mycroft, A. (2019). *Modern Java in Action*. 2nd edition. Manning. Chapters 3 (lambdas), 4 to 7 (Streams, collectors, parallelism) and 11 (`Optional`).
- Bloch, J. (2018). *Effective Java*. 3rd edition. Addison-Wesley. Items 42 to 48 (lambdas and Streams; "use caution when making streams parallel") and 55 (`Optional`).
- Oaks, S. (2020). *Java Performance: In-Depth Advice for Tuning and Programming Java 8, 11, and Beyond*. 2nd edition. O'Reilly. Chapters 2 (measurement methodology) and 12 (Streams, boxing and parallelism).
- Horstmann, C. S. (2022). *Core Java Volume II: Advanced Features*. 12th edition. Oracle Press. Chapter 1 (Streams).
- Oracle. *Package java.util.stream*, Java SE 21 API Specification. https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html
- Oracle. *Class Collectors*, Java SE 21 API Specification. https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html
- Oracle. *Class Optional*, Java SE 21 API Specification. https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html
- Oracle. *Package java.util.function*, Java SE 21 API Specification. https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html
- OpenJDK. *JMH: Java Microbenchmark Harness*. https://github.com/openjdk/jmh
