package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_NOT_ACTIVATABLE;

/**
 * A held instance of one Título Aventyr (e.g. {@code org.aventyrs.core.title.santo.Santo}) —
 * unlike {@link org.aventyrs.core.race.Race} (one stateless concrete class per race, since
 * every holder of a given race gets identical racial abilities), this is the per-character
 * *held* instance, carrying acquisition-time state (which Especializações/Habilidades were
 * chosen) — the same "instance carries the acquisition-time choice" shape as
 * {@code org.aventyrs.core.ego.MoralHerdadaAbility}/
 * {@code org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}. "Which Título family
 * this is" is answered by which concrete class implements this interface (e.g. {@code Santo}),
 * not a separate identity enum — deferred until a second Título creates real pressure for one,
 * mirroring {@code org.aventyrs.core.ego.SorteAdvantage}'s own precedent for the same
 * restraint. See CLAUDE.md's "Adding a new Título" section for the full convention.
 *
 * <p>Whether a held instance is the holder's "Título Primário" is deliberately **not** a
 * method here — it's a fact about which of {@link org.aventyrs.core.character.Character
 * #getPrimaryTitle()}/{@code #getSecondaryTitle()}/{@code #getTertiaryTitle()} slot holds this
 * instance, not something the instance reports about itself (an earlier version had a
 * self-reported {@code isPrimaryTitle()} boolean, unenforced against holding more than one; the
 * three-slot shape on {@code Character} makes that structurally impossible instead).
 */
public interface AventyrTitle {

    /** This Título's own name, e.g. "Santo" — matches its rules-text header exactly. */
    String getName();

    /**
     * This title's named identity when a rule distinguishes it from its archetype. Most Títulos
     * have no identity consumer and remain empty until one does.
     */
    default Optional<TitleIdentity> getIdentity() {
        return Optional.empty();
    }

    /**
     * Which broad kind of Título this is — the axis a Talento's Pré-requisito names when it
     * demands not merely "1 Título Aventyr Desperto" but "1 Título Aventyr <b>Bruto</b>
     * Desperto" (see {@link TitleArchetype}, and {@code
     * org.aventyrs.core.feat.FeatRequirements#requiredTitleArchetype} for the consuming gate).
     *
     * <p>Abstract rather than defaulted: every Título belongs to exactly one archetype, so
     * there is no sensible fallback, and a new Título silently defaulting to the wrong one
     * would quietly hand its holder Talentos they should not qualify for.
     */
    TitleArchetype getArchetype();

    /**
     * The base passive granted just for holding this Título, with no activation cost of its
     * own — e.g. Santo's own "Despertar" clause. Lives here, not on a catalog
     * {@link AventyrTitleAbility} constant, because the base effect isn't itself a
     * Habilidade/Especialização — it's a property of holding the Título at all.
     */
    String getBaseEffectDescription();

    /**
     * The additional bonus this Título's base effect grants specifically when held as the
     * holder's Título Primário (see {@code Character#getPrimaryTitle()}) — e.g. Santo's own
     * "Se este for seu Título Primário, aliados adjacentes a você recebem Bônus em Defesas
     * iguais à metade dos Bônus que você receber..." clause. Kept separate from {@link
     * #getBaseEffectDescription()} rather than left concatenated onto it, since "is this Título
     * held as Primário" is a distinct fact a caller resolves separately (via {@code
     * Character#getPrimaryTitle() == title}) from "does this Título's base effect apply at
     * all" (true unconditionally, just by holding it). {@code null} when this Título's base
     * effect has no such clause — not every Título's rules text needs one.
     */
    default String getPrimaryTitleBonusDescription() {
        return null;
    }

    /**
     * Defesas this Título's Efeito Base grants its own holder right now — the mechanical half of
     * {@link #getBaseEffectDescription()}, e.g. Santo's Despertar "+2 em suas Defesas, esse Bônus
     * aumenta em +1 para cada aliado adjacente, Especializações e Supremas que você possua".
     *
     * <p>Broad {@code ModifierType.DEFESAS}-shaped: "suas Defesas" is plural, so what this returns
     * applies to DF and DM alike and {@code DefenseService} adds it once whichever is asked for.
     *
     * <p><b>Scanned, never granted.</b> {@code DefenseServiceImpl} sums this at the moment a
     * Defesa is calculated, so a bonus that varies with who is standing next to the holder is
     * correct by construction as characters move — nothing to grant, revoke or persist. Same
     * discipline {@link AventyrTitleAbility#resolveAllyAbsoluteDamageReduction} documents for RA.
     *
     * <p>{@code sceneContext} is the holder's own, and {@code null} reads as "no adjacent allies"
     * rather than as an error. Zero by default; only override on a Título whose base effect
     * actually grants Defesas.
     */
    default int resolveBaseDefesasBonus(SceneContext sceneContext) {
        return 0;
    }

    /**
     * {@link #resolveBaseDefesasBonus(SceneContext)} for a Título whose Defesas depend on its
     * holder — what they hold in hand, what they wear, their own combat-scoped state — or on
     * being held as the Título Primário. {@code DefenseServiceImpl} calls this form; it delegates
     * to the short one by default, so a Título reading only its {@code SceneContext} (Santo)
     * overrides that one and needs nothing here.
     *
     * @param holder  who holds this Título — never {@code null} from {@code DefenseServiceImpl}
     * @param sheet   the holder's live sheet, or {@code null} on a {@code Character}-only path,
     *                which reads as "no combat-scoped state"
     * @param primary whether this instance sits in {@code Character#getPrimaryTitle()} — resolved
     *                by the caller, never self-reported (see this interface's javadoc)
     */
    default int resolveBaseDefesasBonus(final SceneContext sceneContext, final Character holder,
                                        final CombatantSheet sheet, final boolean primary) {
        return resolveBaseDefesasBonus(sceneContext);
    }

    /**
     * How many steps this Título scales up weapon's Dano Base for its holder — "o Dano Base de
     * seus ataques com Armas Naturais aumentam em +1". Summed by {@code DamageBaseServiceImpl}
     * beside the Talento/Perícia/equipment/Excelência scale-ups. {@code weapon} is {@code null}
     * on the unarmed-by-Perícia path. Zero by default.
     *
     * @param primary whether this instance is the holder's Título Primário
     */
    default int resolveDamageBaseIncrease(final Character holder, final Weapon weapon, final boolean primary) {
        return 0;
    }

    /**
     * How many <i>números</i> this Título widens holder's Margem Crítica Menor by on a roll of
     * skillType made with attackSource — summed by {@code CriticalServiceImpl
     * #sumCriticalMarginIncrease} beside every other source, so it reaches an attack and a Defesa
     * roll alike. Zero by default.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final AttackSource attackSource,
                                              final CombatantSheet holder) {
        return 0;
    }

    /**
     * {@link #resolveCriticalMarginIncrease(SkillType, AttackSource, CombatantSheet)} with the
     * roll's {@code SceneContext} — what a margin conditioned on where the holder stands needs
     * (Entre as Pernas' Malícia de Valentão, read off {@code EnvironmentalState#sharingSpaceWith}).
     * {@code CriticalServiceImpl} calls this form; it delegates to the short one by default.
     */
    default int resolveCriticalMarginIncrease(final SkillType skillType, final AttackSource attackSource,
                                              final CombatantSheet holder, final SceneContext sceneContext) {
        return resolveCriticalMarginIncrease(skillType, attackSource, holder);
    }

    /**
     * Whether this Título lets its holder move <em>through</em> spaces enemies occupy — which then
     * count as Terreno Difícil. Read by {@code MovementTerrainService#stepRules}; {@code false} by
     * default. Senhor da Briga's Entre as Pernas.
     */
    default boolean passesThroughEnemySpaces() {
        return false;
    }

    /**
     * Whether holder may end a movement in the space occupant stands in. occupantIsEnemy is the
     * mover's own reading of the Scene. Read by {@code MovementTerrainService#stepRules}; {@code
     * false} by default. Senhor da Briga's Entre as Pernas (a larger enemy) and Malícia de Valentão
     * (anyone).
     */
    default boolean mayShareSpaceWith(final CombatantSheet holder, final CombatantSheet occupant,
                                      final boolean occupantIsEnemy) {
        return false;
    }

    /**
     * How many <i>números</i> this Título widens holder's Margem Crítica <b>Maior</b> by on a roll
     * of skillType made with attackSource — Campeão da Taverna's "A Margem Crítica … Maior de suas
     * Armas Naturais aumenta em +1". Summed by {@code CriticalServiceImpl
     * #sumMajorCriticalMarginIncrease}. Zero by default.
     */
    default int resolveMajorCriticalMarginIncrease(final SkillType skillType, final AttackSource attackSource,
                                                   final CombatantSheet holder) {
        return 0;
    }

    /**
     * Efeitos Críticos this Título adds to holder's critical hits with attackSource, <em>on top of</em>
     * the attack source's own — Punho Inigualável's "recebem Guilhotina como Efeito Crítico
     * Adicional". Built and applied by {@code AttackDelivery}. Empty by default.
     */
    default List<org.aventyrs.core.effect.CriticalEffectType> resolveAdditionalCriticalEffects(
            final SkillType skillType, final AttackSource attackSource, final CombatantSheet holder) {
        return List.of();
    }

    /**
     * How many <em>extra</em> times holder's attack with attackSource applies its own natural Efeito
     * Crítico — Finalização's Corrente: "se este ataque não for um Acerto Crítico este ataque aplica o
     * Efeito Crítico Menor de sua Arma Natural, se este ataque for uma Acerto Crítico o Efeito Crítico
     * será aplicado uma vez adicional". {@code AttackDelivery} applies the natural effect this many
     * more times on a critical hit, and this many times (at Menor) on any other hit. 0 by default.
     */
    default int resolveExtraNaturalCriticalEffectApplications(final CombatantSheet holder,
                                                              final AttackSource attackSource) {
        return 0;
    }

    /**
     * Efeitos Críticos Defensivos this Título adds to holder's Defesa Acertos Críticos, beside the
     * ones the worn Armadura/Escudo grant — Fantasma do Ringue's Ímpeto Defensivo, Cruz de Sangue's
     * Contra-atacante. Empty by default.
     */
    default List<org.aventyrs.core.effect.DefensiveCriticalEffectType> resolveAdditionalDefensiveCriticalEffects(
            final CombatantSheet holder) {
        return List.of();
    }

    /**
     * What this Título adds to the Dano Crítico of a critical hit with attackSource — summed by
     * {@code CriticalServiceImpl#getCriticalDamage} on top of the baseline Vantagem.
     * {@link CriticalDamage#NONE} by default.
     */
    default CriticalDamage resolveCriticalDamage(final SkillType skillType, final AttackSource attackSource,
                                                 final CriticalResult criticalResult, final CombatantSheet holder) {
        return CriticalDamage.NONE;
    }

    /**
     * A bonus this Título gives holder's attack roll against attackTarget — a Vantagem scoped to
     * what the attack is made with, whom it is made against, or a budget of attacks the holder
     * still has. Summed by {@code AbstractSkillInteraction} on its attack-target pass, so only an
     * attack with a named target sees it. Zero by default.
     */
    default int resolveAttackRollBonus(final SkillType skillType, final AttackSource attackSource,
                                       final CombatantSheet holder, final CombatantSheet attackTarget,
                                       final SceneContext sceneContext) {
        return 0;
    }

    /**
     * A flat addition this Título makes to holder's dano roll against attackTarget — "Vantagem
     * em … Danos" is a flat +2 here as everywhere. Summed into the same {@code DamageBonus} as
     * every other source, named {@code DamageContributionSource#TITLE}. Zero by default.
     */
    default int resolveDamageRollBonus(final SkillType skillType, final AttackSource attackSource,
                                       final CombatantSheet holder, final CombatantSheet attackTarget,
                                       final SceneContext sceneContext) {
        return 0;
    }

    /**
     * What this Título changes about the attack holder is about to build that this core cannot
     * apply itself — dice it never rolls, a PA price it never charges, a Defesa and a
     * damage type the caller chooses. See {@link TitleAttackModifiers#resolve}, the one query a
     * caller makes. {@link TitleAttackModifiers#NONE} by default.
     */
    default TitleAttackModifiers resolveAttackModifiers(final CombatantSheet holder, final AttackSource attackSource,
                                                        final CombatantSheet attackTarget,
                                                        final SceneContext sceneContext) {
        return TitleAttackModifiers.NONE;
    }

    /**
     * Spends whatever budget of attacks this Título's traits hold that an attack with
     * attackSource uses up — see {@link TitleAttackModifiers#consumeCharges}. Called by the
     * caller <b>after</b> the attack resolves, since the roll reads the budget. No-op by default.
     */
    default void consumeAttackCharges(final CombatantSheet holder, final AttackSource attackSource) {
    }

    /**
     * Defesas this Título's Efeito Base grants <b>each adjacent ally</b>, and only while it is held
     * as the holder's Título Primário — e.g. Santo's "aliados adjacentes a você recebem Bônus em
     * Defesas iguais à metade dos Bônus que você receber em função do Efeito Base deste Título".
     * The per-ally amount, not a total.
     *
     * <p><b>This method does not know whether it is the Primário</b>, and deliberately so: that is
     * structural, a fact about which of {@code Character}'s three slots holds this instance (see
     * this interface's own javadoc on why a self-reported {@code isPrimaryTitle()} was removed).
     * {@code AbstractCombatantSheet#resolveProjectedAuras} reads {@code
     * Character#getPrimaryTitle()} and only consults this on the Título it finds there.
     *
     * <p><b>Granted, not scanned</b> — the one place this core does the opposite of {@link
     * #resolveBaseDefesasBonus}. The figure derives from the <i>holder's</i> own adjacency, which a
     * scan running from the recipient cannot see: it would count the recipient's neighbours and
     * silently undercount the holder's. Resolved holder-side into a real Aura instead, granted and
     * revoked by {@code Scene#refreshProjectedAuras}.
     *
     * <p>{@code sceneContext} is the holder's own; {@code null} reads as "no adjacent allies".
     * Zero by default.
     */
    default int resolvePrimaryTitleAllyDefesasBonus(SceneContext sceneContext) {
        return 0;
    }

    /**
     * How many Rodadas of each Cena de Combate this Título's Efeito Base lets its holder ignore
     * Efeitos Críticos <b>Menores</b> for — Santo's Despertar "Nas primeiras Rodadas de cada Cena
     * de Combate você Ignora Efeitos Críticos Menores dos ataques de seus inimigos, esta Habilidade
     * tem por Duração 1 Rodada para cada Especialização e Suprema de Santo que possuir".
     *
     * <p>A count, not a boolean, because the window is what the rules text scales: {@code
     * AbstractCombatantSheet#ignoresMinorCriticalEffects} passes it to {@link
     * SceneContext#isWithinFirstCombatRounds(int)}, the established idiom for a "nas primeiras
     * Rodadas" clause. Zero — the default — therefore reads as "never", which is exactly right for
     * a Santo holding no Especialização or Suprema at all.
     */
    default int resolveMinorCriticalImmunityRounds() {
        return 0;
    }

    List<AventyrTitleSpecialization> getSpecializations();

    List<AventyrTitleAbility> getAbilities();

    /**
     * Adds ability to this Título's own held Habilidades/Supremas — the mutator backing an
     * XP-gated post-acquisition grant like {@code
     * org.aventyrs.core.ability.InstinctAbility#CENTELHA_SUPERIOR}'s "uma Suprema adicional"
     * (see {@code org.aventyrs.core.character.services.TitleAbilityService#grantTitleAbility}),
     * mirroring {@code Character#grantTitle}'s own plain-mutator shape now that a held
     * Título's own ability list can grow after acquisition, not just at construction.
     */
    void grantAbility(AventyrTitleAbility ability);

    /**
     * Adds specialization to this Título's own held Especializações — the mutator backing a
     * post-acquisition grant, exactly as {@link #grantAbility} does for a Habilidade/Suprema.
     *
     * <p><b>Separate from {@link #grantAbility} deliberately.</b> An
     * {@link AventyrTitleSpecialization} <i>is</i> an {@link AventyrTitleAbility} (it extends that
     * interface), so a single mutator would compile and silently do the wrong thing: the
     * Especialização would land in the Habilidade list, where {@link #getSpecializations()} would
     * never report it. That matters beyond tidiness — {@code
     * AventyrTitleAbility#getRequiredSpecializations} counts what that method returns, so a
     * misfiled Especialização would leave every "Requer 1 Especialização" Habilidade unacquirable.
     * {@code TitleAbilityService#grantTitleAbility} is what routes each kind to its own mutator.
     */
    void grantSpecialization(AventyrTitleSpecialization specialization);

    /**
     * Especializações plus Supremas held, combined — pure arithmetic over the two methods
     * above, kept as a shared default so any Título whose own rules text scales off this same
     * count (as Santo's Despertar duration does) doesn't need to re-derive the formula.
     */
    default int getSpecializationAndSupremaCount() {
        return getSpecializations().size()
                + (int) getAbilities().stream().filter(AventyrTitleAbility::isSupreme).count();
    }

    /**
     * Every {@link AventyrTitleAbility} this Título grants, combining {@link #getSpecializations()}
     * (each one an Active Ability in its own right — see that interface's own javadoc) and
     * {@link #getAbilities()} into one list — e.g. for a scanning service that needs to sum a
     * {@code resolve*} hook across every Título trait the holder has, regardless of whether
     * it's cataloged as an Especialização or a Habilidade/Suprema (see {@code
     * DamageServiceImpl}'s own Título-ability RA scan).
     */
    default List<AventyrTitleAbility> getAllAbilities() {
        return Stream.concat(
                getSpecializations().stream().map(AventyrTitleAbility.class::cast),
                getAbilities().stream()
        ).toList();
    }

    /**
     * Activates one of this Título's traits — the single entry point for any activated Habilidade,
     * Suprema or Especialização. Checks ability is actually held (among {@link #getAbilities()} or
     * {@link #getSpecializations()}), builds the {@link AbstractTitleAbilityInteraction} its
     * catalog constant names in {@code interactionClass} through that class's no-argument
     * constructor, and runs {@link AbstractTitleAbilityInteraction#activate}, which gates, pays and
     * resolves it.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code
     *         REQUIRED_TITLE_TRAIT_NOT_HELD} if ability isn't held, {@code
     *         TITLE_ABILITY_NOT_ACTIVATABLE} if it names no activation, or anything {@code
     *         activate} throws
     */
    default InteractionResult activateAbility(final AventyrTitleAbility ability,
                                              final TitleAbilityActivationRequest request) {
        if (!getAllAbilities().contains(ability)) {
            throw new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD);
        }
        Class<?> interactionClass = ability.getInteractionClass()
                .filter(AbstractTitleAbilityInteraction.class::isAssignableFrom)
                .orElseThrow(() -> new IllegalOperationException(TITLE_ABILITY_NOT_ACTIVATABLE));
        AbstractTitleAbilityInteraction interaction;
        try {
            interaction = (AbstractTitleAbilityInteraction) interactionClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + interactionClass.getName(), e);
        }
        return interaction.activate(request);
    }
}
