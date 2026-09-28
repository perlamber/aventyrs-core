package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;

/**
 * Whether a character knows, or can cast, a given Magia — the scope every Metamágico resistance
 * clause names: "Magias que você conheça" ({@code MetamagicoFeat#ARCANISTA}'s RM) and "magias que
 * você seja capaz de conjurar" (the Aptidão Mágica DM bonuses and GD reductions, Artesão de
 * Barreiras, Aptidão Mágica Dracônica's immunity).
 *
 * <p><b>Table ruling (2026-09-27): both phrasings mean the same set</b> — the Magias the character
 * has learned ({@code Character#getSpells()}) plus the ones they may mimetize ({@code
 * Character#getMimetizedSpells()}). A Magia's Efeito Alternativo counts through its base version,
 * since "um personagem que aprenda a versão base automaticamente aprende sua segunda versão".
 */
public final class SpellFamiliarity {

    private SpellFamiliarity() {
    }

    /** Whether character has learned or may mimetize spell (or the Magia it is a version of). */
    public static boolean canCast(final Character character, final Spell spell) {
        if (character == null || spell == null) {
            return false;
        }
        Spell base = spell instanceof AlternateSpellVersion alternate ? alternate.getParent() : spell;
        return character.getSpells().stream().anyMatch(known -> known == base || known == spell)
                || character.getMimetizedSpells().stream()
                        .anyMatch(mimetized -> mimetized.getSpell() == base || mimetized.getSpell() == spell);
    }
}
