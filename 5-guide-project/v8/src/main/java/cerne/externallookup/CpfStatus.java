package cerne.externallookup;

/** Registration status returned by the external service. */
public enum CpfStatus {
    REGULAR,
    SUSPENDED,
    CANCELED,
    NULLIFIED;

    /** Reads {"status":"REGULAR"}; minimal parser, no JSON library. */
    public static CpfStatus fromJson(String json) {
        int key = json.indexOf("\"status\"");
        if (key < 0) {
            throw new IllegalArgumentException("Response without status: " + json);
        }
        int start = json.indexOf('"', json.indexOf(':', key) + 1) + 1;
        int end = json.indexOf('"', start);
        return valueOf(json.substring(start, end).trim().toUpperCase());
    }
}
