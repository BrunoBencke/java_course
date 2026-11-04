package cerne.externallookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import cerne.members.Cpf;

/** Correctness, not timing: the timings are measured with main, outside the suite. */
class BatchLookupServiceTest {

    private static final List<Cpf> CPFS = IntStream.rangeClosed(1, 200)
            .mapToObj(i -> new Cpf("%011d".formatted(i))).toList();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"platform", "virtual", "pinning", "reentrant"})
    @DisplayName("the four variants return the same answer, in CPF order")
    void sameAnswers(String variant) {
        var simulator = new FakeCpfLookup(2).with(new Cpf("00000000007"), CpfStatus.SUSPENDED);
        var service = switch (variant) {
            case "platform" -> BatchLookupService.withPlatformPool(simulator, 50);
            case "virtual" -> BatchLookupService.withVirtualThreads(simulator);
            case "pinning" -> BatchLookupService.withVirtualThreads(new PerCpfLockingCpfLookup(simulator));
            default -> BatchLookupService.withVirtualThreads(new ReentrantLockCpfLookup(simulator));
        };

        var statuses = service.lookUp(CPFS);

        assertEquals(200, statuses.size());
        assertEquals(CpfStatus.SUSPENDED, statuses.get(6));
        assertEquals(199, statuses.stream().filter(s -> s == CpfStatus.REGULAR).count());
    }
}
