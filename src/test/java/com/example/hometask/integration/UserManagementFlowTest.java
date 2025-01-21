package com.example.hometask.integration;


import com.example.hometask.data.ApiResponse;
import com.example.hometask.data.User;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Component
class UserManagementFlowTest{

    void testUserManagement(TestRestTemplate restTemplate, HttpHeaders adminHeaders,  HttpHeaders customerHeaders) {
        // create admin: admin
        User adminUser = new User(null, "AdminUser", "admin@example.com", "password", "ROLE_ADMIN");
        HttpEntity<User> adminUserRequest = new HttpEntity<>(adminUser, adminHeaders);

        ResponseEntity<ApiResponse<Long>> adminResponse = restTemplate.exchange(
                "/v1/users/register/admin", HttpMethod.POST, adminUserRequest, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
        Long adminUserId = Objects.requireNonNull(adminResponse.getBody()).getData();

        // create customer: no auth
        User customerUser = new User(null, "CustomerUser", "customer@example.com", "password", "ROLE_CUSTOMER");
        HttpEntity<User> customerUserRequest = new HttpEntity<>(customerUser);

        ResponseEntity<ApiResponse<Long>> customerResponse = restTemplate.exchange(
                "/v1/users/register/customer", HttpMethod.POST, customerUserRequest, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
        Long customerUserId = Objects.requireNonNull(customerResponse.getBody()).getData();

        // get all users: admin
        ResponseEntity<ApiResponse<List<User>>> adminGetAllUsersResponse = restTemplate.exchange(
                "/v1/users", HttpMethod.GET, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, adminGetAllUsersResponse.getStatusCode());
        assertTrue(Objects.requireNonNull(adminGetAllUsersResponse.getBody()).getData().size() >= 2); // At least admin + customer

        // get all users: customer
        ResponseEntity<ApiResponse<List<User>>> customerGetAllUsersResponse = restTemplate.exchange(
                "/v1/users", HttpMethod.GET, new HttpEntity<>(customerHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.FORBIDDEN, customerGetAllUsersResponse.getStatusCode()); // Assuming "ROLE_CUSTOMER" cannot view all users

        // delete user: admin
        ResponseEntity<ApiResponse<Long>> deleteCustomerResponse = restTemplate.exchange(
                "/v1/users/" + customerUserId, HttpMethod.DELETE, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, deleteCustomerResponse.getStatusCode());
        assertEquals(customerUserId, Objects.requireNonNull(deleteCustomerResponse.getBody()).getData());

        // verify deletion
        ResponseEntity<ApiResponse<List<User>>> getAllUsersAfterDeletionResponse = restTemplate.exchange(
                "/v1/users", HttpMethod.GET, new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, getAllUsersAfterDeletionResponse.getStatusCode());
        assertFalse(Objects.requireNonNull(getAllUsersAfterDeletionResponse.getBody()).getData()
                .stream().anyMatch(user -> user.getId().equals(customerUserId)));
    }
}
