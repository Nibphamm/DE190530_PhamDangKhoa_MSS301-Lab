package com.fudn.customerservice.service;

import com.fudn.customerservice.dto.LoginRequest;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.model.Customer;
import com.fudn.customerservice.model.CustomerStatus;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private CustomerRepository customers;
    private AuthService auth;
    private BCryptPasswordEncoder passwords;

    @BeforeEach void setUp() {
        customers = mock(CustomerRepository.class);
        passwords = new BCryptPasswordEncoder();
        auth = new AuthService(customers, passwords,
                new JwtService("fu-cinema-booking-system-secret-key-2026-mss301", 60));
        ReflectionTestUtils.setField(auth, "adminEmail", "admin@fucinema.com");
        ReflectionTestUtils.setField(auth, "adminPassword", "@@abc123@@");
    }

    private void account(CustomerStatus status) {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("an@gmail.com");
        customer.setCustomerName("Nguyễn Văn An");
        customer.setPassword(passwords.encode("123456"));
        customer.setCustomerStatus(status);
        when(customers.findByEmailIgnoreCase("an@gmail.com")).thenReturn(Optional.of(customer));
    }

    @Test void adminUsesPropertiesAndSignsRequiredClaims() throws Exception {
        var login = auth.login(new LoginRequest("admin@fucinema.com", "@@abc123@@"));
        var token = SignedJWT.parse(login.accessToken());
        assertEquals("HS256", token.getHeader().getAlgorithm().getName());
        assertEquals("admin@fucinema.com", token.getJWTClaimsSet().getSubject());
        assertEquals(0L, token.getJWTClaimsSet().getLongClaim("uid"));
        assertEquals("ADMIN", token.getJWTClaimsSet().getStringClaim("role"));
        assertEquals(3600000L, token.getJWTClaimsSet().getExpirationTime().getTime()
                - token.getJWTClaimsSet().getIssueTime().getTime());
        verifyNoInteractions(customers);
    }

    @Test void activeCustomerReceivesOwnIdentity() {
        account(CustomerStatus.ACTIVE);
        var login = auth.login(new LoginRequest("an@gmail.com", "123456"));
        assertEquals("CUSTOMER", login.role());
        assertEquals(1L, login.userId());
        assertEquals("Nguyễn Văn An", login.fullName());
    }

    @Test void inactiveCustomerCannotLogin() {
        account(CustomerStatus.INACTIVE);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> auth.login(new LoginRequest("an@gmail.com", "123456"))).getStatus());
    }

    @Test void wrongPasswordReturns401() {
        account(CustomerStatus.ACTIVE);
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class,
                () -> auth.login(new LoginRequest("an@gmail.com", "wrong"))).getStatus());
    }

    @Test void seedHashMatchesDocumentedPassword() {
        assertTrue(passwords.matches("123456", "$2a$10$dmoDdVpWYdqLarqBfkYQteoq1YORLC5LLMd55bpomZ3EarS/vtjtW"));
    }
}
