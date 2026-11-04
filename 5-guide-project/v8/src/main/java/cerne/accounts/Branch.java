package cerne.accounts;

public class Branch {

    private String code;
    private String name;

    public Branch() {
    }

    public Branch(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
