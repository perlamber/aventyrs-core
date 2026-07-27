package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DlcRulesetTest {

    @Test
    void hasOnlyTheBaseRuleset() {
        assertEquals(1, DlcRuleset.values().length);
        assertEquals(DlcRuleset.BASE, DlcRuleset.valueOf("BASE"));
    }
}
