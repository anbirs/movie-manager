package com.example.hometask.integration;

import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.Movie;
import com.example.hometask.data.Showtime;
import com.example.hometask.data.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class TestUtils {
    protected final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

    public Long extractMovieId(ResponseEntity<ApiResponse> response) {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) response.getBody().getData();
        Movie movie = objectMapper.convertValue(dataMap, Movie.class);
        return movie.getId();
    }

    public Long extractShowtimeId(ResponseEntity<ApiResponse> response) {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) response.getBody().getData();
        Showtime showtime = objectMapper.convertValue(dataMap, Showtime.class);
        return showtime.getId();
    }

    public Movie extractMovie(ResponseEntity<ApiResponse> response) {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) response.getBody().getData();
        return objectMapper.convertValue(dataMap, Movie.class);
    }


    public Showtime extractShowtime(ResponseEntity<ApiResponse> response) {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) response.getBody().getData();
        return objectMapper.convertValue(dataMap, Showtime.class);
    }

    public Long extracTicketrId(ResponseEntity<ApiResponse> response) {
        LinkedHashMap<String, Object> dataMap = (LinkedHashMap<String, Object>) response.getBody().getData();
        Ticket movie = objectMapper.convertValue(dataMap, Ticket.class);
        return movie.getId();
    }
}
