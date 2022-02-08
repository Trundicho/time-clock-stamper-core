package de.trundicho.timeclockstamper.core.adapters.persistence;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.service.ClockTimePersistencePort;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilePersistence implements ClockTimePersistencePort {

    private final ObjectMapper objectMapper;
    private final String persistenceFile;
    private final String persistenceFolder;
    private final String timezone;

    public FilePersistence(String persistenceFolder, String persistenceFile, String timeZone) {
        this.persistenceFile = persistenceFile;
        this.persistenceFolder = persistenceFolder;
        this.timezone = timeZone;
        this.objectMapper = JsonMapper.builder()
                                        .addModule(new JavaTimeModule())
                                        .build();
    }

    public void write(List<ClockTime> clockTimes, Integer year, Integer month) {
        LocalDateTime localDate = localDate(year, month);
        List<ClockTime> clockTimesOfCurrentMonth = clockTimes.stream()
                                                             .filter(c -> localDate.getMonth().equals(c.getDate().getMonth())
                                                                     && localDate.getYear() == c.getDate().getYear())
                                                             .sorted()
                                                             .collect(Collectors.toList());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(createFileName(year, month)), clockTimesOfCurrentMonth);
        } catch (IOException e) {
            log.error("Can not write to file " + e.getMessage());
        }
    }

    private String createFileName(Integer year, Integer month) {
        LocalDateTime currentDate = localDate(year, month);
        int currentMonth = currentDate.getMonthValue();
        String m = currentMonth < 10 ? "0" + currentMonth : "" + currentMonth;
        int currentYear = currentDate.getYear();
        return persistenceFolder + currentYear + "-" + m + "-" + persistenceFile;
    }

    private LocalDateTime localDate(Integer year, Integer month) {
        LocalDateTime now = getLocalDateTime();
        return LocalDateTime.of(
                year == null ? now.getYear() : year,
                month == null ? now.getMonth().getValue() : month,
                1,
                0, 0);
    }

    private LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(ZoneId.of(timezone));
    }

    public List<ClockTime> read() {
        List<ClockTime> clockTimes = new ArrayList<>();
        try {
            File folder = new File(persistenceFolder);
            File[] files = folder.listFiles(pathname -> pathname.toString().endsWith(persistenceFile));
            for (File file : Objects.requireNonNull(files)) {
                clockTimes.addAll(objectMapper.readValue(file, new TypeReference<>() {

                }));
            }

        } catch (NullPointerException | IOException e) {
            log.error("Can not read from file " + e.getMessage());
        }
        return clockTimes;
    }
}
