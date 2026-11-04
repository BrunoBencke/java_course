# Cerne · milestone v21 (result of sessions 5 and 8)

Java 21 · Jakarta EE 10 · JUnit 5.10 · JAXB 4 · Jackson 2.17 · no XStream and no `--add-opens`.

## Build and test

```sh
export JAVA_HOME=~/.sdkman/candidates/java/21-open
mvn -q -f v21/pom.xml test
mvn -q -f v21/pom.xml package      # v21/target/cerne-v21.jar + v21/target/lib/
$JAVA_HOME/bin/jdeps --jdk-internals v21/target/cerne-v21.jar   # no output
grep -rn "instanceof" v21/src/main/java/cerne/transfers v21/src/main/java/cerne/statements   # zero lines
grep -rn "instanceof" v21/src/main/java/cerne/reconciliation | wc -l                         # 11: session 5 challenge
```

## What this milestone shows

**Session 5:**
- `FeeCalculator` uses a switch with patterns over the `sealed Transfer`, with no `default` and no `throw`. The case of a rural producer sending to a cooperative bank is a nested record pattern (`ExternalAccount(var ispb, ...)`) with a guard, before the general case.
- The `TransferType` discriminator is gone.
- `CsvLineGenerator` and `TextLineGenerator` became exhaustive `switch`es. The Visitor and `accept()` left the domain.
- `StatementService.buildStatement` uses `getFirst()`, `getLast()` and `reversed()`. `reversed()` is a view; the statement keeps a copy, and there is a test that shows it.
- `BatchLookupService` for the virtual threads experiment.

**Session 8 (version C of the course material):**
- Package `cerne.transfers.validation`: `Violation`, `ValidationContext(request, source, now)`, `ValidationResult` sealed {`Approved`, `Rejected`}.
- The `sealed interface Rule` has the records `PositiveAmount`, `ActiveAccount`, `ActiveDestination`, `SufficientBalance` and `DailyLimit`. `RuleEvaluator` is an exhaustive switch; `TransferValidator` is the list of rules; `ValidatorProducer` is the CDI producer.
- A new rule goes into the `permits` and gets a `case`. The two challenge rules are in `../solutions/challenge-08/`.
- XStream replaced by Jackson in `AuditService`, and `AuditEvent` became a record. The `argLine` left `pom.xml`.

**Solution to the session 7 challenge:**
- `DailyLimitTransferService`, the manual and active version. `DailyLimitDecorator` is the `@Decorator` version, switched off because it has no `@Priority`.
- `TransferServiceComposition` fixes the order audit → deduplication → limit → default, with the justification in the header.

**Left as it was, on purpose:**
- `RegistrationService` with `if`s (session 9 assessment).
- The 11 `instanceof` in `cerne.reconciliation` (session 5 challenge).

## Virtual threads experiment (session 5)

Usage: `BatchLookupService [platform|virtual|pinning|reentrant] [lookups=5000]`. The simulator `FakeCpfLookup(200)` waits 200 ms with `Thread.sleep` and never touches the network.

| Variant | Executor | Lookup | Expected time |
|---|---|---|---|
| `platform` | `newFixedThreadPool(50)` | simulator | lookups ÷ 50 × 200 ms |
| `virtual` | `newVirtualThreadPerTaskExecutor()` | simulator | ~200 ms + overhead |
| `pinning` | virtual threads | `PerCpfLockingCpfLookup` (`synchronized`) | lookups ÷ cores × 200 ms |
| `reentrant` | virtual threads | `ReentrantLockCpfLookup` (`ReentrantLock`) | ~200 ms: this is the fix |

```sh
J=~/.sdkman/candidates/java/21-open/bin/java
$J -cp v21/target/classes cerne.externallookup.BatchLookupService platform 5000
$J -cp v21/target/classes cerne.externallookup.BatchLookupService virtual 5000
$J -Djdk.tracePinnedThreads=full -cp v21/target/classes cerne.externallookup.BatchLookupService pinning 1000
$J -Djdk.tracePinnedThreads=full -cp v21/target/classes cerne.externallookup.BatchLookupService reentrant 1000
```

On the reference machine, with 10 cores, the times were 20.4 s, 0.3 s, 20.5 s and 0.2 s. In `pinning`, `tracePinnedThreads` points to `PerCpfLockingCpfLookup.lookUp ... <== monitors:1`.

## Measurements (session 6)

```sh
J=~/.sdkman/candidates/java/21-open/bin/java
$J -Xms1g -Xmx1g "-Xlog:gc*:file=gc-v21.log:uptime,level,tags" \
  -cp v21/target/classes cerne.measurement.EntryProcessing 3 5
# JFR recording cerne-v21.jfr
$J -Xms1g -Xmx1g -XX:StartFlightRecording=filename=cerne-v21.jfr,settings=profile \
  -cp v21/target/classes cerne.measurement.EntryProcessing 3 5
~/.sdkman/candidates/java/21-open/bin/jfr summary cerne-v21.jfr
```

In zsh, `-Xlog:gc*` needs quotes; without them, the shell answers `no matches found`. The third argument of `EntryProcessing` reduces the load (default `1000000`); use the same value in all four milestones.

## Difference from v17

- Pattern matching for `switch` replaced the enum discriminator, the `instanceof` ladder and the Visitor in transfers and statements.
- Validation went from chained `if`s to a chain of rules-as-data with exhaustive dispatch, and a rejected transfer returns all violations at once (`TransferRejectedException`).
- The audit trail became JSON with Jackson and no longer needs `--add-opens`.
- Virtual threads arrived in the batch lookup and sequenced collections in the statement.
