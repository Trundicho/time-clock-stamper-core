package de.trundicho.timeclockstamper.core.domain.ports;

import java.util.List;

import de.trundicho.timeclockstamper.core.domain.model.ClockTime;

public interface ClockTimePersistencePort {

    void write(List<ClockTime> clockTimes);

    List<ClockTime> read();
}
