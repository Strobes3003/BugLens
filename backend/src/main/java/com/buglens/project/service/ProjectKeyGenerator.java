package com.buglens.project.service;

import com.buglens.project.exception.InvalidProjectKeyException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class ProjectKeyGenerator {

    public String normalizeProvidedKey(String key) {
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9]{1,9}")) {
            throw new InvalidProjectKeyException(key);
        }
        return normalized;
    }

    public String generateBaseKey(String projectName) {
        String separatedName = projectName
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim();
        String[] words = separatedName.isBlank() ? new String[0] : separatedName.split("\\s+");

        String base;
        if (words.length > 1) {
            base = java.util.Arrays.stream(words)
                    .map(word -> word.substring(0, 1))
                    .reduce("", String::concat);
        } else {
            base = separatedName.replaceAll("[^A-Za-z0-9]", "");
        }

        base = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        if (base.length() == 1) {
            base += "X";
        }
        return base.substring(0, Math.min(base.length(), 10));
    }
}
