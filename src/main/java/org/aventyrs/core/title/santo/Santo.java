package org.aventyrs.core.title.santo;

import lombok.NonNull;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;

import java.util.ArrayList;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;

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
        this.specializations = specializations;
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
     * Despertar's "esta Habilidade tem por Duração 1 Rodada para cada Especialização e Suprema
     * de Santo que possuir" clause — real, tested arithmetic over
     * {@link AventyrTitle#getSpecializationAndSupremaCount()}, exposed ahead of a consumer the
     * same way {@code ArtesAprimorarComArteAbility#getBaseDamageBonus} was: it needs no
     * missing system of its own to compute, even though nothing can consume it yet (see the
     * TODO below for why).
     */
    public int getIgnoreCriticalEffectDurationInRounds() {
        return getSpecializationAndSupremaCount();
    }

    /**
     * Despertar's "Bônus de +2 em suas Defesas, esse Bônus aumenta em +1 para cada aliado
     * adjacente, Especializações e Supremas que você possua" clause — real, tested arithmetic,
     * same "build the formula ahead of the missing system" reasoning as {@link
     * #getIgnoreCriticalEffectDurationInRounds()}: {@code sceneContext} supplies the adjacent-ally
     * count via {@link SceneContext#countAlliesWithin(Range)}, {@link
     * AventyrTitle#getSpecializationAndSupremaCount()} the rest, and neither needs a Defesas
     * stat to exist in order to be computed — only to be *applied* (see the TODO below).
     * {@code null} sceneContext is treated as "no adjacent allies," same restraint every other
     * Scene-conditioned {@code resolve*}/formula method in this core already applies.
     */
    public int getDefesasBonus(final SceneContext sceneContext) {
        int adjacentAllyCount = sceneContext == null ? 0 : sceneContext.countAlliesWithin(Range.ADJACENTE);
        return BASE_DEFESAS_BONUS + adjacentAllyCount + getSpecializationAndSupremaCount();
    }

    /**
     * The Título-Primário clause's "Bônus em Defesas iguais à metade dos Bônus que você
     * receber em função do Efeito Base deste Título" — half of {@link
     * #getDefesasBonus(SceneContext)}, rounded down (integer division), matching this core's
     * established half-rounds-down convention (see {@code DamageService}'s own half-damage
     * step). Real arithmetic, same caveat as {@link #getDefesasBonus(SceneContext)}: computing
     * *how much* is not the missing piece — see the TODO below for what actually is. This is
     * the per-ally amount, not a total; a caller grants it to each adjacent ally individually
     * once both the Defesas stat and the granting mechanism below exist.
     */
    public int getPrimaryTitleAllyDefesasBonus(final SceneContext sceneContext) {
        return getDefesasBonus(sceneContext) / 2;
    }

    /**
     * Activates Abençoado pela Luz's own touch-heal-or-cure effect against target — the entry
     * point a caller actually invokes to trigger it, since holding the raw {@code
     * SantoSpecialization.ABENCOADO_PELA_LUZ} constant alone (via {@link #getSpecializations()})
     * gave no way to actually *do* anything with it. Validates this Santo instance actually
     * holds the Especialização first (mirroring {@code AbstractSkillInteraction
     * #validateRequestedTrait}'s own "must actually be held" check), then delegates the real
     * computation to {@link AbencoadoPelaLuzInteraction} — this instance doesn't compute the
     * effect itself.
     *
     * <p>Deliberately a method on {@code Santo}, not a static/standalone call directly against
     * {@code SantoSpecialization.ABENCOADO_PELA_LUZ}: this Título instance is what actually
     * knows which Especializações/Habilidades are held (needed by {@link #getDefesasBonus}/
     * {@link #getIgnoreCriticalEffectDurationInRounds}, and by any future ability whose own
     * effect scales the same way), and is the natural place to resolve "is this held at all"
     * before an activation proceeds — the same reasoning applies to "is this the holder's
     * Título Primário" for a *future* ability that needs it (this one doesn't — Abençoado pela
     * Luz's own touch effect has no Primário-conditioned clause — so no such parameter is
     * threaded through here; a caller resolves that fact via {@code
     * Character#getPrimaryTitle() == santoInstance} and passes it explicitly whenever an
     * ability that actually needs it is built).
     *
     * @throws IllegalOperationException if this Santo instance doesn't hold Abençoado pela Luz
     */
    public InteractionResult activateAbencoadoPelaLuz(final CombatantSheet target, final SceneContext sceneContext, final boolean chooseHeal) {
        if (!specializations.contains(SantoSpecialization.ABENCOADO_PELA_LUZ)) {
            throw new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD);
        }
        return new AbencoadoPelaLuzInteraction().applyTo(target, sceneContext, chooseHeal);
    }

    /**
     * Activates Grito de Guerra Vulcano — returns every {@link
     * org.aventyrs.core.sheet.Blessing} its own rules text grants (Vantagem toward both Perícias
     * de Ataque, plus +2 Defesas) via {@link InteractionResult#getBlessings()}; see {@link
     * GritoDeGuerraVulcanoInteraction}'s own class javadoc for why the Defesas one is still
     * inert (no consuming Defesas stat/service exists yet) despite being reported here for real.
     * This method itself doesn't apply anything — resolving actor plus adjacent allies and
     * calling {@code CombatantSheet#grantTemporaryBonus} on each is the caller's job, the same
     * "compute what, caller applies who" shape {@code ArtesCompetencyAbility#DOM_BARDICO} already
     * established. Validates this Santo instance actually holds {@link
     * AbencoadoPelaLuzAbility#GRITO_DE_GUERRA_VULCANO} first, same "must actually be held" shape
     * as {@link #activateAbencoadoPelaLuz}.
     *
     * @throws IllegalOperationException if this Santo instance doesn't hold Grito de Guerra Vulcano
     */
    public InteractionResult activateGritoDeGuerraVulcano(final CombatantSheet actor, final SceneContext sceneContext) {
        if (!abilities.contains(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO)) {
            throw new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD);
        }
        return new GritoDeGuerraVulcanoInteraction().applyTo(actor, sceneContext);
    }

    // TODO: three genuinely separate gaps stand between the real formulas above and Despertar
    // actually doing anything:
    // (1) "Ignora Efeitos Críticos Menores" itself is unimplemented — this core's
    // CriticalResult/CriticalEffect machinery has no veto/downgrade point at all; every
    // CriticalEffect is constructed only after a caller has already decided a crit landed.
    // (2) The Defesas stat itself now EXISTS (character.services/DefenseService, reading
    // ModifierType.DEFESAS/PHYSICAL_DEFENSE/MAGIC_DEFENSE), so this is no longer what blocks the
    // self bonus — what's missing is narrower: DefenseService scans abilities, equipment and the
    // sheet's TemporaryBonus pool, but not a Título's own hook, and getDefesasBonus is a method
    // on this concrete class rather than on AventyrTitleAbility (unlike the RA hook DamageService
    // does scan). Wiring it means either promoting it to the interface or granting its result as
    // a DEFESAS-typed TemporaryBonus at the moment Despertar activates.
    // (3) The ally-shared half additionally needs this core's still-missing
    // "continuously-recomputed cross-character passive grant" mechanism (see
    // SantoAbility#BASTIAO_DOS_NECESSITADOS's own TODO for the identical gap) — even once
    // Defesas exists, something still has to decide *when* to grant each adjacent ally their
    // share and keep it current as allies move in and out of range.
}
