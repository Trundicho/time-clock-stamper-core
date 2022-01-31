package de.trundicho.timeclockstamper.core.domain.model;

import java.util.List;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class ClockTimeData {

    private ClockType currentState;
    private String hoursWorkedToday;
    private String overtimeMonth;
    private List<ClockTime> clockTimes;
}
