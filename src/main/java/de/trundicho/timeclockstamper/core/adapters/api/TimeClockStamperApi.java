package de.trundicho.timeclockstamper.core.adapters.api;

import java.time.LocalTime;

public interface TimeClockStamperApi {

    ClockTimeDataDto stampInOrOut();

    ClockTimeDataDto getTimeClockResponse();

    String getOvertimeMonth(Integer year, Integer month);

    ClockTimeDataDto stamp(LocalTime time);

    ClockTimeDataDto setToday(ClockTimeDataDto clockTimeDto);
}
