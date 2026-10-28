package fu.de190530.reservation.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/** Keep SQL DATE values independent of the JVM's local time zone. */
@Converter
public class DateOnlyConverter implements AttributeConverter<Date, LocalDate> {
    @Override
    public LocalDate convertToDatabaseColumn(Date value) {
        return value == null ? null : Instant.ofEpochMilli(value.getTime())
                .atZone(ZoneOffset.UTC).toLocalDate();
    }

    @Override
    public Date convertToEntityAttribute(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
