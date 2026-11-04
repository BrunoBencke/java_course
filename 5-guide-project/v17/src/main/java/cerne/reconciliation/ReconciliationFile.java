package cerne.reconciliation;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** XML file sent to the central (JAXB requires an empty constructor: it does not become a record). */
@XmlRootElement(name = "reconciliation")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReconciliationFile {

    @XmlAttribute
    private String account;
    @XmlElement(name = "entry")
    private List<ReconciliationItem> items = new ArrayList<ReconciliationItem>();
    @XmlElement
    private String total;
    @XmlElement
    private String signature;

    public ReconciliationFile() {
    }

    public ReconciliationFile(String account, List<ReconciliationItem> items, String total, String signature) {
        this.account = account;
        this.items = items;
        this.total = total;
        this.signature = signature;
    }

    public String getAccount() {
        return account;
    }

    public List<ReconciliationItem> getItems() {
        return items;
    }

    public String getTotal() {
        return total;
    }

    public String getSignature() {
        return signature;
    }
}
