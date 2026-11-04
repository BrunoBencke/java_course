package cerne.reconciliation;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import cerne.entries.Entry;

/** Generates the reconciliation XML with JAXB 4 (jakarta.xml.bind). The test's reference file did not change. */
public class ReconciliationFileGenerator {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            BigDecimal amount = entry.amount().setScale(2, RoundingMode.HALF_EVEN);
            items.add(new ReconciliationItem(DAY_MONTH_YEAR.format(entry.date()), entry.type().name(),
                    amount.toPlainString()));
            total = entry.isCredit() ? total.add(amount) : total.subtract(amount);
        }
        var signature = Base64.getEncoder().encodeToString(
                (accountNumber + "|" + total.toPlainString() + "|" + items.size()).getBytes(StandardCharsets.UTF_8));
        var file = new ReconciliationFile(accountNumber, items, total.toPlainString(), signature);
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            var output = new StringWriter();
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
