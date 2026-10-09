package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;
import java.util.OptionalInt;

/**
 * Bocarra — the Ogro's swallow-whole Característica, and the {@code OgricoFeat} tree that widens
 * it. "Antes de efetuar um ataque de mordida podem escolher aumentar o Tempo de Ação em +1PA e
 * utilizar 1PD, se o fizer recebem Vantagem neste ataque e a Corrente de Efeitos – Devorar Inteiro."
 *
 * <p><b>The flow, every step caller-driven:</b>
 * <ol>
 *   <li>{@link #declareBite} before the bite — pays the PD and reports what the attack gains,
 *   including the {@link org.aventyrs.core.effect.DevorarInteiro} chain to put on it;</li>
 *   <li>the chain swallows the target when the attack's Corrente threshold is met ({@link
 *   #swallow});</li>
 *   <li>{@link #digest} once per Rodada deals the digestion damage to everyone inside;</li>
 *   <li>{@link #recordDamageFromInside} after a swallowed character hits the captor, which frees
 *   them once they have dealt double the captor's Vigor; {@link #regurgitate} otherwise.</li>
 * </ol>
 *
 * <p>Digestion itself is measured in hours, which this core has no clock for:
 * {@link #getDigestionHours} and {@link #getRegurgitationActionPoints} report the figures.
 */
public interface DevourService {

    /** "Aumentar o Tempo de Ação em +1PA." */
    int BITE_ACTION_POINT_INCREASE = 1;

    /** "E utilizar 1PD." */
    int BITE_DETERMINATION_POINT_COST = 1;

    /**
     * What a Bocarra bite gains: the extra PA (reported, like every PA here), the Vantagem as a
     * roll bonus, Devoratriz's Roubo de Vida for that bite (0 without it), and the Corrente to put
     * on the attack's {@code DeliveredAttack#effectChains}.
     */
    record BocarraBite(int extraActionPoints, int attackRollBonus, int lifeSteal, EffectChain devorarInteiro) {
    }

    /** One swallowed character's digestion this Rodada. */
    record Digestion(CombatantSheet victim, int damage, boolean killed, int hitPointsRecovered) {
    }

    /** Whether sheet has Bocarra right now — an Ogro whose physical racial traits no Forma suppresses. */
    boolean hasBocarra(CombatantSheet sheet);

    /**
     * Pays Bocarra's 1PD and reports what the bite gains.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code BOCARRA_NOT_HELD}, or {@code
     *         NOT_ENOUGH_DETERMINATION_POINTS}
     */
    BocarraBite declareBite(CombatantSheet ogre);

    /** "Baseado no Vigor do Ogro" — Vigor, plus Dois Estômagos' +2 for this count only. */
    int getStomachCapacity(Character ogre);

    /**
     * How much of ogre's stomach victim would fill — 1 two or more Categorias below, 2 one below;
     * with Mandíbula Desarticulada 3 at the same Categoria and 4 one above. Poderoso Glutão takes 1
     * off (minimum 1). Empty for a victim too large to swallow at all.
     */
    OptionalInt getVigorOccupied(CombatantSheet ogre, CombatantSheet victim);

    /** How much of ogre's stomach its current victims fill. */
    int getVigorInUse(CombatantSheet ogre);

    /**
     * Swallows victim if it fits — small enough, room left, not already inside someone — applying a
     * {@code Devoured} and recording it on ogre. {@code true} if it did.
     */
    boolean swallow(CombatantSheet ogre, CombatantSheet victim);

    /** "Sofrem 1+Metade do Vigor do Ogro pontos de dano a cada Rodada." */
    int getDigestionDamage(Character ogre);

    /**
     * Deals each of ogre's victims their digestion damage. With Devoratriz the captor recovers its
     * Roubo de Vida (one per Título Desperto) from each damaged victim, and a victim left at 0 PV or
     * below dies outright — the captor recovering PV equal to that victim's Vigor.
     */
    List<Digestion> digest(CombatantSheet ogre);

    /** "Precisam causar danos (dobro do vigor do Ogro)" to break free. */
    int getEscapeDamage(Character ogre);

    /**
     * Records damage victim dealt ogre from inside, regurgitating them once the total reaches
     * {@link #getEscapeDamage}. {@code true} if they are free.
     */
    boolean recordDamageFromInside(CombatantSheet ogre, CombatantSheet victim, int damage);

    /** Lets victim out — lifts the {@code Devoured} and drops them from ogre's list. */
    void regurgitate(CombatantSheet ogre, CombatantSheet victim);

    /** "Digerir um personagem leva uma quantidade de horas igual ao dobro da quantidade de Vigor ocupada", halved by Poderoso Glutão. */
    int getDigestionHours(CombatantSheet ogre, CombatantSheet victim);

    /** "Regurgitar um personagem devorado ao tempo de 1PA para cada hora" — at least 1PA. */
    int getRegurgitationActionPoints(int hoursInside);

    // --- Libertar-se da Predação — any Devorado, not only Bocarra's (core 0.1.5) ---------------

    /**
     * "A GD da Perícia de Ataque é igual à Defesa, reduzido -2 níveis (até mínimo de GD Médio)" — the
     * devourer's Defesa Física as the tier it reaches, two níveis easier, never below Médio.
     */
    org.aventyrs.core.skill.DifficultyLevel getEscapeDifficulty(CombatantSheet devourer);

    /**
     * An attack from inside — "Libertar-se da Predação … requer uma rolagem de Ataque Corpo-a-Corpo.
     * Apenas Armas Naturais ou Armas leves podem ser utilizados". roll is the victim's Ataque
     * Corpo-a-Corpo, weapon what it is swung with ({@code null} for an Ataque Desarmado), judged
     * against {@link #getEscapeDifficulty}. Only the hit is decided here; the caller rolls its damage,
     * which reaches the devourer at Meio-Dano, and hands what landed to {@link #recordEscapeHit}.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NOT_HELD} when victim is not
     *         Devorado, {@code DEVOURED_WEAPON_NOT_ALLOWED} for a weapon neither natural nor light
     */
    GrappleResult escapeAttack(CombatantSheet victim, org.aventyrs.core.scene.SceneContext sceneContext,
                               org.aventyrs.core.skill.SkillRoll roll, org.aventyrs.core.item.Weapon weapon);

    /**
     * Records the damage victim's escape hit dealt its devourer: "Caso um único ataque inflija uma
     * quantidade de danos igual ao dobro do Vigor do devorador o personagem é liberto" — or, inside a
     * Bocarra, the running total its own rule keeps (table ruling, 2026-10-07). A freed victim is
     * Pronto in an adjacent space (where is the caller's) and "recebe a condição Abalado e o estado de
     * Desprevenido por 1 Rodada". {@code true} if they are free.
     */
    boolean recordEscapeHit(CombatantSheet victim, int damageDealt);
}
