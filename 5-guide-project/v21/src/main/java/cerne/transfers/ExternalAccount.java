package cerne.transfers;

/** Account at another bank; the bank is identified by its ISPB (8 digits). */
public record ExternalAccount(String ispb, String branch, String number) {

    public ExternalAccount {
        ispb = TransferValues.notBlank(ispb, "ISPB");
        if (!ispb.matches("\\d{8}")) {
            throw new IllegalArgumentException("Destination bank ISPB must have 8 digits: " + ispb);
        }
        branch = TransferValues.notBlank(branch, "branch");
        number = TransferValues.notBlank(number, "account number");
    }
}
