import java.time.format.DateTimeFormatter;

// Enum for DateTimeFormatter
public enum DateFormat {
    FORMAT1(DateTimeFormatter.ofPattern("yyyy-M-d")),
    FORMAT2(DateTimeFormatter.ofPattern("d.M.yyyy")),
    FORMAT3(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
    FORMAT4(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    private final DateTimeFormatter formatter;

    DateFormat(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public DateTimeFormatter getFormatter() {
        return formatter;
    }
}
