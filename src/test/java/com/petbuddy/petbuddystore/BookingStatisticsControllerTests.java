package com.petbuddy.petbuddystore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "SPRING_MAIL_HOST=localhost",
        "SPRING_MAIL_PORT=1025",
        "SPRING_MAIL_USERNAME=test@petbuddy.local",
        "SPRING_MAIL_PASSWORD=test"
})
@AutoConfigureMockMvc
class BookingStatisticsControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanGetBookingStatisticsSummary() throws Exception {
        mockMvc.perform(get("/api/statistics/bookings/summary")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").exists())
                .andExpect(jsonPath("$.data.totalBookings").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanGetBookingStatisticsByService() throws Exception {
        mockMvc.perform(get("/api/statistics/bookings/by-service")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanGetBookingStatisticsByPeriod() throws Exception {
        mockMvc.perform(get("/api/statistics/bookings/by-period")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("groupBy", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
