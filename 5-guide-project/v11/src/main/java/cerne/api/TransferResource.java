package cerne.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

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

import cerne.statements.StatementService;
import cerne.entries.Entry;
import cerne.entries.EntryType;
import cerne.transfers.TransferService;
import cerne.transfers.Transfer;

/** JAX-RS resource for transfers (compiles against the API; the server runs it). */
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
            entries = statements.entriesForPeriod(cpf, LocalDate.parse(start), LocalDate.parse(end));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid period");
        }
        return entries.stream().map(EntryDTO::from).collect(Collectors.toList());
    }

    /** Sums the credits of a response (used in the app header). */
    public BigDecimal totalCredits(List<EntryDTO> dtos) {
        return dtos.stream()
                .filter(dto -> dto.getType() == EntryType.CREDIT)
                .map(EntryDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
