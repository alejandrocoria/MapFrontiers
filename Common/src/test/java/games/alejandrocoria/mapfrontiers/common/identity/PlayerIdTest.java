package games.alejandrocoria.mapfrontiers.common.identity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerIdTest {
    @Test
    void rejectsNullUuid() {
        assertThrows(NullPointerException.class, () -> new PlayerId(null));
    }

    @Test
    void equalityAndHashCodeDependOnUuid() {
        UUID uuid = UUID.fromString("22527f7f-b0ec-4a63-93b8-df8b6a33399c");
        PlayerId first = new PlayerId(uuid);
        PlayerId second = new PlayerId(uuid);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
