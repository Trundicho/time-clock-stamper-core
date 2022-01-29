package de.trundicho.timeclockstamper.core.adatpers.persistence;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.domain.ports.ClockTimePersistencePort;
import de.trundicho.timeclockstamper.core.domain.model.PropertiesUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilePersistence implements ClockTimePersistencePort {

    private final ObjectMapper objectMapper;
    private final String persistenceFile = PropertiesUtil.getString("persistence.file");
    private final String persistenceFolder = PropertiesUtil.getString("persistence.folder");
    private final String timezone = PropertiesUtil.getString("time.zone");

    public FilePersistence() {
        this.objectMapper = JsonMapper.builder()
                                        .addModule(new JavaTimeModule())
                                        .build();
    }

    public void write(List<ClockTime> clockTimes) {
        LocalDateTime currentDate = getCurrentDate();
        List<ClockTime> clockTimesOfCurrentMonth = clockTimes.stream()
                                                             .filter(c -> currentDate.getMonth().equals(c.getDate().getMonth())
                                                                     && currentDate.getYear() == c.getDate().getYear())
                                                             .sorted()
                                                             .collect(Collectors.toList());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(createFileName()), clockTimesOfCurrentMonth);
        } catch (IOException e) {
            log.error("Can not write to file " + e.getMessage());
        }
    }

    private String createFileName() {
        LocalDateTime currentDate = getCurrentDate();
        int currentMonth = currentDate.getMonthValue();
        String month = currentMonth < 10 ? "0" + currentMonth : "" + currentMonth;
        int currentYear = currentDate.getYear();
        return persistenceFolder + currentYear + "-" + month + "-" + persistenceFile;
    }

    private LocalDateTime getCurrentDate() {
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

        } catch (IOException e) {
            log.error("Can not read from file " + e.getMessage());
        }
        return clockTimes;
    }
}
