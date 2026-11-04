package cerne.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import cerne.entries.EntryType;
import cerne.statements.StatementService;
import cerne.transfers.InternalTransfer;
import cerne.transfers.TransferRequest;
import cerne.transfers.TransferService;

/** JAX-RS resource for transfers (jakarta.ws.rs). Receives the bean already decorated by the container. */
@Path("/transfers")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransferResource {

    @Inject
    TransferService service;

    @Inject
    StatementService statements;

    @POST
    @Path("/internal")
    public Response transfer(InternalTransfer transfer) {
        var receipt = service.transfer(TransferRequest.of(transfer));
        return Response.status(Response.Status.CREATED).entity(receipt.identifier()).build();
    }

    @GET
    @Path("/statement/{cpf}")
    public List<EntryDTO> entries(@PathParam("cpf") String cpf,
                                  @QueryParam("start") String start,
                                  @QueryParam("end") String end) {
        try {
            return statements.entriesForPeriod(cpf, LocalDate.parse(start), LocalDate.parse(end)).stream()
                    .map(EntryDTO::from)
                    .toList();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid period");
        }
    }

    public BigDecimal totalCredits(List<EntryDTO> dtos) {
        return dtos.stream()
                .filter(dto -> dto.type() == EntryType.CREDIT)
                .map(EntryDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
