package de.trundicho.timeclockstamper.core.domain.model;

import java.util.List;

import de.trundicho.timeclockstamper.core.adapters.api.ClockType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@ToString
@Accessors(chain = true)
@AllArgsConstructor
public class ClockTimeData {

    private final ClockType currentState;
    private final String hoursWorkedToday;
    private final String overtimeMonth;
    private final List<ClockTime> clockTimes;
}
