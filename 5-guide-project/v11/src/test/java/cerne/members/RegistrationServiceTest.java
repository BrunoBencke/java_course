package cerne.members;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class RegistrationServiceTest {

    private InMemoryMemberRepository repository;
    private RegistrationService service;

    @Before
    public void setUp() {
        repository = new InMemoryMemberRepository();
        service = new RegistrationService(repository);
    }

    private void assertRejects(String cpf, String name, MemberType type, String message) {
        try {
            service.register(cpf, name, type);
            fail("should reject: " + message);
        } catch (InvalidRegistrationException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void registersValidMember() {
        service.register("52998224725", "  Ana Souza ", MemberType.INDIVIDUAL);
        assertEquals("Ana Souza", repository.findByCpf("52998224725").orElseThrow().getName());
    }

    @Test
    public void rejectsInvalidCpf() {
        assertRejects("52998224700", "Ana", MemberType.INDIVIDUAL, "Invalid CPF: 52998224700");
    }

    @Test
    public void rejectsDuplicateCpf() {
        service.register("52998224725", "Ana", MemberType.INDIVIDUAL);
        assertRejects("52998224725", "Another Ana", MemberType.INDIVIDUAL, "CPF already registered: 52998224725");
    }

    @Test
    public void rejectsShortName() {
        assertRejects("52998224725", "Al", MemberType.INDIVIDUAL, "Name too short");
    }
}
