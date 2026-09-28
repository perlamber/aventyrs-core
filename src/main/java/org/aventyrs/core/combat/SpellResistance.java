package org.aventyrs.core.combat;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * What a defender's Talentos do against one particular incoming Magia — the "Magias que você
 * conheça / seja capaz de conjurar" clauses of {@code MetamagicoFeat}, asked by {@link
 * AttackReceiver} and {@link AttackDelivery} whenever the attack source is a {@link Spell}. Every
 * answer is 0/false for any other source, so the two orchestrators ask unconditionally.
 */
final class SpellResistance {

    private SpellResistance() {
    }

    /** The Magia source is, or {@code null} when it is none. */
    static Spell spellOf(final AttackSource source) {
        return source instanceof Spell spell ? spell : null;
    }

    /** Every held Talento's {@code Feat#resolveSpellDefenseBonus} against source. */
    static int defenseBonus(final CombatantSheet defender, final DefenseType defenseType, final AttackSource source) {
        Spell spell = spellOf(source);
        return spell == null ? 0 : defender.getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveSpellDefenseBonus(defenseType, spell, defender))
                .sum();
    }

    /** Every held Talento's {@code Feat#resolveSpellResistanceDifficultyReduction} against source, in níveis. */
    static int difficultyReduction(final CombatantSheet defender, final AttackSource source) {
        Spell spell = spellOf(source);
        return spell == null ? 0 : defender.getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveSpellResistanceDifficultyReduction(spell, defender))
                .sum();
    }

    /** Whether a held Talento makes defender immune to source ({@code Feat#isImmuneToSpell}). */
    static boolean immune(final CombatantSheet defender, final AttackSource source) {
        Spell spell = spellOf(source);
        return spell != null && defender.getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.isImmuneToSpell(spell, defender));
    }
}
