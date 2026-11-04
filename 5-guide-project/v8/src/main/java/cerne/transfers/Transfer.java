package cerne.transfers;

import java.math.BigDecimal;

import cerne.accounts.Account;

/** One class for three types: each field only makes sense for one of them. */
public class Transfer {

    public static final String INTERNAL = "INTERNAL";
    public static final String BETWEEN_CREDIT_UNIONS = "BETWEEN_CREDIT_UNIONS";
    public static final String OTHER_BANK = "OTHER_BANK";

    private String type;
    private Account source;
    private BigDecimal amount;
    private Account destination;                      // INTERNAL only
    private String destinationCreditUnionCode;        // BETWEEN_CREDIT_UNIONS only
    private String bankCode;                          // OTHER_BANK only (ISPB)
    private String destinationBranch;                 // OTHER_BANK only
    private String destinationAccountNumber;          // BETWEEN_CREDIT_UNIONS and OTHER_BANK

    public Transfer() {
    }

    public static Transfer internal(Account source, Account destination, BigDecimal amount) {
        Transfer t = new Transfer();
        t.type = INTERNAL;
        t.source = source;
        t.destination = destination;
        t.amount = amount;
        return t;
    }

    public static Transfer betweenCreditUnions(Account source, String creditUnionCode,
                                               String accountNumber, BigDecimal amount) {
        Transfer t = new Transfer();
        t.type = BETWEEN_CREDIT_UNIONS;
        t.source = source;
        t.destinationCreditUnionCode = creditUnionCode;
        t.destinationAccountNumber = accountNumber;
        t.amount = amount;
        return t;
    }

    public static Transfer toOtherBank(Account source, String bankCode, String branch,
                                       String accountNumber, BigDecimal amount) {
        Transfer t = new Transfer();
        t.type = OTHER_BANK;
        t.source = source;
        t.bankCode = bankCode;
        t.destinationBranch = branch;
        t.destinationAccountNumber = accountNumber;
        t.amount = amount;
        return t;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Account getSource() {
        return source;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Account getDestination() {
        return destination;
    }

    public String getDestinationCreditUnionCode() {
        return destinationCreditUnionCode;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getDestinationBranch() {
        return destinationBranch;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }
}
