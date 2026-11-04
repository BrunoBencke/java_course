package cerne.reconciliation;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import cerne.entries.Entry;
import cerne.util.CerneUtil;

/** Generates the reconciliation XML with JAXB; since 11 the API comes from jakarta.xml.bind-api 2.3.3 (still javax.*). */
public class ReconciliationFileGenerator {

    private final JAXBContext context;

    public ReconciliationFileGenerator() {
        try {
            context = JAXBContext.newInstance(ReconciliationFile.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("JAXB unavailable", e);
        }
    }

    public String generate(String accountNumber, List<Entry> entries) {
        List<ReconciliationItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Entry entry : entries) {
            BigDecimal amount = entry.getAmount().setScale(2, RoundingMode.HALF_EVEN);
            items.add(new ReconciliationItem(CerneUtil.formatDate(entry.getDate()), entry.getType().name(),
                    amount.toPlainString()));
            total = entry.isCredit() ? total.add(amount) : total.subtract(amount);
        }
        String signature = CerneUtil.encodeBase64(
                (accountNumber + "|" + total.toPlainString() + "|" + items.size()).getBytes(StandardCharsets.UTF_8));
        ReconciliationFile file = new ReconciliationFile(accountNumber, items, total.toPlainString(), signature);
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter output = new StringWriter();
            marshaller.marshal(file, output);
            return output.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to generate reconciliation file", e);
        }
    }

    public ReconciliationFile read(String xml) {
        try {
            return (ReconciliationFile) context.createUnmarshaller().unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new IllegalStateException("Invalid reconciliation file", e);
        }
    }
}
