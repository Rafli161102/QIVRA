package org.qivra.core;

import java.util.List;
import java.util.stream.Collectors;

public class LegacyService {
    public List<String> normalize(List<String> values) {
        return values.stream().map(String::trim).collect(Collectors.toList());
    }
}
