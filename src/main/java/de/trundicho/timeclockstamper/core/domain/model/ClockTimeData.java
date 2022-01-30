package de.trundicho.timeclockstamper.core.domain.model;

import java.util.List;

import de.trundicho.timeclockstamper.core.adapters.api.ClockType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
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
