# Solution · session 8 challenge

The `AllowedHours` and `LimitPerMemberType` rules in the `v21` validation chain (version C: rules as records, dispatch in `RuleEvaluator`). It is not part of `v21` because it is the answer to the challenge.

## What is here

The paths mirror `v21`:

| File | Status |
|---|---|
| `validation/Rule.java` | replaces the one in `v21`: two new records and the extended `permits` |
| `validation/RuleEvaluator.java` | replaces the one in `v21`: two new `case`s, no `default` |
| `validation/ValidatorProducer.java` | replaces the one in `v21`: chain with the new rules. The order is justified in the comment |
| `validation/TimeWindow.java` | new. Internal 24h; between credit unions 6h–22h; to another bank 6h30–17h on business days |
| `validation/LimitPolicy.java` + `DefaultLimitPolicy.java` | new. Individual/rural producer R$ 5.000; legal entity/association R$ 50.000; special agreement with no limit |
| tests `AllowedHoursTest`, `LimitPerMemberTypeTest`, `ValidatorWithChallengeRulesTest` | new. Each rule has approval, rejection and edge; the validator returns the three violations in chain order |

`AllowedHours` reads `context.now()`, which the service computes with the injected `Clock`. The tests use `Clock.fixed`.

## How to apply and run

Run from `5-guide-project/`, on a **copy** of `v21`:

```sh
cp -R v21 /tmp/v21-challenge08
cp -R solutions/challenge-08/src/. /tmp/v21-challenge08/src/
JAVA_HOME=~/.sdkman/candidates/java/21-open mvn -q -f /tmp/v21-challenge08/pom.xml test
```

The `v21` suite stays green alongside the new tests: `defaultRules(accounts, transfers)` was kept and the full chain is the overload that takes the `LimitPolicy`.
