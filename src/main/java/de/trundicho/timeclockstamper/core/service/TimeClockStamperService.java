package de.trundicho.timeclockstamper.core.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.domain.model.ClockTimeData;
import de.trundicho.timeclockstamper.core.domain.model.ClockType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeClockStamperService {

    private final int hoursToWorkPerDayInMinutes;
    private final ClockTimePersistencePort clockTimePersistencePort;
    private final String timezone;

    public TimeClockStamperService(String timeZone, ClockTimePersistencePort clockTimePersistencePort) {
        this(timeZone, clockTimePersistencePort, 480);
    }

    public TimeClockStamperService(String timeZone, ClockTimePersistencePort clockTimePersistencePort,
            int hoursToWorkPerDayInMinutes) {
        this.clockTimePersistencePort = clockTimePersistencePort;
        this.timezone = timeZone;
        this.hoursToWorkPerDayInMinutes = hoursToWorkPerDayInMinutes;
    }

    public ClockTimeData stampInOrOut() {
        return stamp(clockNow());
    }

    public ClockTimeData getTimeClockResponse() {
        List<ClockTime> clockTimes = clockTimePersistencePort.read(null, null);
        return createClockTimeResponse(clockTimes, null, null);
    }

    public String getOvertimeMonth(Integer year, Integer month) {
        List<ClockTime> clockTimes = clockTimePersistencePort.read(year, month);
        return overtimeMonth(clockTimes, year, month);
    }

    private ClockTimeData createClockTimeResponse(List<ClockTime> clockTimes, Integer year, Integer month) {
        LocalDateTime localDateTime = localDate(year, month, null);
        return createClockTimeResponse(clockTimes, localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth());
    }

    private ClockTimeData createClockTimeResponse(List<ClockTime> clockTimes, Integer year, Integer month, Integer day) {
        List<ClockTime> filteredPerDay = clockTimes.stream()
                                                   .filter(c -> (year == null || c.getDate().getYear() == year) && (month == null
                                                           || c.getDate().getMonthValue() == month) && (day == null
                                                           || c.getDate().getDayOfMonth() == day))
                                                   .collect(Collectors.toList());
        List<ClockTime> filteredPerMonth = clockTimes.stream()
                                                     .filter(c -> (year == null || c.getDate().getYear() == year) && (month == null
                                                             || c.getDate().getMonthValue() == month))
                                                     .collect(Collectors.toList());
        ClockType clockType = ClockType.valueOf(currentStampState(filteredPerDay));
        String hoursWorkedToday = hoursWorkedAtDay(filteredPerDay);
        return new ClockTimeData().setCurrentState(clockType)
                                  .setHoursWorkedToday(hoursWorkedToday)
                                  .setOvertimeMonth(overtimeMonth(filteredPerMonth, year, month))
                                  .setClockTimes(filteredPerDay);
    }

    private String currentStampState(List<ClockTime> clockTimes) {
        return getCurrentClockType(clockTimes).toString();
    }

    private String hoursWorkedAtDay(List<ClockTime> clockTimesOfADay) {
        LocalDateTime today = clockNow().getDate();
        Optional<ClockTime> first = clockTimesOfADay.stream()
                                                    .filter(c -> c.getDate().getYear() == today.getYear()
                                                            && c.getDate().getMonthValue() == today.getMonthValue()
                                                            && c.getDate().getDayOfMonth() == today.getDayOfMonth())
                                                    .findFirst();
        if (first.isEmpty() && getCurrentClockType(clockTimesOfADay) == ClockType.CLOCK_IN) {
            return "Can not compute time worked this day";
        }
        List<ClockTime> day = new ArrayList<>(clockTimesOfADay);
        int overallWorkedMinutes = 0;
        if (getCurrentClockType(day) == ClockType.CLOCK_IN) {
            //add fake clockOut
            day.add(clockNow());
        }
        if (!day.isEmpty()) {
            overallWorkedMinutes = getOverallMinutes(day);
        }
        return toHoursAndMinutes(overallWorkedMinutes) + ". Left: " + toHoursAndMinutes(hoursToWorkPerDayInMinutes - overallWorkedMinutes);
    }

    private String overtimeMonth(List<ClockTime> clockTimes, Integer year, Integer month) {
        final ClockTime currentMonth;
        if (year != null && month != null) {
            LocalDateTime monthDate = LocalDateTime.of(year, month, 1, 0, 0);
            currentMonth = new ClockTime().setDate(monthDate);
        } else {
            currentMonth = clockNow();
        }
        final int monthInteger = Objects.requireNonNullElseGet(month, () -> currentMonth.getDate().getMonthValue());
        final int yearInteger = Objects.requireNonNullElseGet(year, () -> currentMonth.getDate().getYear());
        List<ClockTime> allClocksThisMonth = new ArrayList<>(clockTimes).stream()
                                                                        .filter(clockTime ->
                                                                                clockTime.getDate().getMonthValue() == monthInteger
                                                                                        && clockTime.getDate().getYear() == yearInteger)
                                                                        .collect(Collectors.toList());
        if (month == null && getCurrentClockType(clockTimes) == ClockType.CLOCK_IN) {
            //remove last stamp
            if (!allClocksThisMonth.isEmpty()) {
                allClocksThisMonth.remove(allClocksThisMonth.size() - 1);
            }
        }
        int dayOfMonth = month == null ? currentMonth.getDate().getDayOfMonth() : 31;
        int overallWorkedMinutes = 0;
        for (int i = 1; i <= dayOfMonth; i++) {
            final int dom = i;
            List<ClockTime> clocksAtDay = allClocksThisMonth.stream()
                                                            .filter(clockTime -> clockTime.getDate().getDayOfMonth() == dom)
                                                            .collect(Collectors.toList());
            if (clocksAtDay.isEmpty()) {
                overallWorkedMinutes += hoursToWorkPerDayInMinutes;
            } else {
                overallWorkedMinutes += getOverallMinutes(clocksAtDay);
            }
        }
        int minutesToWorkUntilToday = dayOfMonth * hoursToWorkPerDayInMinutes;
        return toHoursAndMinutes(overallWorkedMinutes - minutesToWorkUntilToday);
    }

    private int getOverallMinutes(List<ClockTime> todayClockTimes) {
        int allPausesOnDay = todayClockTimes.stream()
                                                .map(ClockTime::getPause)
                                                .filter(Objects::nonNull)
                                                .mapToInt(Integer::intValue)
                                                .sum();
        List<ClockTime> todayClocksReverse = todayClockTimes.stream().filter(c -> c.getPause() == null).collect(Collectors.toList());
        Collections.reverse(todayClocksReverse);
        if (todayClocksReverse.size() % 2 == 1) {
            log.error("Not correct clocked day: " + todayClocksReverse + " assuming 8 hours of work");
            return hoursToWorkPerDayInMinutes;
        }
        if (todayClocksReverse.isEmpty()) {
            log.info("Not clocked on this day, assuming 8 hours of work");
            return hoursToWorkPerDayInMinutes;
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
        int workedMinutes = overallWorkedMinutes;
        if (overallWorkedMinutes < 0) {
            workedMinutes = overallWorkedMinutes * -1;
        }
        String s = prependZero(workedMinutes / 60) + "h" + prependZero(workedMinutes % 60) + "m";
        return overallWorkedMinutes < 0 ? "-" + s : s;
    }

    private String prependZero(int currentMonth) {
        if (currentMonth < 10) {
            return "0" + currentMonth;
        }
        return "" + currentMonth;
    }

    private int toMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    private LocalDateTime localDate(Integer year, Integer month, Integer day) {
        LocalDateTime now = getLocalDateTime();
        return LocalDateTime.of(year == null ? now.getYear() : year, month == null ? now.getMonth().getValue() : month,
                day == null ? now.getDayOfMonth() : day, 0, 0);
    }

    public ClockTimeData stamp(LocalTime time) {
        ClockTime clockTime = clockNow();
        LocalDateTime date = clockTime.getDate();
        LocalDateTime of = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), time.getHour(), time.getMinute(),
                time.getSecond());
        return stamp(new ClockTime().setDate(of));
    }

    private ClockTimeData stamp(ClockTime clockTime) {
        LocalDateTime localDateTime = localDate(null, null, null);
        int year = localDateTime.getYear();
        int month = localDateTime.getMonthValue();
        List<ClockTime> clockTimeDb = new ArrayList<>(clockTimePersistencePort.read(year, month));
        clockTimeDb.add(clockTime);
        clockTimePersistencePort.write(clockTimeDb, year, month);
        return createClockTimeResponse(clockTimeDb, year, month);
    }

    private ClockTimeData stampByOverrideDay(List<ClockTime> clockTimesToSave, int year, int month, int day) {
        List<ClockTime> clockTimeDb = new ArrayList<>(clockTimePersistencePort.read(year, month));
        List<ClockTime> clockTimes = clockTimeDb.stream()
                                                .filter(c -> !(c.getDate().getYear() == year && c.getDate().getMonthValue() == month
                                                        && c.getDate().getDayOfMonth() == day))
                                                .collect(Collectors.toCollection(ArrayList::new));
        clockTimes.addAll(clockTimesToSave.stream()
                                          .filter(c -> c.getDate().getYear() == year && c.getDate().getMonthValue() == month
                                                  && c.getDate().getDayOfMonth() == day)
                                          .collect(Collectors.toList()));
        clockTimePersistencePort.write(clockTimes, year, month);
        return createClockTimeResponse(clockTimes, year, month, day);
    }

    public ClockTimeData setToday(ClockTimeData clockTimeData) {
        LocalDateTime today = LocalDateTime.now(ZoneId.of(timezone));
        return stampByOverrideDay(clockTimeData.getClockTimes(), today.getYear(), today.getMonthValue(), today.getDayOfMonth());
    }

    public ClockTimeData setDay(ClockTimeData clockTimeData, int year, int month, int day) {
        return stampByOverrideDay(clockTimeData.getClockTimes(), year, month, day);
    }

    public ClockTimeData getDay(int year, int month, int day) {
        List<ClockTime> clockTimes = clockTimePersistencePort.read(year, month);
        return createClockTimeResponse(clockTimes, year, month, day);
    }
}
