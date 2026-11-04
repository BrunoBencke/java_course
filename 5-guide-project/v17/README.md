# Cerne · milestone v17 (result of sessions 4, 6 and 7)

Java 17 · Jakarta EE 10 (`jakarta.*`) · JUnit 5.10 · JAXB 4 · XStream 1.4.9 still present (with `--add-opens`) · no Lombok.
It is the starting point for sessions 5 and 8 and for the session 9 assessment.

## Build and test

```sh
export JAVA_HOME=~/.sdkman/candidates/java/17-open
mvn -q -f v17/pom.xml test
mvn -q -f v17/pom.xml package      # v17/target/cerne-v17.jar + v17/target/lib/
mvn -q -f v17/pom.xml test -Dtest=AuditServiceTest -DargLine=""   # shows the InaccessibleObjectException
```

## What this milestone demonstrates

**Session 4, platform:**
- `jakarta.jakartaee-api:10.0.0` and `jakarta.*` in EJB, CDI, JPA, JAX-RS and JTA.
- JAXB 4.0 (`jakarta.xml.bind`), with the reference file from `v11` unchanged and green.
- JUnit 5 without Vintage, with `@DisplayName`, `@ParameterizedTest` and `@CsvSource`.
- `argLine` required for XStream: four `--add-opens`.

**Session 4, domain:**
- `EntryDTO`, `EntrySummary`, `ReportLine`, `Branch`, `Cpf` and the reconciliation records became records, with a compact constructor where there is a rule.
- `Entry` is a `sealed interface` with the records `Credit`, `Debit`, `Fee` and `Reversal`.
- `Transfer` is a `sealed interface` with the records `InternalTransfer`, `InterCreditUnionTransfer` and `OtherBankTransfer`. The destinations are `Account`, `CreditUnionAccount` and `ExternalAccount`, and the positive amount and filled-in destinations are validated in the compact constructor (session 4 challenge).
- `FeeCalculator` has both forms from the challenge:
  - `calculate`: switch expression over the `TransferType` discriminator, without `default`;
  - `calculateFee`: `instanceof` ladder with pattern matching, with the final `throw` that only 21 removes.
- `Account` and `Member` remain classes, because they are entities. `Account` uses record-style accessors: `number()`, `balance()`, `active()`, `dailyLimit()`, `overdraftLimit()`, `holderType()`.
- `Member.hasSpecialAgreement()`. The dataset has one member with an agreement in every fifty.

**Session 7:**
- `EntryFactory.from(FileLine)`, with `default` because the selector is a `String`.
- `TransferService` (single-method interface) + `DefaultTransferService`.
- Decorators `AuditedTransferService` and `DeduplicatingTransferService`.
- `AuditDecorator`, the CDI `@Decorator` variant, which only compiles.
- `CachingCpfLookup` with an injected `Clock`. `AdjustableClock` stays in test scope.
- The Visitor (`EntryVisitor` + `accept`) stays in `cerne.statements` for the side-by-side with `v21`.

**Session 6:** `EntryProcessing` is the same in all four milestones.

**Still here, on purpose:**
- `TransferValidator` with chained `if`s (session 8). Overdraft only for legal entities.
- `RegistrationService.register` with sequential `if`s (session 9 assessment).
- XStream in `AuditService`.
- The `cerne.reconciliation` module (`EntryReconciler`, `ReconciliationReportGenerator`, `DiscrepancyClassifier`, `ReconciliationExporter`) with 11 `instanceof` in a ladder.

## Measurements

```sh
$JAVA_HOME/bin/java -Xms1g -Xmx1g "-Xlog:gc*:file=gc-v17.log:uptime,level,tags" \
  -cp v17/target/classes cerne.measurement.EntryProcessing 3 5
mvn -q -f v17/pom.xml compile exec:java -Dexec.mainClass=cerne.measurement.ReportBenchmark
```

## Difference from v11

The platform went up first (`jakarta`, JUnit 5, `--add-opens`) and the domain came afterwards. `cerne.util` was eliminated:
- `CerneUtil` was taken apart: the CPF went to `Cpf`, the dates to `DateTimeFormatter`, the Base64 signature to the reconciliation generator and the identifier to the service.
- The three classes "named after a pattern" gave way to real patterns.

Lombok left `pom.xml`. `Transfer` is no longer a class with optional fields per type. The transfer service became an interface, to allow the decorators. The domain dates are all `java.time`.
