package com.buglens.project.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectKeyGeneratorTest {

    private final ProjectKeyGenerator generator = new ProjectKeyGenerator();

    @Test
    void generatesInitialsForMultiWordProjectName() {
        assertThat(generator.generateBaseKey("Cipher Drop")).isEqualTo("CD");
    }

    @Test
    void detectsCamelCaseProjectName() {
        assertThat(generator.generateBaseKey("CipherDrop")).isEqualTo("CD");
    }

    @Test
    void normalizesProvidedKey() {
        assertThat(generator.normalizeProvidedKey(" cd ")).isEqualTo("CD");
    }
}
