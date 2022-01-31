package de.trundicho.timeclockstamper.core.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.trundicho.timeclockstamper.core.domain.model.ClockType;
import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.domain.model.ClockTimeData;
import de.trundicho.timeclockstamper.core.domain.ports.ClockTimePersistencePort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeClockStamperService {

    private static final int EIGHT_HOURS_IN_MINUTES = 480;
    private final ClockTimePersistencePort clockTimePersistencePort;
    private final String timezone;

    public TimeClockStamperService(String timeZone, ClockTimePersistencePort clockTimePersistencePort) {
        this.clockTimePersistencePort = clockTimePersistencePort;
        this.timezone = timeZone;
    }

    public ClockTimeData stampInOrOut() {
        return stamp(clockNow());
    }

    public ClockTimeData getTimeClockResponse() {
        List<ClockTime> clockTimes = clockTimePersistencePort.read();
        return createClockTimeResponse(clockTimes, null, null);
    }

    public String getOvertimeMonth(Integer year, Integer month) {
        List<ClockTime> clockTimes = clockTimePersistencePort.read();
        return overtimeMonth(clockTimes, year, month);
    }

    private ClockTimeData createClockTimeResponse(List<ClockTime> clockTimes, Integer year, Integer month) {
        ClockType clockType = month == null ? ClockType.valueOf(currentStampState(clockTimes)) : null;
        String hoursWorkedToday = month == null ? hoursWorkedToday(clockTimes) : null;
        return new ClockTimeData().setCurrentState(clockType).setHoursWorkedToday(hoursWorkedToday).setOvertimeMonth(overtimeMonth(clockTimes, year,
                month)).setClockTimes(
                getClocksAndPausesOn(today()));
    }

    private String currentStampState(List<ClockTime> clockTimes) {
        return getCurrentClockType(clockTimes).toString();
    }

    private String hoursWorkedToday(List<ClockTime> clockTimes) {
        List<ClockTime> todayClockTimes = getClocksAndPausesOn(today());
        int overallWorkedMinutes = 0;
        if (getCurrentClockType(clockTimes) == ClockType.CLOCK_IN) {
            //add fake clockOut
            todayClockTimes.add(clockNow());
        }
        if (!todayClockTimes.isEmpty()) {
            overallWorkedMinutes = getOverallMinutes(todayClockTimes);
        }
        return toHoursAndMinutes(overallWorkedMinutes) + ". Left to 8 hours: " + toHoursAndMinutes(
                EIGHT_HOURS_IN_MINUTES - overallWorkedMinutes);
    }

    private String overtimeMonth(List<ClockTime> clockTimes, Integer year, Integer month) {
        final ClockTime now;
        if (year != null && month != null) {
            LocalDateTime monthDate = LocalDateTime.of(year, month, 1, 0, 0);
            now = new ClockTime().setDate(monthDate);
        } else {
            now = clockNow();
        }
        final int monthInteger = Objects.requireNonNullElseGet(month, () -> now.getDate().getMonthValue());
        List<ClockTime> allClocksThisMonth = new ArrayList<>(clockTimes).stream()
                                                                        .filter(clockTime -> clockTime.getDate().getMonthValue()
                                                                                == monthInteger)
                                                                        .collect(Collectors.toList());
        if (month == null && getCurrentClockType(clockTimes) == ClockType.CLOCK_IN) {
            //add fake clockOut
            allClocksThisMonth.add(now);
        }
        int dayOfMonth = month == null ? now.getDate().getDayOfMonth() : 31;
        int overallWorkedMinutes = 0;
        for (int i = 1; i <= dayOfMonth; i++) {
            final int dom = i;
            List<ClockTime> clocksAtDay = allClocksThisMonth.stream()
                                                            .filter(clockTime -> clockTime.getDate().getDayOfMonth() == dom)
                                                            .collect(Collectors.toList());
            if (clocksAtDay.isEmpty()) {
                overallWorkedMinutes += EIGHT_HOURS_IN_MINUTES;
            } else {
                overallWorkedMinutes += getOverallMinutes(clocksAtDay);
            }
        }
        int minutesToWorkUntilToday = dayOfMonth * EIGHT_HOURS_IN_MINUTES;
        return toHoursAndMinutes(overallWorkedMinutes - minutesToWorkUntilToday);
    }

    private int getOverallMinutes(List<ClockTime> todayClockTimes) {
        Integer allPausesOnDay = todayClockTimes.stream()
                                                .map(ClockTime::getPause)
                                                .filter(Objects::nonNull)
                                                .mapToInt(Integer::intValue)
                                                .sum();
        List<ClockTime> todayClocksReverse = todayClockTimes.stream().filter(c -> c.getPause() == null).collect(Collectors.toList());
        Collections.reverse(todayClocksReverse);
        if (todayClocksReverse.size() % 2 == 1) {
            log.error("Not correct clocked day: " + todayClocksReverse + " assuming 8 hours of work");
            return EIGHT_HOURS_IN_MINUTES;
        }
        if (todayClocksReverse.isEmpty()) {
            log.info("Not clocked on this day, assuming 8 hours of work");
            return EIGHT_HOURS_IN_MINUTES;
        }
        LocalDateTime lastClock = todayClocksReverse.get(0).getDate();
        int overallWorkedMinutes = 0;
        for (int i = 1; i < todayClocksReverse.size(); i++) {
            if (i % 2 == 0) {
                lastClock = todayClocksReverse.get(i).getDate();
                continue;
            }
            ClockTime clockTime = todayClocksReverse.get(i);
            int hour = lastClock.getHour();
            int minute = lastClock.getMinute();
            lastClock = clockTime.getDate();
            int minutes1 = toMinutes(hour, minute);
            int minutes2 = toMinutes(lastClock.getHour(), lastClock.getMinute());
            overallWorkedMinutes += minutes1 - minutes2;
        }
        return overallWorkedMinutes - allPausesOnDay;
    }

    private ClockTime clockNow() {
        return new ClockTime().setDate(getLocalDateTime());
    }

    private LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(ZoneId.of(timezone));
    }

    private ClockType getCurrentClockType(List<ClockTime> clockTimes) {
        List<ClockTime> clockTimesWithoutPause = clockTimes.stream().filter(c -> c.getPause() == null).collect(Collectors.toList());
        if (clockTimesWithoutPause.size() % 2 == 0) {
            return ClockType.CLOCK_OUT;
        }
        return ClockType.CLOCK_IN;
    }

    private String toHoursAndMinutes(int overallWorkedMinutes) {
        return (overallWorkedMinutes / 60) + "h " + overallWorkedMinutes % 60 + "m";
    }

    private int toMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    private LocalDateTime today() {
        LocalDateTime now = getLocalDateTime();
        return LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
    }

    private List<ClockTime> getClocksAndPausesOn(LocalDateTime day) {
        return clockTimePersistencePort.read().stream().filter(c -> c.getDate().isAfter(day)).collect(Collectors.toList());
    }

    public ClockTimeData stamp(LocalTime time) {
        ClockTime clockTime = clockNow();
        LocalDateTime date = clockTime.getDate();
        LocalDateTime of = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), time.getHour(), time.getMinute(),
                time.getSecond());
        return stamp(new ClockTime().setDate(of));
    }

    private ClockTimeData stamp(ClockTime clockTime) {
        List<ClockTime> clockTimeDb = new ArrayList<>(clockTimePersistencePort.read());
        clockTimeDb.add(clockTime);
        clockTimePersistencePort.write(clockTimeDb);
        return createClockTimeResponse(clockTimeDb, null, null);
    }

    private ClockTimeData stampOrOverride(List<ClockTime> clockTimesToSave, int year, int month, int day) {
        List<ClockTime> clockTimeDb = new ArrayList<>(clockTimePersistencePort.read());
        List<ClockTime> clockTimes = clockTimeDb.stream()
                                                .filter(c -> !(c.getDate().getYear() == year && c.getDate().getMonthValue() == month
                                                        && c.getDate().getDayOfMonth() == day))
                                                .collect(Collectors.toCollection(ArrayList::new));
        clockTimes.addAll(clockTimesToSave.stream()
                                          .filter(c -> c.getDate().getYear() == year && c.getDate().getMonthValue() == month
                                                  && c.getDate().getDayOfMonth() == day)
                                          .collect(Collectors.toList()));
        clockTimePersistencePort.write(clockTimes);
        return createClockTimeResponse(clockTimes, null, null);
    }

    public ClockTimeData setToday(ClockTimeData clockTimeData) {
        LocalDateTime today = LocalDateTime.now(ZoneId.of(timezone));
        return stampOrOverride(clockTimeData.getClockTimes(), today.getYear(), today.getMonthValue(), today.getDayOfMonth());
    }
}
