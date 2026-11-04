package cerne.members;

/** CPF as a value: digits only, always 11. The check-digit validation replaces CerneUtil.isValidCpf. */
public record Cpf(String number) {

    public Cpf {
        if (number == null) {
            throw new IllegalArgumentException("CPF is required");
        }
        number = number.replaceAll("\\D", "");
        if (number.length() != 11) {
            throw new IllegalArgumentException("CPF must have 11 digits: " + number);
        }
    }

    /** Check digits match and the digits are not all the same. */
    public boolean valid() {
        if (number.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(9) == number.charAt(9) - '0' && checkDigit(10) == number.charAt(10) - '0';
    }

    public static boolean isValid(String text) {
        try {
            return new Cpf(text).valid();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String formatted() {
        return number.substring(0, 3) + "." + number.substring(3, 6) + "." + number.substring(6, 9) + "-"
                + number.substring(9);
    }

    private int checkDigit(int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += (number.charAt(i) - '0') * (position + 1 - i);
        }
        int digit = (sum * 10) % 11;
        return digit == 10 ? 0 : digit;
    }
}
