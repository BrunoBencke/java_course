package cerne.reconciliation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ReconciliationItem {

    @XmlAttribute
    private String date;
    @XmlAttribute
    private String type;
    @XmlAttribute
    private String amount;

    public ReconciliationItem() {
    }

    public ReconciliationItem(String date, String type, String amount) {
        this.date = date;
        this.type = type;
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getAmount() {
        return amount;
    }
}
