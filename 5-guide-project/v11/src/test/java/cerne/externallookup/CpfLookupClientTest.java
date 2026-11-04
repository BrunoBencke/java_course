package cerne.externallookup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** No network: tests only what does not depend on the external service. */
public class CpfLookupClientTest {

    @Test
    public void buildsBasicHeader() throws Exception {
        assertEquals("Basic dXNlcjpwYXNzd29yZA==", CpfLookupClient.authenticationHeader("user", "password"));
    }

    @Test
    public void recoversUsernameFromHeader() throws Exception {
        assertEquals("user", CpfLookupClient.usernameFromHeader("Basic dXNlcjpwYXNzd29yZA=="));
    }

    @Test
    public void stubReturnsRegularByDefault() {
        CpfLookup lookup = new FakeCpfLookup().with("11144477735", CpfStatus.SUSPENDED);
        assertEquals(CpfStatus.REGULAR, lookup.lookUp("52998224725"));
        assertEquals(CpfStatus.SUSPENDED, lookup.lookUp("11144477735"));
    }

    @Test
    public void readsStatusFromJson() {
        assertEquals(CpfStatus.CANCELED, CpfStatus.fromJson("{\"cpf\":\"1\", \"status\": \"CANCELED\"}"));
    }
}
