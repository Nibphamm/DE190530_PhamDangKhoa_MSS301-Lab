package com.fudn.movieservice.service;

import com.fudn.movieservice.dto.ShowtimeRequest;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.model.*;
import com.fudn.movieservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {
    @Mock ShowtimeRepository showtimes;
    @Mock MovieRepository movies;
    @Mock RoomRepository rooms;
    @Mock MovieService movieService;
    @Mock RoomService roomService;
    @InjectMocks ShowtimeService service;
    Movie movie;
    CinemaRoom room;

    @BeforeEach void setUp() {
        movie = new Movie();
        movie.setMovieId("66f200000000000000000001");
        movie.setTitle("Galaxy Rangers");
        movie.setDurationMinutes(125);
        movie.setMovieStatus(MovieStatus.NOW_SHOWING);
        room = new CinemaRoom("66f100000000000000000001", "Room 01", RoomType.STANDARD, 8, 10, RoomStatus.ACTIVE);
    }

    private ShowtimeRequest request(LocalDateTime start) {
        return new ShowtimeRequest(movie.getMovieId(), room.getRoomId(), start, new BigDecimal("95000"));
    }

    private void references() {
        when(movieService.find(movie.getMovieId())).thenReturn(movie);
        when(roomService.find(room.getRoomId())).thenReturn(room);
    }

    @Test void computesEndTimeAndUsesHalfOpenOverlapQuery() {
        references();
        LocalDateTime start = LocalDateTime.now().plusDays(7);
        when(showtimes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(request(start));
        assertEquals(start.plusMinutes(125), response.endTime());
        verify(showtimes).countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(
                room.getRoomId(), ShowtimeStatus.SCHEDULED, start.plusMinutes(125), start, "");
    }

    @Test void overlapReturns409AndDoesNotSave() {
        references();
        when(showtimes.countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(
                anyString(), any(), any(), any(), anyString())).thenReturn(1L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> service.create(request(LocalDateTime.now().plusDays(7)))).getStatus());
        verify(showtimes, never()).save(any());
    }

    @Test void endedMovieCannotBeScheduled() {
        references();
        movie.setMovieStatus(MovieStatus.ENDED);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.create(request(LocalDateTime.now().plusDays(7)))).getStatus());
        verifyNoInteractions(showtimes);
    }

    @Test void maintenanceRoomCannotBeScheduled() {
        references();
        room.setRoomStatus(RoomStatus.MAINTENANCE);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.create(request(LocalDateTime.now().plusDays(7)))).getStatus());
        verifyNoInteractions(showtimes);
    }

    @Test void missingMovieReturns404() {
        when(movieService.find(movie.getMovieId())).thenThrow(ApiException.notFound("Movie not found"));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
                () -> service.create(request(LocalDateTime.now().plusDays(7)))).getStatus());
        verifyNoInteractions(showtimes, roomService);
    }

    @Test void pastShowtimeCannotBeScheduled() {
        references();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.create(request(LocalDateTime.now().minusMinutes(1)))).getStatus());
        verifyNoInteractions(showtimes);
    }
}
