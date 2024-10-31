// Enum for Einzahlung_Auszahlung
public enum TransactionType {
    AUSZAHLUNG(0),
    EINZAHLUNG(1);

    private final int value;

    TransactionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TransactionType fromValue(int value) {
        for (TransactionType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
