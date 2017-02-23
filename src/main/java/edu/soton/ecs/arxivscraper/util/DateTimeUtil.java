package edu.soton.ecs.arxivscraper.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtil {

    private static final DateTimeFormatter ISO_8601_DATEFORMAT = ISODateTimeFormat.dateTime().withZoneUTC();

    private static final DateTimeParser[] DATEFORMAT_STANDARD_PARSERS = {
            ISODateTimeFormat.dateTimeParser().getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser()
    };

    public static Optional<Date> tryParse(@NotNull String dateString) {
        return tryParse(dateString, DATEFORMAT_STANDARD_PARSERS);
    }

    public static Optional<Date> tryParse(@NotNull String dateString,
                                          @NotNull String dateformatString) {
        return tryParse(dateString, new String[]{dateformatString});
    }

    public static Optional<Date> tryParse(@NotNull String dateString,
                                          @NotNull String[] dateformatStrings) {
        Preconditions.checkNotNull(dateformatStrings);

        DateTimeParser[] parsers = new DateTimeParser[dateformatStrings.length];
        for (int i = 0; i < parsers.length; i++) {
            parsers[i] = DateTimeFormat.forPattern(dateformatStrings[i]).getParser();
        }
        return tryParse(dateString, parsers);
    }

    private static Optional<Date> tryParse(@NotNull String dateString,
                                           @NotNull DateTimeParser[] parsers) {
        Preconditions.checkNotNull(parsers);

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .append(null, parsers)
                .toFormatter()
                .withZoneUTC();
        return parse(dateString, formatter);
    }

    public static Optional<Date> parseISO8601(@NotNull String dateString) {
        return parse(dateString, ISO_8601_DATEFORMAT);
    }

    private static Optional<Date> parse(@NotNull String dateString,
                                        @NotNull DateTimeFormatter formatter) {
        Preconditions.checkNotNull(dateString);

        try {
            DateTime dateTime = formatter.parseDateTime(dateString);
            Date date = dateTime.toDate();
            return Optional.of(date);
        } catch (IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    public static Calendar UTCCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public static String currentDateTimeISO8601() {
        return formatISO8601(new Date());
    }

    public static String formatISO8601(@NotNull Date date) {
        Preconditions.checkNotNull(date);

        return ISO_8601_DATEFORMAT.print(new DateTime(date));
    }

    public static String format(@NotNull Date date, @NotNull String pattern) {
        Preconditions.checkNotNull(date);
        Preconditions.checkNotNull(pattern);

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(pattern).withZoneUTC();
        return dateTimeFormatter.print(new DateTime(date));
    }

}
