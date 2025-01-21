package com.example.hometask.integration;


import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
class MovieManagementFlowTest {

    @Autowired
    private TestUtils testUtils;

    void testMovieManagement(TestRestTemplate restTemplate, HttpHeaders adminHeaders,  HttpHeaders customerHeaders) {

        Movie movie1 = new Movie(null, "Test Movie 1", "Abc", 123, "KKK", 2020);
        Movie movie2 = new Movie(null, "Movie 2", "Cd", 132, "AAA", 1974);
        Movie movie3 = new Movie(null, "Superman", "lll", 190, "UUU", 2025);

        HttpEntity<Movie> movieRequest1 = new HttpEntity<>(movie1, adminHeaders);
        HttpEntity<Movie> movieRequest2 = new HttpEntity<>(movie2, adminHeaders);
        HttpEntity<Movie> movieRequest3 = new HttpEntity<>(movie3, adminHeaders);
        Long idToUpdate1 = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest1, new ParameterizedTypeReference<>() {}
        ));
        Long idToUpdate2 = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest2, new ParameterizedTypeReference<>() {}
        ));
        Long idToUpdate3 = testUtils.extractMovieId(restTemplate.exchange(
                "/v1/movies", HttpMethod.POST, movieRequest3, new ParameterizedTypeReference<>() {}
        ));

        HttpEntity requestMoviesList = new HttpEntity<>(customerHeaders);

        ResponseEntity<ApiResponse<List<Movie>>> moviesResponse = restTemplate.exchange(
                "/v1/movies", HttpMethod.GET, requestMoviesList,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, moviesResponse.getStatusCode());
        assertEquals(3, Objects.requireNonNull(moviesResponse.getBody()).getData().size());

        ResponseEntity<ApiResponse<List<Movie>>> moviesDetailsResponse = restTemplate.exchange(
                "/v1/movies?details=title,genre,duration", HttpMethod.GET, requestMoviesList,
                new ParameterizedTypeReference<>() {}
        );

        List<Movie> movieDetails = List.of(new Movie(null, "Test Movie 1", "Abc", 123, null, null),
                    new Movie(null, "Movie 2", "Cd", 132, null, null),
                    new Movie(null, "Superman", "lll", 190, null, null));


        assertEquals(HttpStatus.OK, moviesDetailsResponse.getStatusCode());
        List<Movie> respData = Objects.requireNonNull(moviesDetailsResponse.getBody()).getData();
        assertEquals(3, respData.size());
        assertEquals(movieDetails, respData);

        Movie movieUpdateDto = new Movie(null, "Updated Movie 1", "ddd", 200, "ttt", 2000);
        HttpEntity<Movie> movieUpdate = new HttpEntity<>(movieUpdateDto, adminHeaders);
        ResponseEntity<ApiResponse> movieUpdateResponse = restTemplate.exchange(
                "/v1/movies/" + idToUpdate1, HttpMethod.PUT, movieUpdate, new ParameterizedTypeReference<>() {}
        );

        movieUpdateDto.setId(testUtils.extractMovieId(movieUpdateResponse));
        assertEquals(HttpStatus.OK, moviesDetailsResponse.getStatusCode());
        Movie movieUpdateResponseData = testUtils.extractMovie(movieUpdateResponse);

        assertEquals(movieUpdateDto, movieUpdateResponseData);

        // delete
        ResponseEntity<ApiResponse<Long>> deleteResponse = restTemplate.exchange(
                "/v1/movies/" + idToUpdate3, HttpMethod.DELETE, requestMoviesList,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());

        restTemplate.exchange(
                "/v1/movies/" + idToUpdate3, HttpMethod.DELETE,  new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );

        // get size == 2
        ResponseEntity<ApiResponse<List<Movie>>> moviesReducedResponse = restTemplate.exchange(
                "/v1/movies", HttpMethod.GET, requestMoviesList,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, moviesReducedResponse.getStatusCode());
        assertEquals(2, Objects.requireNonNull(moviesReducedResponse.getBody()).getData().size());

        ResponseEntity<ApiResponse> movieResponse = restTemplate.exchange(
                "/v1/movies/" + idToUpdate2, HttpMethod.GET, requestMoviesList,
                new ParameterizedTypeReference<>() {}
        );
        movie2.setId(testUtils.extractMovieId(movieResponse));
        assertEquals(HttpStatus.OK, moviesReducedResponse.getStatusCode());
        assertEquals(movie2, testUtils.extractMovie(movieResponse));

    }
}
