package de.trundicho.timeclockstamper.core.adapters.api;

import java.time.LocalTime;

import de.trundicho.timeclockstamper.core.domain.ports.ClockTimePersistencePort;
import de.trundicho.timeclockstamper.core.service.TimeClockStamperService;

public class TimeClockStamperApi {

    private final TimeClockStamperService timeClockStamperService;
    private final ClockTimeDataMapper mapper;

    public TimeClockStamperApi(String timeZone, ClockTimePersistencePort clockTimePersistencePort) {
        timeClockStamperService = new TimeClockStamperService(timeZone, clockTimePersistencePort);
        mapper = new ClockTimeDataMapper();
    }

    public ClockTimeDataDto stampInOrOut() {
        return mapper.dataToDto(timeClockStamperService.stampInOrOut());
    }

    public ClockTimeDataDto getTimeClockResponse() {
        return mapper.dataToDto(timeClockStamperService.getTimeClockResponse());
    }

    public String getOvertimeMonth(Integer year, Integer month) {
        return timeClockStamperService.getOvertimeMonth(year, month);
    }

    public ClockTimeDataDto stamp(LocalTime time) {
        return mapper.dataToDto(timeClockStamperService.stamp(time));
    }

    public ClockTimeDataDto setToday(ClockTimeDataDto clockTimeDto) {
        return mapper.dataToDto(timeClockStamperService.setToday(mapper.dtoToData(clockTimeDto)));
    }

}
