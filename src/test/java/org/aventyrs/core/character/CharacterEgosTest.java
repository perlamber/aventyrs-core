package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class CharacterEgosTest {

    @Test
    void getEgoReturnsTheMatchingFieldForEveryDomain() {
        CharacterEgos egos = CharacterEgos.builder()
                .autocontrole(EgoValue.builder().base(1).build())
                .recursos(EgoValue.builder().base(2).build())
                .sorte(EgoValue.builder().base(3).build())
                .iniciativa(EgoValue.builder().base(4).build())
                .build();

        assertEquals(1, egos.getEgo(EgoDomain.AUTOCONTROLE).getBase());
        assertEquals(2, egos.getEgo(EgoDomain.RECURSOS).getBase());
        assertEquals(3, egos.getEgo(EgoDomain.SORTE).getBase());
        assertEquals(4, egos.getEgo(EgoDomain.INICIATIVA).getBase());
    }

    @Test
    void withVariableBonusRaisesOnlyTheTargetedDomainsVariable() {
        CharacterEgos egos = CharacterEgos.builder().build();

        CharacterEgos updated = egos.withVariableBonus(EgoDomain.SORTE, 1);

        assertEquals(1, updated.getSorte().getVariable());
        assertEquals(2, updated.getSorte().getBase());
        assertEquals(0, updated.getAutocontrole().getVariable());
        assertEquals(0, updated.getRecursos().getVariable());
        assertEquals(0, updated.getIniciativa().getVariable());
    }

    @Test
    void withVariableBonusAddsOnTopOfAnExistingVariable() {
        CharacterEgos egos = CharacterEgos.builder()
                .sorte(EgoValue.builder().variable(2).build())
                .build();

        CharacterEgos updated = egos.withVariableBonus(EgoDomain.SORTE, 1);

        assertEquals(3, updated.getSorte().getVariable());
    }

    @Test
    void withVariableBonusLeavesTheOriginalInstanceUnchanged() {
        CharacterEgos egos = CharacterEgos.builder().build();

        CharacterEgos updated = egos.withVariableBonus(EgoDomain.SORTE, 1);

        assertNotSame(egos, updated);
        assertEquals(0, egos.getSorte().getVariable());
    }
}
