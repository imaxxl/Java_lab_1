package ru.university.socialnetwork.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommunityTest {
    @Test
    void validCommunityHasNoErrors() {
        Community community = new Community(
                1,
                "Java Club",
                "Москва",
                2020,
                "Сообщество Java",
                5
        );

        assertTrue(community.validate().isEmpty());
    }

    @Test
    void invalidCommunityReturnsErrors() {
        Community community = new Community(
                1,
                "",
                "",
                1800,
                "",
                0
        );

        List<String> errors = community.validate();

        assertEquals(5, errors.size());
    }

    @Test
    void detectsInvalidBirthYear() {
        Community community = new Community(
                1,
                "Java Club",
                "Москва",
                1800,
                "Сообщество Java",
                5
        );

        List<String> errors = community.validate();

        assertEquals(1, errors.size());
        assertEquals("Год должен быть от 1900 до 2026", errors.get(0));
    }

    @Test
    void detectsInvalidAdministratorId() {
        Community community = new Community(
                1,
                "Java Club",
                "Москва",
                2020,
                "Сообщество Java",
                0
        );

        List<String> errors = community.validate();

        assertEquals(1, errors.size());
        assertEquals("ID администратора должен быть положительным", errors.get(0));
    }

    @Test
    void communityImplementsEditable() {
        Community community = new Community(
                1,
                "Java Club",
                "Москва",
                2020,
                "Сообщество Java",
                5
        );

        assertInstanceOf(Editable.class, community);
    }

    @Test
    void deletedProfileIsReadOnly() {
        DeletedProfile profile = new DeletedProfile(
                1,
                "Пётр",
                "Омск",
                1999,
                "Нарушение правил"
        );

        assertFalse(profile instanceof Editable);
    }
}