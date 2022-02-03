package de.trundicho.timeclockstamper.core.adapters.api;

import java.time.LocalTime;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;
import de.trundicho.timeclockstamper.core.domain.model.ClockTimeData;
import de.trundicho.timeclockstamper.core.service.ClockTimePersistencePort;
import de.trundicho.timeclockstamper.core.service.TimeClockStamperService;

public class TimeClockStamperApiImpl implements TimeClockStamperApi {
    @Mapper
    interface ClockTimeDataMapper {

        ClockTimeDataDto dataToDto(ClockTimeData source);

        ClockTimeData dtoToData(ClockTimeDataDto source);

        ClockTimeDto clockTimeToDto(ClockTime source);

        ClockTime dtoToClockTime(ClockTimeDto source);
    }

    private final TimeClockStamperService timeClockStamperService;
    private final ClockTimeDataMapper mapper;

    public TimeClockStamperApiImpl(String timeZone, ClockTimePersistencePort clockTimePersistencePort) {
        timeClockStamperService = new TimeClockStamperService(timeZone, clockTimePersistencePort);
        mapper = Mappers.getMapper(ClockTimeDataMapper.class);
    }

    @Override
    public ClockTimeDataDto stampInOrOut() {
        return mapper.dataToDto(timeClockStamperService.stampInOrOut());
    }

    @Override
    public ClockTimeDataDto getTimeClockResponse() {
        return mapper.dataToDto(timeClockStamperService.getTimeClockResponse());
    }

    @Override
    public String getOvertimeMonth(Integer year, Integer month) {
        return timeClockStamperService.getOvertimeMonth(year, month);
    }

    @Override
    public ClockTimeDataDto stamp(LocalTime time) {
        return mapper.dataToDto(timeClockStamperService.stamp(time));
    }

    @Override
    public ClockTimeDataDto setToday(ClockTimeDataDto clockTimeDto) {
        return mapper.dataToDto(timeClockStamperService.setToday(mapper.dtoToData(clockTimeDto)));
    }

}
