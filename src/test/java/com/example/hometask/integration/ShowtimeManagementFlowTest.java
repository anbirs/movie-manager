package com.example.hometask.integration;


import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.Movie;
import com.example.hometask.data.Showtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
class ShowtimeManagementFlowTest {

    @Autowired
    private TestUtils testUtils;

    void testShowtimeManagement(TestRestTemplate restTemplate, HttpHeaders adminHeaders,  HttpHeaders customerHeaders) {
       // create movies: admin
        Movie movie1 = new Movie(null, "Test Movie 1", "Abc", 123, "KKK", 2020);
        Movie movie2 = new Movie(null, "Movie 2", "Cd", 132, "AAA", 1974);
        Movie movie3 = new Movie(null, "Superman", "lll", 190, "UUU", 2025);

        HttpEntity<Movie> movieRequest1 = new HttpEntity<>(movie1, adminHeaders);
        HttpEntity<Movie> movieRequest2 = new HttpEntity<>(movie2, adminHeaders);
        HttpEntity<Movie> movieRequest3 = new HttpEntity<>(movie3, adminHeaders);
        HttpEntity requestGetAsAdmin = new HttpEntity<>(adminHeaders);
        Long movie1Id = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest1, new ParameterizedTypeReference<>() {}
        ));
        Long movie2Id = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest2, new ParameterizedTypeReference<>() {}
        ));
        Long movie3Id = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest3, new ParameterizedTypeReference<>() {}
        ));

        // create showtime: admin
        var start = LocalDateTime.now().plusHours(24);
        var end = LocalDateTime.now().plusHours(26);
        Showtime showtimeRequest = new Showtime(null, movie1Id, "Theater 1", 100, start, end);
        HttpEntity<Showtime> postShowtimes = new HttpEntity<>(showtimeRequest, adminHeaders);
        ResponseEntity<ApiResponse> showtimeResponse = restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST, postShowtimes, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, showtimeResponse.getStatusCode());

        // create overlapping showtime: admin
        Showtime showtimeOverlappingRequest = new Showtime(null, movie1Id, "Theater 1", 100, start, end);
        ResponseEntity<ApiResponse> showtimeOverlappingResponse = restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST, new HttpEntity<>(showtimeOverlappingRequest, adminHeaders), new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.BAD_REQUEST, showtimeOverlappingResponse.getStatusCode());

        // create more stowtimes: admin
        restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST,
                new HttpEntity<>(new Showtime(null, movie2Id, "Theater 1", 100, start.plusHours(4), end.plusHours(6)), adminHeaders), new ParameterizedTypeReference<>() {}
        );

        restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST,
                new HttpEntity<>(new Showtime(null, movie1Id, "Theater 2", 100, start, end), adminHeaders), new ParameterizedTypeReference<>() {}
        );

        restTemplate.exchange(
                "/v1/showtimes", HttpMethod.POST,
                new HttpEntity<>(new Showtime(null, movie2Id, "Theater 3", 100, start, end), adminHeaders), new ParameterizedTypeReference<>() {}
        );

        // get all check size: customer
        HttpEntity requestGetAsCustomer = new HttpEntity<>(customerHeaders);

        ResponseEntity<ApiResponse<List<Showtime>>> stResponse = restTemplate.exchange(
                "/v1/showtimes", HttpMethod.GET, requestGetAsCustomer,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, stResponse.getStatusCode());
        assertEquals(4, Objects.requireNonNull(stResponse.getBody()).getData().size());

        // update showtime: admin
        Long idToUpdate = testUtils.extractShowtimeId(showtimeResponse);
        ResponseEntity<ApiResponse> updateShowTimeResponse = restTemplate.exchange(
                "/v1/showtimes/" + idToUpdate, HttpMethod.PUT,
                new HttpEntity<>(new Showtime(null, movie3Id, "Theater 3", 100, start.plusHours(4), end.plusHours(6)), adminHeaders), new ParameterizedTypeReference<>() {}
        );

        Showtime expectedShowtime = new Showtime(testUtils.extractShowtimeId(updateShowTimeResponse), movie3Id, "Theater 3", 100, start.plusHours(4), end.plusHours(6));

        assertEquals(HttpStatus.OK, updateShowTimeResponse.getStatusCode());
        assertEquals(expectedShowtime, testUtils.extractShowtime(updateShowTimeResponse));

        // get not existing showtime: customer
        ResponseEntity<ApiResponse> noStResponse = restTemplate.exchange(
                "/v1/showtimes/100", HttpMethod.GET, requestGetAsCustomer,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.NO_CONTENT, noStResponse.getStatusCode());

        // get showtimes by movie: customer
        ResponseEntity<ApiResponse<List<Showtime>>> stByMovieResponse = restTemplate.exchange(
                "/v1/showtimes?movieId=" + movie3Id, HttpMethod.GET, requestGetAsCustomer,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, stByMovieResponse.getStatusCode());
        assertEquals(1, Objects.requireNonNull(stByMovieResponse.getBody()).getData().size());

        // get showtimes by theater: admin
        ResponseEntity<ApiResponse<List<Showtime>>> stByTheaterResponse = restTemplate.exchange(
                "/v1/showtimes?theaterName=Theater 3", HttpMethod.GET, requestGetAsAdmin,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, stByTheaterResponse.getStatusCode());
        assertEquals(2, Objects.requireNonNull(stByTheaterResponse.getBody()).getData().size());

        // delete showtime: admin
        ResponseEntity<ApiResponse<Long>> deleteResponse = restTemplate.exchange(
                "/v1/showtimes/" + idToUpdate, HttpMethod.DELETE, requestGetAsAdmin,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        ResponseEntity<ApiResponse<List<Showtime>>> getAllResponse = restTemplate.exchange(
                "/v1/showtimes", HttpMethod.GET, requestGetAsAdmin,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, getAllResponse.getStatusCode());
        assertEquals(3, Objects.requireNonNull(getAllResponse.getBody()).getData().size());

    }

}
