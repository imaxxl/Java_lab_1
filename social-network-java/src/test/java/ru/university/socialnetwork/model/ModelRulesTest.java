package ru.university.socialnetwork.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Modifier;
import java.time.Year;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class ModelRulesTest {
    @Test
    void ordinaryProfileIsEditable() {
        assertInstanceOf(Editable.class, new EditableProfile(1, "Анна", "Город", 2000));
    }

    @Test
    void deletedProfileHasNoPublicSettersOrMutableFields() {
        assertFalse(Editable.class.isAssignableFrom(DeletedProfile.class));
        assertTrue(Modifier.isFinal(DeletedProfile.class.getModifiers()));
        assertFalse(Arrays.stream(DeletedProfile.class.getMethods()).anyMatch(m -> m.getName().startsWith("set")));
        for (Class<?> type : new Class<?>[]{Profile.class, DeletedProfile.class}) {
            assertTrue(Arrays.stream(type.getDeclaredFields()).allMatch(f -> Modifier.isFinal(f.getModifiers())));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void ownIdMustBePositive(int id) {
        assertFalse(new EditableProfile(id, "Анна", "Город", 2000).validate().isEmpty());
    }

    @Test
    void currentYearIsAllowedButNextYearIsNot() {
        int year = Year.now().getValue();
        assertTrue(new EditableProfile(1, "Анна", "Город", year).validate().isEmpty());
        assertFalse(new EditableProfile(1, "Анна", "Город", year + 1).validate().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"0,2,1", "1,0,1", "-1,2,1", "1,1,1", "1,2,0", "1,2,-1"})
    void friendshipRejectsInvalidIdsLoopsAndStrength(int first, int second, int strength) {
        assertThrows(IllegalArgumentException.class, () -> new Friendship(first, second, strength));
    }

    @Test
    void getOtherIdRejectsUnrelatedId() {
        Friendship friendship = new Friendship(1, 2, 5);
        assertEquals(2, friendship.getOtherId(1));
        assertEquals(1, friendship.getOtherId(2));
        assertThrows(IllegalArgumentException.class, () -> friendship.getOtherId(999));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void invalidStrengthDoesNotChangeExistingStrength(int strength) {
        Friendship friendship = new Friendship(1, 2, 5);
        assertThrows(IllegalArgumentException.class, () -> friendship.setStrength(strength));
        assertEquals(5, friendship.getStrength());
    }
}
