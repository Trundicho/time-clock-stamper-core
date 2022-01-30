package de.trundicho.timeclockstamper.core.adapters.api;

import java.util.List;
import java.util.stream.Collectors;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.domain.model.ClockTimeData;

class ClockTimeDataMapper {

    public ClockTimeDataDto dataToDto(ClockTimeData source) {
        return new ClockTimeDataDto(source.getCurrentState(), source.getHoursWorkedToday(), source.getOvertimeMonth(),
                mapClockTimesToData(source));
    }

    public ClockTimeData dtoToData(ClockTimeDataDto source) {
        return new ClockTimeData().setCurrentState(source.getCurrentState())
                                  .setHoursWorkedToday(source.getHoursWorkedToday())
                                  .setOvertimeMonth(source.getOvertimeMonth())
                                  .setClockTimes(mapClockTimes(source));
    }

    private List<ClockTimeDto> mapClockTimesToData(ClockTimeData source) {
        return source.getClockTimes().stream().map(this::clockTimeToClockTimeDto).collect(Collectors.toList());
    }

    private List<ClockTime> mapClockTimes(ClockTimeDataDto source) {
        return source.getClockTimes().stream().map(this::clockTimeDtoToClockTime).collect(Collectors.toList());
    }

    public ClockTimeDto clockTimeToClockTimeDto(ClockTime source) {
        return new ClockTimeDto().setDate(source.getDate()).setPause(source.getPause());
    }

    public ClockTime clockTimeDtoToClockTime(ClockTimeDto source) {
        return new ClockTime().setDate(source.getDate()).setPause(source.getPause());
    }
}
