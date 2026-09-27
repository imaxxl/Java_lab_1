package ru.university.socialnetwork.csv;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderTest {

    @Test
    void loadsValidProfiles() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");

        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "PROFILE;1;Анна;Москва;2002;;;"
                        + System.lineSeparator()
                        + "PROFILE;2;Иван;Омск;2001;;;"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(2, result.profiles().size());
        assertTrue(result.errors().isEmpty());
        assertEquals("Анна", result.profiles().get(0).getName());
        assertEquals("Иван", result.profiles().get(1).getName());
    }

    @Test
    void loadsDifferentProfileTypes() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");

        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "PROFILE;1;Анна;Москва;2002;;;"
                        + System.lineSeparator()
                        + "DELETED;2;Пётр;Омск;1999;;;Нарушение правил"
                        + System.lineSeparator()
                        + "COMMUNITY;3;Java Club;Москва;2020;Сообщество Java;1;"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(3, result.profiles().size());
        assertTrue(result.errors().isEmpty());
        assertInstanceOf(ru.university.socialnetwork.model.Profile.class, result.profiles().get(0));
        assertInstanceOf(ru.university.socialnetwork.model.DeletedProfile.class, result.profiles().get(1));
        assertInstanceOf(ru.university.socialnetwork.model.Community.class, result.profiles().get(2));
    }

    @Test
    void skipsBrokenRows() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");

        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "PROFILE;1;Анна;Москва;2002;;;"
                        + System.lineSeparator()
                        + "PROFILE;abc;Иван;Омск;2001;;;"
                        + System.lineSeparator()
                        + "PROFILE;3;Пётр;Новосибирск;2000;;;"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(2, result.profiles().size());
        assertEquals(1, result.errors().size());
        assertEquals(CsvErrorCode.BAD_NUMBER, result.errors().get(0).getCode());
    }

    @Test
    void detectsDuplicateId() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");

        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "PROFILE;1;Анна;Москва;2002;;;"
                        + System.lineSeparator()
                        + "PROFILE;1;Иван;Омск;2001;;;"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(1, result.profiles().size());
        assertEquals(1, result.errors().size());
        assertEquals(CsvErrorCode.DUPLICATE_ID, result.errors().get(0).getCode());
    }

    @Test
    void detectsWrongFieldCount() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");

        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "PROFILE;1;Анна"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(0, result.profiles().size());
        assertEquals(1, result.errors().size());
        assertEquals(CsvErrorCode.WRONG_FIELD_COUNT, result.errors().get(0).getCode());
    }

    @Test
    void detectsUnknownType() throws Exception {
        Path file = Files.createTempFile("profiles", ".csv");
        Files.writeString(file,
                CsvLoader.header() + System.lineSeparator()
                        + "ADMIN;1;Анна;Москва;2002;;;"
        );

        CsvLoader.LoadResult result = new CsvLoader().load(file);

        assertEquals(0, result.profiles().size());
        assertEquals(1, result.errors().size());
        assertEquals(CsvErrorCode.UNKNOWN_TYPE, result.errors().get(0).getCode());
    }
}