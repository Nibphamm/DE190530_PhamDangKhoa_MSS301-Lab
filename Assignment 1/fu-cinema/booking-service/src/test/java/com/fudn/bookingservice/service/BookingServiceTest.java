package com.fudn.bookingservice.service;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.*;
import com.fudn.bookingservice.repository.*;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final String SHOWTIME = "66f300000000000000000001";
    @Mock BookingRepository bookings;
    @Mock BookingDetailRepository details;
    @Mock MovieClient movies;
    @InjectMocks BookingService service;

    private ShowtimeResponse showtime(String status, LocalDateTime start) {
        return new ShowtimeResponse(SHOWTIME, "66f200000000000000000001", "Galaxy Rangers",
                "66f100000000000000000001", "Room 01", 5, 8, start,
                start.plusMinutes(125), new BigDecimal("95000.00"), status);
    }

    private CreateBookingRequest request(String... seats) {
        return new CreateBookingRequest(List.of(seats).stream()
                .map(seat -> new BookingItemRequest(SHOWTIME, seat)).toList());
    }

    private Booking booking(Long owner, LocalDateTime start, BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingId(7L);
        booking.setCustomerId(owner);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingStatus(status);
        booking.setTotalPrice(new BigDecimal("95000.00"));
        BookingDetail ticket = new BookingDetail();
        ticket.setShowtimeStart(start);
        ticket.setMovieId("66f200000000000000000001");
        ticket.setMovieTitle("Galaxy Rangers");
        ticket.setPrice(new BigDecimal("95000.00"));
        booking.addDetail(ticket);
        return booking;
    }

    private void expectStatus(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ApiException.class, action::run).getStatus());
        verify(bookings, never()).save(any());
    }

    @Test void calculatesPricesSnapshotsAndFetchesShowtimeOnce() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("SCHEDULED", LocalDateTime.now().plusDays(7)));
        when(details.findSeatCodesByShowtime(SHOWTIME, BookingStatus.CONFIRMED)).thenReturn(List.of());
        when(bookings.save(any())).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(7L);
            return booking;
        });
        BookingResponse result = service.create(1L, request("E5", "E6"));
        assertEquals(new BigDecimal("190000.00"), result.totalPrice());
        assertEquals(2, result.details().size());
        assertEquals("Galaxy Rangers", result.details().getFirst().movieTitle());
        verify(movies, times(1)).getShowtime(SHOWTIME);
        verify(details, times(1)).findSeatCodesByShowtime(SHOWTIME, BookingStatus.CONFIRMED);
    }

    @Test void rejectsDuplicateRequestWithoutSaving() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("SCHEDULED", LocalDateTime.now().plusDays(7)));
        when(details.findSeatCodesByShowtime(SHOWTIME, BookingStatus.CONFIRMED)).thenReturn(List.of());
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L, request("A2", "A2")));
    }

    @Test void rejectsSoldSeatWithoutSaving() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("SCHEDULED", LocalDateTime.now().plusDays(7)));
        when(details.findSeatCodesByShowtime(SHOWTIME, BookingStatus.CONFIRMED)).thenReturn(List.of("E5"));
        expectStatus(HttpStatus.CONFLICT, () -> service.create(1L, request("E5")));
    }

    @Test void rejectsStartedShowtime() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("SCHEDULED", LocalDateTime.now().minusMinutes(1)));
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L, request("A1")));
    }

    @Test void rejectsCancelledShowtime() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("CANCELLED", LocalDateTime.now().plusDays(7)));
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L, request("A1")));
    }

    @Test void rejectsSeatOutsideRoom() {
        when(movies.getShowtime(SHOWTIME)).thenReturn(showtime("SCHEDULED", LocalDateTime.now().plusDays(7)));
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L, request("F1")));
    }

    @Test void mapsMovieConnectionFailureTo503() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/showtimes/" + SHOWTIME,
                Map.of(), (byte[]) null, null, null);
        when(movies.getShowtime(SHOWTIME)).thenThrow(new RetryableException(
                -1, "Connection refused", Request.HttpMethod.GET, (Long) null, request));
        expectStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> service.create(1L, request("A1")));
    }

    @Test void otherCustomerCannotReadOrCancel() {
        when(bookings.findById(7L)).thenReturn(java.util.Optional.of(
                booking(1L, LocalDateTime.now().plusDays(7), BookingStatus.CONFIRMED)));
        expectStatus(HttpStatus.FORBIDDEN, () -> service.getById(7L, 2L, "CUSTOMER"));
        expectStatus(HttpStatus.FORBIDDEN, () -> service.cancel(7L, 2L, "CUSTOMER"));
    }

    @Test void customerCannotCancelInsideTwoHours() {
        when(bookings.findById(7L)).thenReturn(java.util.Optional.of(
                booking(1L, LocalDateTime.now().plusMinutes(119), BookingStatus.CONFIRMED)));
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.cancel(7L, 1L, "CUSTOMER"));
    }

    @Test void adminCanCancelAfterShowtimeStarts() {
        when(bookings.findById(7L)).thenReturn(java.util.Optional.of(
                booking(1L, LocalDateTime.now().minusHours(1), BookingStatus.CONFIRMED)));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(BookingStatus.CANCELLED, service.cancel(7L, 0L, "ADMIN").bookingStatus());
    }

    @Test void reportIncludesEntireEndDayAndGroupsTicketSnapshots() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 7);
        when(bookings.findForReport(BookingStatus.CONFIRMED, start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(booking(1L, LocalDateTime.now().plusDays(7), BookingStatus.CONFIRMED)));
        ReportResponse report = service.report(start, end);
        assertEquals(1, report.totalTickets());
        assertEquals(new BigDecimal("95000.00"), report.totalRevenue());
        assertEquals("Galaxy Rangers", report.revenueByMovie().getFirst().movieTitle());
        verifyNoInteractions(movies);
    }

    @Test void rejectsReversedReportDates() {
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.report(LocalDate.now(), LocalDate.now().minusDays(1)));
        verifyNoInteractions(details, movies);
    }
}
