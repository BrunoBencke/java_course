package cerne.externallookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cerne.members.Cpf;

/** No network: tests only what does not depend on the external service. */
class CpfLookupClientTest {

    @Test
    @DisplayName("Basic header with java.util.Base64")
    void buildsBasicHeader() {
        assertEquals("Basic dXNlcjpwYXNzd29yZA==", CpfLookupClient.authenticationHeader("user", "password"));
        assertEquals("user", CpfLookupClient.usernameFromHeader("Basic dXNlcjpwYXNzd29yZA=="));
    }

    @Test
    @DisplayName("stub returns REGULAR by default")
    void stubReturnsRegular() {
        CpfLookup lookup = new FakeCpfLookup().with(new Cpf("11144477735"), CpfStatus.SUSPENDED);
        assertEquals(CpfStatus.REGULAR, lookup.lookUp(new Cpf("52998224725")));
        assertEquals(CpfStatus.SUSPENDED, lookup.lookUp(new Cpf("111.444.777-35")));
    }
}
