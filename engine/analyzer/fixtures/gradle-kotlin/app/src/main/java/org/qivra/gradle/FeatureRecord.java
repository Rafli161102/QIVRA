package org.qivra.gradle;

import java.util.List;

public record FeatureRecord(String name, List<String> tags) {
    public String label() {
        return name + " [" + String.join(", ", tags) + "]";
    }
}
