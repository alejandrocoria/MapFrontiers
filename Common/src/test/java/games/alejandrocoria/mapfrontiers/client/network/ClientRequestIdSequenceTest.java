package games.alejandrocoria.mapfrontiers.client.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClientRequestIdSequenceTest {
    @Test
    void startsAtOneAndResetsWithTheSession() {
        ClientRequestIdSequence sequence = new ClientRequestIdSequence();

        assertEquals(1L, sequence.next());
        assertEquals(2L, sequence.next());
        sequence.reset();
        assertEquals(1L, sequence.next());
    }

    @Test
    void skipsReservedZero() {
        ClientRequestIdSequence sequence = new ClientRequestIdSequence(-1L);

        long requestId = sequence.next();

        assertEquals(1L, requestId);
        assertNotEquals(0L, requestId);
    }
}
