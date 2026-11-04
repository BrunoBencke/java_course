package cerne.members;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationServiceTest {

    private InMemoryMemberRepository repository;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMemberRepository();
        service = new RegistrationService(repository);
    }

    private void assertRejects(String cpf, String name, MemberType type, String message) {
        var error = assertThrows(InvalidRegistrationException.class, () -> service.register(cpf, name, type));
        assertEquals(message, error.getMessage());
    }

    @Test
    @DisplayName("registers a valid member with the name trimmed at both ends")
    void registersValidMember() {
        service.register("52998224725", "  Ana Souza ", MemberType.INDIVIDUAL);
        assertEquals("Ana Souza", repository.findByCpf("52998224725").orElseThrow().getName());
    }

    @Test
    void rejectsInvalidCpf() {
        assertRejects("52998224700", "Ana", MemberType.INDIVIDUAL, "Invalid CPF: 52998224700");
    }

    @Test
    void rejectsDuplicateCpf() {
        service.register("52998224725", "Ana", MemberType.INDIVIDUAL);
        assertRejects("52998224725", "Another Ana", MemberType.INDIVIDUAL, "CPF already registered: 52998224725");
    }

    @Test
    void rejectsShortName() {
        assertRejects("52998224725", "Al", MemberType.INDIVIDUAL, "Name too short");
    }

    @Test
    void rejectsMissingType() {
        assertRejects("52998224725", "Ana", null, "Member type is required");
    }
}
