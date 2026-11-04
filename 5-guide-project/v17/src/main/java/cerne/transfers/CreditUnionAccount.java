package cerne.transfers;

/** Account at another credit union in the system. */
public record CreditUnionAccount(String creditUnionCode, String number) {

    public CreditUnionAccount {
        creditUnionCode = TransferValues.notBlank(creditUnionCode, "credit union code");
        number = TransferValues.notBlank(number, "account number");
    }
}
