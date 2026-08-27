package org.aventyrs.core.monster;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;

/**
 * A foe whose stat block is <b>parameterized by whoever summoned it</b> — an invocação.
 *
 * <p>A summoned creature is not a fixed stat block with a fixed set of numbers. Half of a Zumbi's
 * block reads "se você possuir N ou mais Graduações em Domínio do Mana…", where <i>you</i> is the
 * Conjurador who cast the Magia. The same catalog entry therefore produces a different creature
 * depending on who called it up, and that parameter has to arrive from outside.
 *
 * <h2>The parameter is an {@code int}, not a summoner</h2>
 *
 * Every clause of this shape keys off exactly one number: the Conjurador's Graduação in Domínio
 * do Mana. So that number <i>is</i> the parameter. Modelling a summoner entity instead would mean
 * inventing a Conjurador↔invocação relationship this core has no other use for, and it would make
 * the common case impossible: a Narrador who puts a Zumbi in a scene for narrative reasons has no
 * summoner to hand, only a number they picked (or none at all).
 *
 * <p>Hence three ways in, in decreasing order of how much the caller knows:
 *
 * <pre>{@code
 * MonsterSheet summoned  = Zumbi.builder().build().spawn(necromancer, gm);  // a real Conjurador
 * MonsterSheet fromForm  = Zumbi.builder().build().spawn(7, gm);            // a number off a form
 * MonsterSheet narrative = Zumbi.builder().build().spawn(gm);               // the GM just wants one
 * }</pre>
 *
 * <p>The inherited {@link MonsterTemplate#spawn(Player)} is the narrative case and needs no
 * override — a template built without a Conjurador already reports 0, and every tier clause is
 * written to be inactive there.
 *
 * <h2>Why {@link #withConjurador} rather than a mutable field</h2>
 *
 * The tier bonuses reach into the foe's Attributes, its abilities and its authored attack bonus —
 * things {@link MonsterTemplate#spawn()} reads off the template while building the {@code
 * Character}. So the Graduação has to be known <i>before</i> spawning, not applied to the sheet
 * afterwards. Returning a re-parameterized copy keeps a catalog entry immutable and shareable:
 * two Conjuradores summoning from the same constant can't disturb each other's numbers, the same
 * independence guarantee {@code spawn()} itself gives.
 */
public interface SummonedMonsterTemplate extends MonsterTemplate {

    /**
     * The Conjurador's Graduação in Domínio do Mana, or {@code 0} when there is no Conjurador —
     * a creature the Narrador placed for narrative reasons rather than one a character summoned.
     * Zero is a real, meaningful value here, not a null stand-in: it's the untiered baseline every
     * "se você possuir N ou mais Graduações" clause measures up from.
     */
    int getConjuradorManaGraduation();

    /** A copy of this stat block as summoned by a Conjurador of the given Graduação. */
    SummonedMonsterTemplate withConjurador(int manaGraduation);

    /** Spawns this creature as summoned by a Conjurador of the given Graduação. */
    default MonsterSheet spawn(final int manaGraduation, final Player gm) {
        return withConjurador(manaGraduation).spawn(gm);
    }

    /**
     * Spawns this creature as summoned by conjurador, reading their Graduação in Domínio do Mana
     * off their own Perícias. A Conjurador untrained in it summons the untiered baseline, exactly
     * as if no Conjurador were involved — this core validates possession, never eligibility, so
     * whether they could legally have cast the Magia at all is not asked here.
     */
    default MonsterSheet spawn(final Character conjurador, final Player gm) {
        return spawn(manaGraduationOf(conjurador), gm);
    }

    /** conjurador's Graduação in Domínio do Mana, or 0 if they have none (or are {@code null}). */
    static int manaGraduationOf(final Character conjurador) {
        if (conjurador == null) {
            return 0;
        }
        CharacterSkill dominioDoMana = conjurador.getSkills().get(SkillType.DOMINIO_DO_MANA);
        return dominioDoMana == null ? 0 : dominioDoMana.getGraduation().getGraduationValue();
    }
}
