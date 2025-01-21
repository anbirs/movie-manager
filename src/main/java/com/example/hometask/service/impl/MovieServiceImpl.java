package com.example.hometask.service.impl;

import ch.qos.logback.core.util.StringUtil;
import com.example.hometask.data.Movie;
import com.example.hometask.repository.MovieRepository;
import com.example.hometask.repository.ShowtimeRepository;
import com.example.hometask.service.mapper.MovieField;
import com.example.hometask.service.MovieService;
import com.example.hometask.service.mapper.MovieMapper;
import com.example.hometask.repository.entity.MovieEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {
    public static final String MOVIE_NOT_FOUND = "Movie not found ";
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public Movie saveMovie(Movie movie) {
        MovieEntity entity = movieMapper.toEntity(movie);
        return movieMapper.toDto(movieRepository.save(entity));
    }

    @Override
    public List<Movie> getAllMovies(String details) {

        final List<MovieEntity> responseList = new ArrayList<>();
        if (StringUtil.isNullOrEmpty(details)) {
            responseList.addAll(movieRepository.findAll());
        } else {
            responseList.addAll(mapDetailsToFields(details));
        }

        return responseList.stream()
                .map(movieMapper::toDto)
                .collect(Collectors.toList());
    }

    private List<MovieEntity> mapDetailsToFields(String details) {
        final List<MovieField> fieldsToFetch = MovieField.parseFields(details);
        return movieRepository.findAllMoviesByDynamicFields(fieldsToFetch).stream()
              // Object[]
                .map(row -> {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (int i = 0; i < fieldsToFetch.size(); i++) {
                        rowMap.put(fieldsToFetch.get(i).getFieldName(), row[i]);
                    }
                    return objectMapper.convertValue(rowMap, MovieEntity.class);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Movie getMovieById(Long id) {
        return movieRepository.findById(id).map(movieMapper::toDto).orElseThrow(() -> new EntityNotFoundException(MOVIE_NOT_FOUND + id));
    }

    @Override
    public Movie updateMovie(Long id, Movie updatedMovie) {
        return movieRepository.findById(id)
                .map(existingEntity -> {
                    movieMapper.updateEntityFromDto(updatedMovie, existingEntity);
                    return movieRepository.save(existingEntity);
                })
                .map(movieMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(MOVIE_NOT_FOUND + id));

    }

    @Override
    @Transactional
    public Long deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new EntityNotFoundException(MOVIE_NOT_FOUND + id);
        }
        showtimeRepository.cleanMovieReferences(id);
        movieRepository.deleteById(id);
        return id;
    }
}
