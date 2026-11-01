package org.qivra.gradle;

public class FeatureSwitch {
    public String describe(Object value) {
        return switch (value) {
            case String text -> text.strip();
            case Integer number -> Integer.toString(number);
            default -> "unknown";
        };
    }
}
