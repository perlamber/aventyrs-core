package org.aventyrs.core.title.santo;

import lombok.NonNull;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleArchetype;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Santo — "os guerreiros divinos", the first concrete Título Aventyr modeled in this core. See
 * CLAUDE.md's "Adding a new Título" section for the general shape this class follows.
 */
public class Santo implements AventyrTitle {

    private static final String BASE_EFFECT_DESCRIPTION =
            "Nas primeiras Rodadas de cada Cena de Combate você Ignora Efeitos Críticos " +
            "Menores dos ataques de seus inimigos, esta Habilidade tem por Duração 1 Rodada " +
            "para cada Especialização e Suprema de Santo que possuir. Você também recebe " +
            "Bônus de +2 em suas Defesas, esse Bônus aumenta em +1 para cada aliado " +
            "adjacente, Especializações e Supremas que você possua.";

    private static final String PRIMARY_TITLE_BONUS_DESCRIPTION =
            "Se este for seu Título Primário, aliados adjacentes a você recebem Bônus em " +
            "Defesas iguais à metade dos Bônus que você receber em função do Efeito Base " +
            "deste Título.";

    private static final int BASE_DEFESAS_BONUS = 2;

    private final List<SantoSpecialization> specializations;
    private final List<AventyrTitleAbility> abilities;

    /**
     * {@code abilities} is typed as the shared {@link AventyrTitleAbility} interface, not
     * {@code SantoAbility} itself, so it can hold both Santo's own top-level Habilidades/
     * Supremas <b>and</b> a held specialization's gated ability constants (e.g.
     * {@link AbencoadoPelaLuzAbility}/{@link AbracadoPelaEscuridaoAbility}) in the same list —
     * exactly what a specialization's own gated abilities are documented as being granted
     * through (see CLAUDE.md's "Adding a new Título" section). Stored as a defensive mutable
     * copy, not the raw reference handed in — {@link #grantAbility} needs to append to it
     * post-construction (e.g. {@code TitleAbilityService#grantTitleAbility}), which an
     * immutable {@code List.of(...)}-style argument (the common case in tests) would reject.
     */
    public Santo(@NonNull final List<SantoSpecialization> specializations,
                 @NonNull final List<AventyrTitleAbility> abilities) {
        this.specializations = new ArrayList<>(specializations);
        this.abilities = new ArrayList<>(abilities);
    }

    /**
     * Santo is an {@link TitleArchetype#ABENCOADO} Título — "os guerreiros divinos", the same
     * wording its own rules-text header uses, and the archetype the <i>Centelha Aventyr
     * Consagrada</i> Talento gates on.
     */
    @Override
    public TitleArchetype getArchetype() {
        return TitleArchetype.ABENCOADO;
    }

    @Override
    public String getName() {
        return "Santo";
    }

    @Override
    public String getBaseEffectDescription() {
        return BASE_EFFECT_DESCRIPTION;
    }

    @Override
    public String getPrimaryTitleBonusDescription() {
        return PRIMARY_TITLE_BONUS_DESCRIPTION;
    }

    @Override
    public List<AventyrTitleSpecialization> getSpecializations() {
        return specializations.stream().map(AventyrTitleSpecialization.class::cast).toList();
    }

    @Override
    public List<AventyrTitleAbility> getAbilities() {
        return abilities;
    }

    @Override
    public void grantAbility(final AventyrTitleAbility ability) {
        abilities.add(ability);
    }

    /**
     * Santo's Especializações are its own two-constant catalog, so anything else is not one of
     * this Título's to hold — refused rather than stored, since a foreign constant would be
     * counted by {@link #getSpecializationAndSupremaCount()} and would inflate Despertar's own
     * Defesas and Duração arithmetic. This is the enforced half of each constant's own "Apenas
     * 'Santos' podem adquirir esta especialização" line.
     */
    @Override
    public void grantSpecialization(final AventyrTitleSpecialization specialization) {
        if (!(specialization instanceof SantoSpecialization santoSpecialization)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }
        specializations.add(santoSpecialization);
    }

    /**
     * Despertar's "A Duração deste Efeito é de 1+ número de Especializações e Supremas de Santo
     * que possuir em Rodadas" clause — real, tested arithmetic over
     * {@link AventyrTitle#getSpecializationAndSupremaCount()}, and the window {@code
     * AbstractCombatantSheet#ignoresMinorCriticalEffects} hands to {@link
     * SceneContext#isWithinFirstCombatRounds(int)}.
     *
     * <p><b>The floor is 1, not 0.</b> V19 reads "1+ número de …", so a freshly Desperto Santo
     * holding no Especialização still ignores Efeitos Críticos Menores for the first Rodada of
     * each Cena de Combate — the previous revision's wording ("1 Rodada para cada …") gave them
     * none at all, which is the one behavioural difference here.
     */
    @Override
    public int resolveMinorCriticalImmunityRounds() {
        return 1 + getSpecializationAndSupremaCount();
    }

    /**
     * Despertar's "Bônus de +2 em suas Defesas, esse Bônus aumenta em +1 para cada aliado
     * adjacente, Especializações e Supremas que você possua" clause — real, tested arithmetic,
     * same "build the formula ahead of the missing system" reasoning as {@link
     * #resolveMinorCriticalImmunityRounds()}: {@code sceneContext} supplies the adjacent-ally
     * count via {@link SceneContext#countAlliesWithin(Range)}, {@link
     * AventyrTitle#getSpecializationAndSupremaCount()} the rest. Summed by {@code
     * DefenseServiceImpl} at the moment a Defesa is calculated, so it tracks who is standing
     * beside the Santo with nothing to grant or revoke.
     *
     * <p><b>Continuous, not windowed.</b> The Duração sentence above belongs to the
     * crit-ignoring clause ("esta Habilidade"); "Você <i>também</i> recebe" starts a new one. A
     * Santo with no Especialização or Suprema has a 0-Rodada window, so reading the Defesas half
     * as windowed too would leave a freshly-Desperto Santo with no base effect whatsoever.
     *
     * <p>{@code null} sceneContext is treated as "no adjacent allies," same restraint every other
     * Scene-conditioned {@code resolve*}/formula method in this core already applies.
     */
    @Override
    public int resolveBaseDefesasBonus(final SceneContext sceneContext) {
        int adjacentAllyCount = sceneContext == null ? 0 : sceneContext.countAlliesWithin(Range.ADJACENTE);
        return BASE_DEFESAS_BONUS + adjacentAllyCount + getSpecializationAndSupremaCount();
    }

    /**
     * The Título-Primário clause's "Bônus em Defesas iguais à metade dos Bônus que você
     * receber em função do Efeito Base deste Título" — half of {@link
     * #resolveBaseDefesasBonus(SceneContext)}, rounded down (integer division), matching this core's
     * established half-rounds-down convention (see {@code DamageService}'s own half-damage
     * step). The per-ally amount, not a total.
     *
     * <p>Unlike {@link #resolveBaseDefesasBonus(SceneContext)} this one is <b>granted</b>, by
     * {@code Scene#refreshProjectedAuras} from an Aura {@code AbstractCombatantSheet
     * #resolveProjectedAuras} projects — see {@link
     * AventyrTitle#resolvePrimaryTitleAllyDefesasBonus} for why the direction flips here: the
     * figure is the <i>holder's</i> adjacency, which a scan running from the recipient cannot
     * see.
     */
    @Override
    public int resolvePrimaryTitleAllyDefesasBonus(final SceneContext sceneContext) {
        return resolveBaseDefesasBonus(sceneContext) / 2;
    }

    /**
     * Activates Abençoado pela Luz: activator pays its 1PD and touches target, who is healed (
     * chooseHeal) or has a Malefício removed (still inert) — see {@link
     * AbencoadoPelaLuzInteraction}. A thin delegate to {@link #activateAbility}, which checks this
     * Santo actually holds the Especialização first.
     *
     * <p>Deliberately reached through this Santo instance rather than the raw {@code
     * SantoSpecialization.ABENCOADO_PELA_LUZ} constant: the held Título is what knows which
     * Especializações/Habilidades are held, and the natural place for any future
     * Primário-conditioned clause (a caller resolves that via {@code Character#getPrimaryTitle()
     * == santoInstance} and passes it explicitly once an ability needs it).
     *
     * @throws IllegalOperationException if this Santo instance doesn't hold Abençoado pela Luz, or
     *                                    any of the activation's gates refuses
     */
    public InteractionResult activateAbencoadoPelaLuz(final CombatantSheet activator, final CombatantSheet target,
                                                      final SceneContext sceneContext, final boolean chooseHeal) {
        return activateAbility(SantoSpecialization.ABENCOADO_PELA_LUZ, TitleAbilityActivationRequest.builder()
                .activator(activator)
                .target(target)
                .sceneContext(sceneContext)
                .choice(chooseHeal ? AbencoadoPelaLuzInteraction.Branch.HEAL
                        : AbencoadoPelaLuzInteraction.Branch.REMOVE_MALEFICIO)
                .build());
    }

    /**
     * Activates Grito de Guerra Vulcano — actor pays its 3PD, and every {@link
     * org.aventyrs.core.sheet.Blessing} its rules text grants (Vantagem toward both Perícias de
     * Ataque, plus +2 Defesas) comes back via {@link InteractionResult#getBlessings()} for the
     * caller to grant to actor and their adjacent allies — see {@link
     * GritoDeGuerraVulcanoInteraction}. A thin delegate to {@link #activateAbility}.
     *
     * @throws IllegalOperationException if this Santo instance doesn't hold Grito de Guerra Vulcano,
     *                                    or any of the activation's gates refuses
     */
    public InteractionResult activateGritoDeGuerraVulcano(final CombatantSheet actor, final SceneContext sceneContext) {
        return activateAbility(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO, TitleAbilityActivationRequest.builder()
                .activator(actor)
                .sceneContext(sceneContext)
                .build());
    }

    /**
     * Activates Orgulho Elduriano — holder spends pdSpent PD (its cost is {@code
     * PDCost.variable(1)}) and a provoking Aura is registered on scene for that many Rodadas; see
     * {@link OrgulhoEldurianoInteraction}. A thin delegate to {@link #activateAbility}; the Aura is
     * read back from {@link Scene#getActiveAuras()}.
     *
     * @param holderContext the holder's current distances, to bind foes already in range — or
     *                      {@code null} to leave that to a later {@link Scene#refreshAura} call
     * @throws IllegalOperationException if this Santo instance doesn't hold Orgulho Elduriano, or
     *                                    any of the activation's gates refuses
     */
    public InteractionResult activateOrgulhoElduriano(final CombatantSheet holder, final Scene scene, final int pdSpent,
                                                      final SceneContext holderContext) {
        return activateAbility(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, TitleAbilityActivationRequest.builder()
                .activator(holder)
                .scene(scene)
                .sceneContext(holderContext)
                .determinationPoints(pdSpent)
                .build());
    }

    /**
     * Furor de Sylph's budget is spent by any attack — "seus ataques recebem" names no weapon —
     * so the caller's one {@code TitleAttackModifiers#consumeCharges} call covers it too.
     */
    @Override
    public void consumeAttackCharges(final CombatantSheet holder, final AttackSource attackSource) {
        if (holder != null) {
            holder.consumeEnhancedAttack(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH);
        }
    }

    // Despertar is wired as of 0.0.43 — all three clauses, each through its own mechanism:
    // (1) "Ignora Efeitos Críticos Menores" -> resolveMinorCriticalImmunityRounds, read by
    // AbstractCombatantSheet#ignoresMinorCriticalEffects against
    // SceneContext#isWithinFirstCombatRounds, and enforced by CriticalEffect#applicableTo — the
    // single place an Efeito Crítico is filtered out, now keyed on severity as well as type.
    // (2) The self Defesas bonus -> resolveBaseDefesasBonus, summed by DefenseServiceImpl.
    // (3) The ally half -> resolvePrimaryTitleAllyDefesasBonus, projected as a ProjectedAura and
    // granted/revoked by Scene#refreshProjectedAuras.
    // What remains is a caller's duty, not a missing mechanism: this core does no geometry, so
    // refreshProjectedAuras must be called after any movement or teleportation — the same
    // contract Scene#refreshAura and Teleportation already document.
}
