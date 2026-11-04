# Cerne guide project · UP-JV-006

Cerne is the fictional core system of a credit union: members, accounts, entries, transfers, statements, reconciliation and audit. The same system is migrated from Java 8 to 21 over the course of the module. Each milestone is an independent Maven project, so you can open the one for any session without depending on git.

```
5-guide-project/
├── v8/     Java 8,  Java EE 7 (javax.*),      JUnit 4, Lombok 1.16  ← starting point (sessions 1 and 2)
├── v11/    Java 11, Java EE 8 (javax.*),      JUnit 4, Lombok 1.18  ← result of session 3
├── v17/    Java 17, Jakarta EE 10 (jakarta.*), JUnit 5, records       ← result of sessions 4, 6 and 7
├── v21/    Java 21, Jakarta EE 10,            JUnit 5, no XStream   ← result of sessions 5 and 8
└── solutions/challenge-08/   rules for the session 8 challenge (to paste over a copy of v21)
```

There is no application server. The Java EE/Jakarta EE APIs come in with `provided` scope, only to compile the annotations. The tests use in-memory repositories and the `FakeCpfLookup` simulator, and no test accesses the network.

## Build and test

The JDKs come from sdkman. Run from this folder:

```sh
JAVA_HOME=~/.sdkman/candidates/java/8.0.432-amzn mvn -q -f v8/pom.xml  package
JAVA_HOME=~/.sdkman/candidates/java/11-open      mvn -q -f v11/pom.xml package
JAVA_HOME=~/.sdkman/candidates/java/17-open      mvn -q -f v17/pom.xml package
JAVA_HOME=~/.sdkman/candidates/java/21-open      mvn -q -f v21/pom.xml package
```

Each `package` runs the suite and produces `vN/target/cerne-vN.jar`, plus `vN/target/lib/` with the runtime dependencies (for `jdeps` and for `java -cp vN/target/classes:vN/target/lib/*`). The commands for each session are in each milestone's `README.md`.

## Session → milestone → classes touched

| Milestone | Session | What changes | Classes touched |
|---|---|---|---|
| v8 | 1, 2 | Starting state with the 7 `// SMELL N:` smells. The session 1 challenge fixes `Entry` while still on 8; session 2 rewrites the report, the statement and the reconciliation with Streams/`Optional`. These fixes show up applied in `v11`, not in `v8` | `Entry`, `EntryReport`, `TransferReconciliation`, `StatementService` |
| v11 | 3 | JDK 11, `var`, `List.of`, HTTP Client with timeouts, external JAXB 2.3.3 (`javax`), Lombok 1.18.30, no `sun.*`, XStream's `--add-opens` in surefire | `CpfLookupClient`, `ReconciliationFileGenerator`, `CerneUtil`, `pom.xml` (+ `LegacyEntryReport`, `ReportBenchmark`) |
| v17 | 4 | Jakarta EE 10, JUnit 5, records, `sealed Entry` and `sealed Transfer`, `instanceof` pattern matching, text blocks, switch expression | `EntryDTO`, `Transfer*`, `FeeCalculator`, `Account`, `Member`, `Cpf` |
| v17 | 6 | No code changes: `EntryProcessing` is the same in all four milestones | `cerne.measurement.*` |
| v17 | 7 | Factory, Decorator (audit, deduplication, CPF cache) and Visitor; `cerne.util` eliminated | `EntryFactory`, `TransferService*`, `CachingCpfLookup`, `AuditDecorator`, `CsvLineGenerator` |
| v21 | 5 | Switch with patterns and record patterns, sequenced collections, virtual threads | `FeeCalculator`, `CsvLineGenerator`/`TextLineGenerator`, `StatementService`, `BatchLookupService` |
| v21 | 8 | Strategy + Chain (rules as records, version C) and XStream replaced by Jackson | `cerne.transfers.validation.*`, `AuditService`, `DefaultTransferService` |
| v21 | challenge 7 | Daily limit Decorator (manual one active, `@Decorator` disabled) and decorator order | `DailyLimitTransferService`, `DailyLimitDecorator`, `TransferServiceComposition` |

**What is deliberately left out:**
- `RegistrationService` with sequential `if`s in `v17` and `v21`: it is the starting point of the session 9 assessment.
- The 11 `instanceof` checks in `cerne.reconciliation` in `v21`: they are the session 5 challenge.

## Course material names × repository

When the course material uses a different name, the repository wins:

| In the course material | In the repository |
|---|---|
| `RegistrationStatus` | `CpfStatus` |
| `XmlStatementExporter` | `ReconciliationFileGenerator` |
| `CpfLookupClient` with `synchronized` (course-material version; in the repository `CpfLookupClient` is only the HTTP client) | `PerCpfLockingCpfLookup` |
| `ProcessingMeter` with a CSV file | `EntryProcessing`, which generates the dataset in memory |
| `cerne.members.Account` | `cerne.accounts.Account` |
| `Entry` in the reconciliation examples of course material 05 | `InternalEntry` |
| `FileSigner` with `sun.misc.BASE64Encoder` (course material 03) | `CerneUtil` and `CpfLookupClient` in `v8` |
| `target/cerne.jar` | `target/cerne-v8.jar`, `cerne-v11.jar`, `cerne-v17.jar`, `cerne-v21.jar` |
| `cerne.entries.ProcessingMeter` | `cerne.measurement.ProcessingMeter` |

Not present in the project: `EntryResource`, `NewEntry`, `BalanceCalculator`, `IntegrationFactory`, `FeeRule` and `ValidationRule` (version B of session 8).
