package org.aventyrs.core.ability;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;

/**
 * An ability the holder must actively spend Pontos de Ação/Magia/Vida to trigger, lasting a
 * fixed number of Rodadas. This is the general active-ability contract used by character-acquired
 * abilities such as {@link FocusAbility#CONCENTRACAO_PROFUNDA} and by a Talento-granted Poder
 * Vampírico (see {@code org.aventyrs.core.feat.VampiricoFeat} / {@code
 * org.aventyrs.core.feat.PoderVampiricoActiveAbility}).
 */
public interface ActiveAbility {
    String getDescription();

    /** Pontos de Ação spent to trigger this ability's activated state — 0 for an Ação Livre. */
    int getActionPointCost();

    /** Pontos de Magia spent to trigger this ability's activated state. */
    int getMagicPointCost();

    /**
     * Pontos de Determinação spent to trigger this ability's activated state — 0 for most
     * abilities. Entering a Forma is what needs it: "Transformar-se em um Draconato requer 3PA +
     * 3PD", {@code BestialFeat#METAMORFOSE_SELVAGEM}'s "gastar 2PD".
     *
     * <p>Checked and spent exactly like the PM cost, against {@code
     * DeterminationPointsService}. Unlike {@link #getHitPointCost()} there is no self-fatal
     * guard: running out of Determinação is not a way to die.
     */
    default int getDeterminationPointCost() {
        return 0;
    }

    /**
     * Pontos de Vida spent to trigger this ability's activated state — 0 for most abilities; a
     * Poder Vampírico "consome 3PV cada". {@code ActiveAbilityService#activate} refuses to let
     * the holder spend down to 0 or below.
     */
    default int getHitPointCost() {
        return 0;
    }

    /** How many Rodadas the activated state lasts once triggered. */
    int getDurationInRounds();

    /**
     * Resfriamento — how many Rodadas must pass after this ability is activated before it may be
     * activated again. Zero by default, which is every ability whose rules text states no
     * Resfriamento and means "as often as you can pay for it".
     *
     * <p>Counted from the activation, on the <b>Rodada</b> boundary ({@code
     * CombatantSheet#startNewRound()}), not the Turn one — deliberately unlike a {@link
     * TemporaryEffect}'s own countdown, which ticks at Turn <em>end</em> via {@code finishTurn}.
     * A Resfriamento measured in Rodadas that ticked at Turn end would come back early for
     * anyone acting late in the order. Same reasoning, and the same boundary, as {@code
     * CombatantSheet#scheduleTemporaryEgoPointGrant}'s "na Rodada seguinte".
     *
     * <p>Independent of {@link #getDurationInRounds()}: a Resfriamento shorter than the Duração
     * simply means the state can be refreshed before it lapses, which is what {@code
     * MetamagicoFeat}'s Barreira Mágica (Duração 2, Resfriamento 1) says outright.
     */
    default int getCooldownRounds() {
        return 0;
    }

    /**
     * A Resfriamento measured in <b>Descansos</b> rather than Rodadas — "este Efeito não poderá
     * ser reativado até que passe por um Descanso Longo" ({@code DraconicoFeat#DRACONATO},
     * {@code FeericoFeat#ANCIENTEFORME}). The tier of Descanso that clears it; {@code null},
     * the default, means no such gate.
     *
     * <p>Independent of {@link #getCooldownRounds()}, and an ability may state both — they are
     * different units, not two spellings of one thing, and {@code ActiveAbilityService#activate}
     * refuses while <em>either</em> is owing. Cleared by {@code RestService#applyRest} at that
     * tier <b>or stronger</b> ({@code RestType#isAtLeast}), so a Descanso Total also frees an
     * ability waiting on a Longo.
     */
    default org.aventyrs.core.rest.RestType getReactivationRest() {
        return null;
    }

    /**
     * The {@link org.aventyrs.core.sheet.FormType} activating this ability puts its holder into
     * — {@code null}, the default, for every ability that is a bonus rather than a shape.
     *
     * <p>When set, {@code ActiveAbilityService#activate} additionally refuses if the holder's own
     * Talentos forbid that shape ({@code CombatantSheet#canTakeForm}), enters it, and applies a
     * {@code FormEffect} that returns them to their own shape when the Duração lapses. That
     * expiry is the one place in this core where leaving a Forma is <em>not</em> a caller's call:
     * a Forma entered through a transaction with a stated Duração ends on its own, while one
     * entered through the bare {@code CombatantSheet#enterForm} mutator is the caller's to undo.
     */
    default org.aventyrs.core.sheet.FormType resolveGrantedForm(final Character character) {
        return null;
    }

    /**
     * The {@link TemporaryEffect} this ability grants once activated, computed from the
     * character's own current stats and not yet applied to any sheet. Kept for the
     * single-effect case; {@link #resolveEffects(Character)} is what {@code ActiveAbilityService}
     * actually applies.
     */
    TemporaryEffect resolveEffect(Character character);

    /**
     * Every {@link TemporaryEffect} this ability grants once activated — defaults to the one
     * {@link #resolveEffect(Character)} returns. An ability whose rules text grants more than one
     * distinct buff at once (a Poder Vampírico raising both PA and Movimento, say) overrides
     * this instead.
     */
    default List<TemporaryEffect> resolveEffects(final Character character) {
        return List.of(resolveEffect(character));
    }
}
