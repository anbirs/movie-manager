package com.example.hometask.integration;


import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.Movie;
import com.example.hometask.data.Showtime;
import com.example.hometask.data.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
class EndToEndFlowTest {

    @Autowired
    private TestUtils testUtils;

    void testEndToEndFlow(TestRestTemplate restTemplate, HttpHeaders adminHeaders,  HttpHeaders customerHeaders) {

        Movie movieRequest =  new Movie(null, "Test Movie 1", "Abc", 123, "KKK", 2020);
        HttpEntity<Movie> requestMovies = new HttpEntity<>(movieRequest, adminHeaders);
        ResponseEntity<ApiResponse> movieResponse = restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, requestMovies, new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, movieResponse.getStatusCode());
        Long movieId = testUtils.extractMovieId(movieResponse);

        Showtime showtimeRequest = new Showtime(null, movieId, "Theater 1", 100, LocalDateTime.now(), LocalDateTime.now().plusHours(2));
        HttpEntity<Showtime> requesShowtimes = new HttpEntity<>(showtimeRequest, adminHeaders);
        ResponseEntity<ApiResponse> showtimeResponse = restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST, requesShowtimes, new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, showtimeResponse.getStatusCode());
        Long showtimeId = testUtils.extractShowtimeId(showtimeResponse);

        Ticket ticketRequest = new Ticket(null, null, showtimeId, "A1", BigDecimal.valueOf(10.0));
        HttpEntity<Ticket> requestTickets = new HttpEntity<>(ticketRequest, customerHeaders);
        ResponseEntity<ApiResponse> ticketResponse = restTemplate.exchange(
                "/v1/tickets", HttpMethod.POST, requestTickets, new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, ticketResponse.getStatusCode());

        HttpEntity<Ticket> requestTicketsList = new HttpEntity<>(adminHeaders);

        ResponseEntity<ApiResponse<List<Ticket>>> allTicketsResponse = restTemplate.exchange(
                "/v1/tickets/showtime/" + showtimeId, HttpMethod.GET, requestTicketsList,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, allTicketsResponse.getStatusCode());
        assertEquals(1, Objects.requireNonNull(allTicketsResponse.getBody()).getData().size());
    }

}
