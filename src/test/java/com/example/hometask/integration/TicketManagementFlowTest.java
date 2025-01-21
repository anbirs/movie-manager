package com.example.hometask.integration;


import com.example.hometask.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Component
class TicketManagementFlowTest{

    @Autowired
    private TestUtils testUtils;

    void testTicketManagement(TestRestTemplate restTemplate, HttpHeaders adminHeaders,  HttpHeaders customerHeaders) {

        Long movieId = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, new HttpEntity<>(new Movie(null, "Newest", "dsff", 190, "ere", 2025), adminHeaders), new ParameterizedTypeReference<>() {}
        ));
        Long showtime1Id = testUtils.extractShowtimeId(restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST,
                new HttpEntity<>(new Showtime(null, movieId, "Theater 1", 100, LocalDateTime.now().plusHours(46), LocalDateTime.now().plusHours(48)), adminHeaders), new ParameterizedTypeReference<>() {}
        ));


        User customerUser = new User(null, "CustomerU", "hfgh", "password", "ROLE_CUSTOMER");
        HttpEntity<User> customerUserRequest = new HttpEntity<>(customerUser);

        ResponseEntity<ApiResponse> customerResponse = restTemplate.exchange(
                "/v1/users/register/customer", HttpMethod.POST, customerUserRequest, new ParameterizedTypeReference<>() {}
        );

        User adminUser = new User(null, "Admin888", "8989@example.com", "password", "ROLE_ADMIN");
        HttpEntity<User> adminUserRequest = new HttpEntity<>(adminUser, adminHeaders);

        ResponseEntity<ApiResponse<Long>> adminResponse = restTemplate.exchange(
                "/v1/users/register/admin", HttpMethod.POST, adminUserRequest, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        Long adminUserId = Objects.requireNonNull(adminResponse.getBody()).getData();


        // create ticket: customer
        Long customerId = Long.valueOf((Integer)customerResponse.getBody().getData());
        Ticket customerTicket = new Ticket(null, customerId, showtime1Id, "A1", BigDecimal.valueOf(10.0));
        HttpEntity<Ticket> customerTicketRequest = new HttpEntity<>(customerTicket, customerHeaders);

        ResponseEntity<ApiResponse> customerTicketResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.POST, customerTicketRequest, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, customerTicketResponse.getStatusCode());
        Long customerTicketId = testUtils.extracTicketrId(customerTicketResponse);

        // create ticket: admin
        Ticket adminTicket = new Ticket(null, adminUserId, showtime1Id, "B1", BigDecimal.valueOf(15.0));
        HttpEntity<Ticket> adminTicketRequest = new HttpEntity<>(adminTicket, adminHeaders);

        ResponseEntity<ApiResponse<Long>> adminTicketResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.POST, adminTicketRequest, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.FORBIDDEN, adminTicketResponse.getStatusCode());

        // create invalid ticket: customer (duplicate seat)
        Ticket invalidTicket = new Ticket(null,  customerId, showtime1Id, "A1", BigDecimal.valueOf(12.0));
        ResponseEntity<ApiResponse> invalidTicketResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.POST, new HttpEntity<>(invalidTicket, customerHeaders), new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidTicketResponse.getStatusCode());

        // get all tickets: admin
        ResponseEntity<ApiResponse<List<Ticket>>> allTicketsAdminResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.GET, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, allTicketsAdminResponse.getStatusCode());
        assertTrue(!Objects.requireNonNull(allTicketsAdminResponse.getBody()).getData().isEmpty());

        // get all tickets by user: customer
        ResponseEntity<ApiResponse<List<Ticket>>> ticketsByUserResponse = restTemplate.exchange(
                "/v1/tickets/user/1", HttpMethod.GET, new HttpEntity<>(customerHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, ticketsByUserResponse.getStatusCode());

        // get all tickets: customer
        ResponseEntity<ApiResponse<List<Ticket>>> allTicketsCustomerResponse = restTemplate.exchange(
                "/v1/tickets/user/" + customerId, HttpMethod.GET, new HttpEntity<>(customerHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, allTicketsCustomerResponse.getStatusCode());
        assertTrue(!Objects.requireNonNull(allTicketsCustomerResponse.getBody()).getData().isEmpty());

        ResponseEntity<ApiResponse<List<Ticket>>> allTicketsShowtimeResponse = restTemplate.exchange(
                "/v1/tickets/showtime/" + showtime1Id, HttpMethod.GET, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, allTicketsShowtimeResponse.getStatusCode());
        assertTrue(!Objects.requireNonNull(allTicketsShowtimeResponse.getBody()).getData().isEmpty());

        // update ticket: customer
        Ticket updatedTicket = new Ticket(customerTicketId, customerId, showtime1Id, "C1", BigDecimal.valueOf(20.0));
        ResponseEntity<ApiResponse<Ticket>> updateTicketResponse = restTemplate.exchange(
                "/v1/tickets/" + customerTicketId, HttpMethod.PUT, new HttpEntity<>(updatedTicket, customerHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, updateTicketResponse.getStatusCode());
        assertEquals(updatedTicket.getSeatNumber(), Objects.requireNonNull(updateTicketResponse.getBody()).getData().getSeatNumber());

        // delete ticket: customer
        ResponseEntity<ApiResponse<Long>> deleteTicketResponse = restTemplate.exchange(
                "/v1/tickets/" + customerTicketId, HttpMethod.DELETE, new HttpEntity<>(customerHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, deleteTicketResponse.getStatusCode());
        assertEquals(customerTicketId, Objects.requireNonNull(deleteTicketResponse.getBody()).getData());

        // verify deletion
        ResponseEntity<ApiResponse<List<Ticket>>> verifyDeletionResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.GET, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, verifyDeletionResponse.getStatusCode());
        assertFalse(Objects.requireNonNull(verifyDeletionResponse.getBody()).getData()
                .stream().anyMatch(ticket -> ticket.getId().equals(customerTicketId)));
    }

}
