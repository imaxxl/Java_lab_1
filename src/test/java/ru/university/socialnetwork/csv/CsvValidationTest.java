package ru.university.socialnetwork.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.university.socialnetwork.model.EditableProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CsvValidationTest {
    @TempDir Path directory;

    static Stream<Arguments> badRows() {
        return Stream.of(
                Arguments.of("PROFILE", CsvErrorCode.WRONG_FIELD_COUNT),
                Arguments.of("PROFILE;1;Анна;Город;2000", CsvErrorCode.WRONG_FIELD_COUNT),
                Arguments.of("PROFILE;1;Анна;Город;2000;;;;extra", CsvErrorCode.WRONG_FIELD_COUNT),
                Arguments.of("DELETED;2;Пётр;Город;2000", CsvErrorCode.WRONG_FIELD_COUNT),
                Arguments.of("COMMUNITY;3;Клуб;Город;2020;desc;1", CsvErrorCode.WRONG_FIELD_COUNT),
                Arguments.of("PROFILE;abc;Анна;Город;2000;;;", CsvErrorCode.BAD_NUMBER),
                Arguments.of("PROFILE;1;Анна;Город;bad;;;", CsvErrorCode.BAD_NUMBER),
                Arguments.of("PROFILE;;Анна;Город;2000;;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("PROFILE;1;;Город;2000;;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("PROFILE;1;Анна;;2000;;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("PROFILE;1;Анна;Город;;;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("PROFILE;-1;Анна;Город;2000;;;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("PROFILE;0;Анна;Город;2000;;;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("PROFILE;1;Анна;Город;1800;;;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("PROFILE;1;Анна;Город;" + (Year.now().getValue() + 1) + ";;;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("COMMUNITY;3;Клуб;Город;2020;;1;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("COMMUNITY;3;Клуб;Город;2020;desc;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("COMMUNITY;3;Клуб;Город;2020;desc;0;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("COMMUNITY;3;Клуб;Город;2020;desc;abc;", CsvErrorCode.BAD_NUMBER),
                Arguments.of("ADMIN;1;Анна;Город;2000;;;", CsvErrorCode.UNKNOWN_TYPE),
                Arguments.of(";1;Анна;Город;2000;;;", CsvErrorCode.EMPTY_FIELD),
                Arguments.of("PROFILE;1;Анна;Город;2000;secret;;", CsvErrorCode.INVALID_VALUE),
                Arguments.of("PROFILE;1;\"Анна;Город;2000;;;", CsvErrorCode.BAD_CSV_FORMAT));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("badRows")
    void skipsBadRowAndContinuesWithCorrectErrorCode(String badRow, CsvErrorCode expected) throws Exception {
        Path file = directory.resolve("rows.csv");
        Files.writeString(file, CsvLoader.header() + "\n" + badRow
                + "\nPROFILE;100;Корректный;Томск;2001;;;\n");
        var result = new CsvLoader().load(file);
        assertEquals(1, result.profiles().size());
        assertEquals(100, result.profiles().getFirst().getId());
        assertInstanceOf(EditableProfile.class, result.profiles().getFirst());
        assertEquals(1, result.errors().size());
        assertEquals(expected, result.errors().getFirst().getCode());
        assertEquals(2, result.errors().getFirst().getLineNumber());
    }

    static Stream<String> badHeaders() {
        return Stream.of("", "not a header", "PROFILE;1;Анна;Город;2000;;;",
                "type;id;city;name;birthYear;description;administratorId;reason");
    }

    @ParameterizedTest
    @MethodSource("badHeaders")
    void rejectsMissingOrWrongHeader(String header) throws Exception {
        Path file = directory.resolve("header.csv");
        Files.writeString(file, header + "\nPROFILE;2;Иван;Омск;2000;;;\n");
        var result = new CsvLoader().load(file);
        assertTrue(result.profiles().isEmpty());
        assertEquals(CsvErrorCode.BAD_HEADER, result.errors().getFirst().getCode());
        assertEquals(1, result.errors().getFirst().getLineNumber());
    }

    @Test
    void emptyFileReportsBadHeader() throws Exception {
        Path file = directory.resolve("empty.csv");
        Files.writeString(file, "");
        assertEquals(CsvErrorCode.BAD_HEADER, new CsvLoader().load(file).errors().getFirst().getCode());
    }

    @Test
    void acceptsUtf8BomBeforeHeader() throws Exception {
        Path file = directory.resolve("bom.csv");
        Files.writeString(file, "\uFEFF" + CsvLoader.header() + "\r\nPROFILE;1;Анна;Город;2000;;;\r\n");
        var result = new CsvLoader().load(file);
        assertEquals(1, result.profiles().size());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void administratorCanAppearAfterCommunity() throws Exception {
        var result = loadRows("COMMUNITY;2;Клуб;Город;2020;Java;1;", "PROFILE;1;Анна;Город;2000;;;");
        assertEquals(2, result.profiles().size());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void skipsCommunityWithMissingAdministrator() throws Exception {
        var result = loadRows("COMMUNITY;2;Клуб;Город;2020;Java;99;", "PROFILE;1;Анна;Город;2000;;;");
        assertEquals(1, result.profiles().size());
        assertEquals(CsvErrorCode.INVALID_REFERENCE, result.errors().getFirst().getCode());
    }

    @Test
    void deletedProfileCannotBeAdministrator() throws Exception {
        var result = loadRows("DELETED;1;Архив;Город;2000;;;Причина", "COMMUNITY;2;Клуб;Город;2020;Java;1;");
        assertEquals(1, result.profiles().size());
        assertEquals(CsvErrorCode.INVALID_REFERENCE, result.errors().getFirst().getCode());
    }

    @Test
    void exampleContainsFourValidRowsAndThreeErrors() throws Exception {
        var result = new CsvLoader().load(Path.of("src/main/resources/example.csv"));
        assertEquals(4, result.profiles().size());
        assertEquals(3, result.errors().size());
        assertEquals(CsvErrorCode.WRONG_FIELD_COUNT, result.errors().getLast().getCode());
    }

    @Test
    void loadResultListsCannotBeModified() throws Exception {
        var result = loadRows("PROFILE;1;Анна;Город;2000;;;");
        assertThrows(UnsupportedOperationException.class, () -> result.profiles().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.errors().clear());
    }

    @Test
    void badReferenceDoesNotReserveIdOfFollowingValidProfile() throws Exception {
        var result = loadRows("COMMUNITY;1;Клуб;Город;2020;Java;99;", "PROFILE;1;Анна;Город;2000;;;");
        assertEquals(1, result.profiles().size());
        assertInstanceOf(EditableProfile.class, result.profiles().getFirst());
        assertEquals(CsvErrorCode.INVALID_REFERENCE, result.errors().getFirst().getCode());
    }

    @Test
    void selfAdministratorDoesNotBlockFollowingHumanWithSameId() throws Exception {
        var result = loadRows("COMMUNITY;1;Клуб;Город;2020;Java;1;", "PROFILE;1;Анна;Город;2000;;;");
        assertEquals(1, result.profiles().size());
        assertInstanceOf(EditableProfile.class, result.profiles().getFirst());
        assertEquals(CsvErrorCode.INVALID_VALUE, result.errors().getFirst().getCode());
    }

    private CsvLoader.LoadResult loadRows(String... rows) throws Exception {
        Path file = directory.resolve("rows.csv");
        Files.writeString(file, CsvLoader.header() + "\n" + String.join("\n", rows) + "\n");
        return new CsvLoader().load(file);
    }
}
