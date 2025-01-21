package com.example.hometask.integration;

import com.example.hometask.config.JwtTokenProvider;
import com.example.hometask.repository.MovieRepository;
import com.example.hometask.repository.ShowtimeRepository;
import com.example.hometask.repository.TicketRepository;
import com.example.hometask.repository.UserRepository;
import com.example.hometask.repository.entity.*;
import com.example.hometask.service.MovieService;
import com.example.hometask.service.ShowtimesService;
import com.example.hometask.service.TicketService;
import com.example.hometask.service.UserService;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class EndToEndTestRunnerIT {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShowtimesService showtimesService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EndToEndFlowTest endToEndFlowTest;

    @Autowired
    private MovieManagementFlowTest movieManagementFlowTest;

    @Autowired
    private ShowtimeManagementFlowTest showtimeManagementFlowTest;

    @Autowired
    private UserManagementFlowTest userManagementFlowTest;

    @Autowired
    private TicketManagementFlowTest ticketManagementFlowTest;

    @Getter
    private HttpHeaders adminHeaders;
    @Getter
    private HttpHeaders customerHeaders;

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        List<MovieEntity> movies = movieRepository.findAll();
        movies.forEach(e -> movieService.deleteMovie(e.getId()));
        List<TicketEntity> tickets = ticketRepository.findAll();
        tickets.forEach(ticket -> ticketService.deleteTicket(ticket.getId()));
        List<UserEntity> users = userRepository.findAll();
        users.forEach(user -> userService.deleteUser(user.getId()));
        List<ShowtimeEntity> showtimes = showtimeRepository.findAll();
        showtimes.forEach(showtime -> showtimesService.deleteShowtime(showtime.getId()));


        // Add admin user
        UserEntity adminUser = new UserEntity(
                null,
                "admin",
                "admin@example.com",
                Role.ROLE_ADMIN,
                passwordEncoder.encode("password")
        );
        userRepository.save(adminUser);

        // Add customer user
        UserEntity customerUser = new UserEntity(
                null,
                "customer",
                "customer@example.com",
                Role.ROLE_CUSTOMER,
                passwordEncoder.encode("password")
        );
        userRepository.save(customerUser);

        // Authenticate admin user
        Authentication adminAuthentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        "admin", // Username
                        "password" // Raw password
                )
        );

        // Generate token for admin
        String adminToken = jwtTokenProvider.generateToken(adminAuthentication);

        adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);

        Authentication customerAuthentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        "customer", "password"
                )
        );
        String customerToken = jwtTokenProvider.generateToken(customerAuthentication);

        customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(customerToken);

    }

    @Test
    void testEndToEndFlow() {
        endToEndFlowTest.testEndToEndFlow(restTemplate, adminHeaders,  customerHeaders);
    }

    @Test
    void testMovieManagement() {
        movieManagementFlowTest.testMovieManagement(restTemplate, adminHeaders,  customerHeaders);
    }

    @Test
    void testUserManagement() {
        userManagementFlowTest.testUserManagement(restTemplate, adminHeaders,  customerHeaders);
    }

    @Test
    void testShowtimeManagement() {
        showtimeManagementFlowTest.testShowtimeManagement(restTemplate, adminHeaders,  customerHeaders);
    }

    @Test
    void testTicketManagement() {
        ticketManagementFlowTest.testTicketManagement(restTemplate, adminHeaders,  customerHeaders);
    }
}
