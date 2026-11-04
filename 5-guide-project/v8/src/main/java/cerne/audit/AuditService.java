package cerne.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.thoughtworks.xstream.XStream;

import cerne.transfers.Transfer;
import cerne.util.CerneUtil;

/** Audit trail in XML via XStream 1.4.9 (uses deep reflection on JDK classes). */
@ApplicationScoped
public class AuditService {

    private final XStream xstream;
    private final List<String> logEntries = Collections.synchronizedList(new ArrayList<String>());

    public AuditService() {
        xstream = new XStream();
        xstream.alias("event", AuditEvent.class);
    }

    public String record(String type, String identifier, Transfer transfer) {
        AuditEvent event = new AuditEvent(type, identifier,
                transfer.getSource().getNumber(), transfer.getAmount(), CerneUtil.formatDateTime(new Date()));
        String xml = xstream.toXML(event);
        logEntries.add(xml);
        return xml;
    }

    public List<String> getRecords() {
        return new ArrayList<String>(logEntries);
    }
}
