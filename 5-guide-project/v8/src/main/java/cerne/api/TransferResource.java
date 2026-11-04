package cerne.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import cerne.entries.Entry;
import cerne.statements.StatementService;
import cerne.transfers.Transfer;
import cerne.transfers.TransferService;
import cerne.util.CerneUtil;

/** JAX-RS resource for transfers (compiles against the API; the server is what runs it). */
@Path("/transfers")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransferResource {

    @Inject
    private TransferService service;

    @Inject
    private StatementService statements;

    @POST
    public Response transfer(Transfer transfer) {
        String identifier = service.transfer(transfer);
        return Response.status(Response.Status.CREATED).entity(identifier).build();
    }

    @GET
    @Path("/statement/{cpf}")
    public List<EntryDTO> entries(@PathParam("cpf") String cpf,
                                  @QueryParam("start") String start,
                                  @QueryParam("end") String end) {
        List<Entry> entries;
        try {
            entries = statements.entriesForPeriod(cpf, CerneUtil.parseDate(start), CerneUtil.parseDate(end));
        } catch (Exception e) {
            throw new BadRequestException("Invalid period");
        }
        if (entries == null) {
            return Collections.emptyList();
        }
        List<EntryDTO> dtos = new ArrayList<EntryDTO>();
        for (Entry entry : entries) {
            dtos.add(EntryDTO.from(entry));
        }
        return dtos;
    }

    /** Sums the credits of a response (used in the app header). */
    public double totalCredits(List<EntryDTO> dtos) {
        double total = 0;
        for (EntryDTO dto : dtos) {
            if (Entry.CREDIT.equals(dto.getType())) {
                total += dto.getAmount();
            }
        }
        return total;
    }
}
