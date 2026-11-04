package cerne.externallookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/** Client for the external CPF registration-status service. It is never called in the tests. */
public class CpfLookupClient implements CpfLookup {

    private static final String BASE_URL = "https://cpf-lookup.example.coop/v1/cpf/";

    private final String username;
    private final String password;

    public CpfLookupClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // SMELL 7: HttpURLConnection with no connect or read timeout; the server thread gets stuck
    @Override
    public CpfStatus lookUp(String cpf) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + cpf).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", authenticationHeader(username, password));
            try {
                int status = connection.getResponseCode();
                if (status != 200) {
                    throw new IOException("CPF lookup failed with status " + status);
                }
                StringBuilder body = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                } finally {
                    reader.close();
                }
                return CpfStatus.fromJson(body.toString());
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException("CPF lookup failed for " + cpf, e);
        }
    }

    /** HTTP Basic header using a JDK internal API. */
    static String authenticationHeader(String username, String password) throws UnsupportedEncodingException {
        String credentials = username + ":" + password;
        return "Basic " + new BASE64Encoder().encode(credentials.getBytes("UTF-8"));
    }

    /** Used in the diagnostic log: recovers the username from the header. */
    static String usernameFromHeader(String header) throws IOException {
        String credentials = new String(new BASE64Decoder().decodeBuffer(header.substring("Basic ".length())), "UTF-8");
        return credentials.substring(0, credentials.indexOf(':'));
    }
}
