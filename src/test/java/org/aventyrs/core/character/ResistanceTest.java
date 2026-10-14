package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resistance table of {@code docs/rules/acoes-de-combate.txt} (V19 20261012), and the rule that keeps it the
 * only place a resistance's figure is written (table ruling, 2026-10-13).
 */
class ResistanceTest {

    /** A resistance {@code ModifierType} handed a bare number — {@code ItemBonus}, {@code Blessing}, a bonus helper. */
    private static final Pattern RAW_GRANT = Pattern.compile(
            "ModifierType\\.(DAMAGE_REDUCTION|MAGIC_REDUCTION|ABSOLUTE_DAMAGE_REDUCTION|DAMAGE_TAKEN_REDUCTION"
                    + "|CRITICAL_RESISTANCE|PROFANE_DAMAGE_REDUCTION|SACRED_DAMAGE_REDUCTION)\\s*,\\s*-?\\d");

    @Test
    void perInstanceFiguresMatchTheRules() {
        assertEquals(2, Resistance.RD.instance());
        assertEquals(2, Resistance.RM.instance());
        assertEquals(3, Resistance.RE.instance(), "V19 20261012 raised RE to -3");
        assertEquals(2, Resistance.RA.instance());
        assertEquals(1, Resistance.RDS.instance());
        assertEquals(2, Resistance.RC.instance());
        assertEquals(1, Resistance.RC_MAJOR_MARGIN);
        assertEquals(5, Resistance.EFFECT_CHAIN_BASE);
        assertEquals(10, Resistance.RDS.instances(10));
        assertEquals(6, Resistance.RE.instances(2));
    }

    @Test
    void noSourceGrantsARawResistanceFigure() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher matcher = RAW_GRANT.matcher(lines.get(i));
                    if (matcher.find()) {
                        offenders.add(file + ":" + (i + 1) + "  " + lines.get(i).trim());
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "write Resistance.X.instances(n) instead:\n" + String.join("\n", offenders));
    }
}
