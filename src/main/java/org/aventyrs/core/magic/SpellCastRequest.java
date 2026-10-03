package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Everything one Magia invocation needs. A Combatant or position target is supplied only when
 * {@link SpellTargeting} for the version being cast permits it.
 *
 * <p><b>One thing a cast chooses that the Magia does not fix</b>: which <em>version</em> is being
 * cast, {@link #useAlternateVersion}. That single flag also settles which effect the cast uses,
 * because a Magia and its {@code Efeito Alternativo} are two separate {@link Spell}s each carrying
 * its own effect columns — so there is no second "which effect" question to ask.
 */
@Getter
@Builder
public class SpellCastRequest {
    @NonNull
    private final CombatantSheet caster;
    @NonNull
    private final Spell spell;
    @NonNull
    private final SceneContext sceneContext;
    @NonNull
    private final Scene scene;
    private final CombatantSheet combatantTarget;
    private final GridPosition positionTarget;

    /**
     * Cast {@link #spell}'s {@code Efeito Alternativo} rather than its base version — "um
     * personagem que aprenda a versão base automaticamente aprende sua segunda versão", so both
     * are always available to whoever knows the Magia.
     *
     * <p>The service resolves {@code Spell#getAlternateVersion()} itself and casts <em>that</em>,
     * so the second version's own Tempo de Ativação, Alcance and GD are what get validated and
     * reported. Refused with {@code NO_ALTERNATE_SPELL_VERSION} when the Magia has none.
     *
     * <p>Passing the alternate as {@link #spell} directly still works — it is a {@link Spell} like
     * any other — but this spares the caller the lookup.
     *
     * <p><b>It is also what says which effect the cast uses.</b> The two versions author separate
     * effect columns, so choosing the version chooses the effect; {@code
     * org.aventyrs.core.effect.SpellEffectFactory} then simply builds whatever the chosen version
     * authors, with no selector of its own.
     */
    private final boolean useAlternateVersion;

    /**
     * Whether the caster pays this Magia's PM in PV instead — Mártir Altruísta's Transferir
     * Vitalidade, refused ({@code HIT_POINT_PAYMENT_NOT_PERMITTED}) unless a held Título permits it
     * for this Magia and target. Like the PM, the PV is <b>reported</b>, not taken: {@link
     * SpellCastingResult#getVitalityCost()}, which the caller pays through {@code
     * CombatantSheet#payWithVitality}.
     */
    private final boolean payManaWithHitPoints;

    /**
     * The rung of what a rung-scaled Magia opposes (core 0.0.94) — Toque Curativo's "Efeitos mundanos ou de magias
     * Sementes: Fácil, Brotos: Médio …", Remover Maldição's. Its GD becomes {@code Spell#getCastingDifficultyAgainst};
     * a mundane affliction is a {@link BranchLevel#SEMENTE}, and "Maldições provenientes de Habilidades Monstruosas
     * ou Aventyrs" (Muito Difícil) an {@link BranchLevel#EMERGENTE}. Ignored by a Magia whose GD is not scaled;
     * {@code null} keeps the authored GD.
     */
    private final BranchLevel opposedBranchLevel;

    /**
     * Who else the cast reaches (core 0.0.94) — Benção Bifurcada's second target, Corrente Abençoada's creatures. At
     * most {@code Spell#getMaxAdditionalTargets()}; each costs {@code Spell#getAdditionalTargetManaCost()} PM. Each
     * gets the version's effect, built by the caller with {@code SpellCastingService#resolveEffect}.
     */
    @lombok.Singular
    private final java.util.List<CombatantSheet> additionalTargets;

    /**
     * The Talentos the caster opts into for this one cast — "Você pode optar por…" ({@code
     * MetamagicoFeat#CONJURACAO_RAPIDA}), "Você pode fazer com que suas magias…" ({@code
     * MetamagicoFeat#PROCRASTINAR_CONJURACAO}). The {@code SkillRoll#getActivatedFeats} twin for a
     * cast. Naming a Talento the caster does not hold does nothing: each hook checks its own
     * constant.
     */
    @Singular
    private final java.util.Set<org.aventyrs.core.feat.Feat> activatedFeats;

    /**
     * The Atributo the caster picks for a "Força ou Destreza" clause — {@code SpellBodyChange#attributeChoice()}
     * (Ogrificar) (core 0.0.99). {@code null} when the Magia asks for none, or none was picked: no Atributo moves.
     */
    private final org.aventyrs.core.character.AttributeDomain chosenAttribute;

    /**
     * Extra PM spent to lengthen the Duração — Serra-Pernas's "+2 rodadas para cada PM gasto" ({@code
     * Spell#getRoundsPerExtraMana()}) (core 0.1.0). Added to the cast's PM cost; ignored by a Magia that allows none.
     */
    private final int extraMana;

    /**
     * Whether the caster aims for the version's Corrente de Efeitos Alternativa rather than its Corrente — declared at
     * the cast (table ruling, 2026-10-02; core 0.1.0). Ignored by a version with none.
     */
    private final boolean alternateEffectChain;

    /** Whether the caster opted into feat for this cast. */
    public boolean activated(final org.aventyrs.core.feat.Feat feat) {
        return activatedFeats.contains(feat);
    }
}
