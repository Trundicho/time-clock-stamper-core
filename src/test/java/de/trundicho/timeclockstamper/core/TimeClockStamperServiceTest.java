package de.trundicho.timeclockstamper.core;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.trundicho.timeclockstamper.core.adapters.api.ClockTypeDto;
import de.trundicho.timeclockstamper.core.adapters.api.TimeClockStamperApiImpl;
import de.trundicho.timeclockstamper.core.adapters.persistence.FilePersistence;
import de.trundicho.timeclockstamper.core.adapters.api.ClockTimeDataDto;
import de.trundicho.timeclockstamper.core.domain.model.ClockType;
import de.trundicho.timeclockstamper.core.domain.model.ClockTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.assertj.core.api.Assertions.assertThat;

class TimeClockStamperServiceTest {

    private final String persistenceFile = PropertiesUtil.getString("persistence.file");
    TimeClockStamperApiImpl timeClockStamperService = new TimeClockStamperApiImpl(PropertiesUtil.getString("time.zone"),
            new FilePersistence(PropertiesUtil.getString("persistence.folder"),
                    PropertiesUtil.getString("persistence.file"),
                    PropertiesUtil.getString("time.zone")));
    ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @BeforeEach
    void setup() throws IOException {
        objectMapper.writeValue(new File(createFileName()), Collections.emptyList());
    }

    @Test
    void whenClockingInOrOut_thenStateChanges() {
        assertThat(timeClockStamperService.getTimeClockResponse().getCurrentState()).isEqualTo(ClockTypeDto.CLOCK_OUT);
        assertThat(timeClockStamperService.stampInOrOut().getCurrentState()).isEqualTo(ClockTypeDto.CLOCK_IN);
        assertThat(timeClockStamperService.stampInOrOut().getCurrentState()).isEqualTo(ClockTypeDto.CLOCK_OUT);
        assertThat(timeClockStamperService.stampInOrOut().getCurrentState()).isEqualTo(ClockTypeDto.CLOCK_IN);
    }

    @Test
    void whenPauseExists_thenItIsSubstracted() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        ClockTime stamp1 = createClockTime(now, 9, 0);
        ClockTime stamp2 = createClockTime(now, 17, 0);
        ClockTime stamp3 = createClockTime(now, 17, 0).setPause(30);
        objectMapper.writeValue(new File(createFileName()), List.of(stamp1, stamp2, stamp3));
        ClockTimeDataDto clockTimeDto = timeClockStamperService.getTimeClockResponse();
        assertThat(clockTimeDto.getHoursWorkedToday()).isEqualTo("7h 30m. Left to 8 hours: 0h 30m");
    }

    @Test
    void whenMoreThanTwoStamping_thenGetHoursWorkedToday() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        ClockTime stamp1 = createClockTime(now, 9, 0);
        ClockTime stamp2 = createClockTime(now, 12, 0);
        ClockTime stamp3 = createClockTime(now, 13, 0);
        ClockTime stamp4 = createClockTime(now, 17, 0);
        objectMapper.writeValue(new File(createFileName()), List.of(stamp1, stamp2, stamp3, stamp4));
        ClockTimeDataDto clockTimeDto = timeClockStamperService.getTimeClockResponse();
        assertThat(clockTimeDto.getHoursWorkedToday()).isEqualTo("7h 0m. Left to 8 hours: 1h 0m");
    }

    private ClockTime createClockTime(LocalDateTime now, int hour, int minute) {
        return new ClockTime().setDate(LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute));
    }

    private String createFileName() {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        String month = currentMonth < 10 ? "0" + currentMonth : "" + currentMonth;
        int year = now.getYear();
        return year + "-" + month + "-" + persistenceFile;
    }

}
