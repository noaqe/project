package kg.alatoo.library;

import kg.alatoo.library.ui.ConsoleInput;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleInputTest {
    private ConsoleInput input(String text) {
        return new ConsoleInput(new Scanner(text), new PrintStream(new ByteArrayOutputStream()));
    }

    @Test void retriesInvalidNumbersAndHugeIds() {
        ConsoleInput input = input("abc\n-1\n2147483648\n2\n9999999999999999999999\n0\n7\n");
        assertEquals(2, input.number("> ", 0, 3)); assertEquals(7, input.id("> "));
    }

    @Test void retriesDuplicateAndMalformedAuthorIds() {
        assertEquals(List.of(1L, 2L), input("1,1\n1,\n0\na,b\n1, 2\n").ids("> "));
    }

    @Test void endOfInputTerminatesRatherThanLooping() {
        assertThrows(ConsoleInput.EndOfInput.class, () -> input("").number("> ", 0, 5));
    }

    @Test void deletionRequiresExactConfirmation() {
        assertFalse(input("yes\n").confirm("Delete?")); assertTrue(input("YES\n").confirm("Delete?"));
    }
}
