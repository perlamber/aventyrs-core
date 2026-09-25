package org.aventyrs.core.title.curandeiro;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.SpellReach;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleAbilityActivationRequest;
import org.aventyrs.core.title.TitleArchetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Curandeiro — "Devotos fiéis dos Deuses, Curandeiros são aqueles que devotam sua vida em ajudar e
 * proteger os vulneráveis". Every trait of the Título is real; this class carries the passive ones
 * as {@link AventyrTitle} hooks, and each activated one has its own Interaction.
 *
 * <p>Every heal-shaped hook here is asked of the <b>healer's</b> (caster's, activator's) Títulos, so
 * "Suas Magias" needs no check that the healer is this Título's holder: a scan over someone else's
 * Títulos never reaches this instance.
 *
 * <p>See {@code docs/curandeiro.md} for what a caller must pass and do.
 */
public class Curandeiro implements AventyrTitle {

    private static final String BASE_EFFECT_DESCRIPTION =
            "Você recebe Vantagem nas rolagens de 'Medicina e Cura'. Você pode realizar uma rolagem de " +
            "'Medicina e Cura', com GD Difícil, em um alvo ferido, se for bem-sucedido o alvo recupera PV como " +
            "se passasse por um Descanso Curto, se Ativada fora de combate esta Habilidade pode receber a " +
            "Corrente de Efeitos – Beijo de Boros: o Alvo recupera PV como se passasse por um Descanso Longo. " +
            "Você não pode afetar um mesmo personagem desta forma até que você passe por um Descanso Longo, " +
            "mesmo que você não tenha sido bem-sucedido em sua primeira tentativa.";

    private static final String PRIMARY_TITLE_BONUS_DESCRIPTION =
            "Se Curandeiro for seu Título Primário você pode utilizar Medicina e Cura em substituição à Domínio " +
            "do Mana para quaisquer efeitos (incluindo Magias, Habilidades de Títulos e Talentos).";

    /** Médico de Guerra: "Os efeitos de recuperação de PV aumentam em +2". */
    static final int MEDICO_DE_GUERRA_HEALING_BONUS = 2;

    /** Mártir Altruísta: "reduzido em -1 Nível". */
    static final int MARTIR_ALTRUISTA_DIFFICULTY_REDUCTION = 1;

    /** Doutor de Eldur: "reduzidos permanentemente em -1PA". */
    static final int DOUTOR_DE_ELDUR_ACTION_POINT_REDUCTION = 1;

    /** Benção de Boros: "custam o dobro de PM e PD". */
    static final int BENCAO_DE_BOROS_COST_MULTIPLIER = 2;

    /** Transferir Rancor: "-1 Nível", "+1d6", "duram apenas 1 Rodada". */
    static final int RANCOR_DIFFICULTY_REDUCTION = 1;
    static final int RANCOR_EXTRA_DICE = 1;
    static final int RANCOR_ROUNDS = 1;

    private final List<CurandeiroSpecialization> specializations;
    private final List<AventyrTitleAbility> abilities;

    /**
     * Both lists are copied into mutable ones, so {@link #grantAbility}/{@link #grantSpecialization}
     * can append to them whatever the caller passed.
     */
    public Curandeiro(@NonNull final List<CurandeiroSpecialization> specializations,
                      @NonNull final List<AventyrTitleAbility> abilities) {
        this.specializations = new ArrayList<>(specializations);
        this.abilities = new ArrayList<>(abilities);
    }

    /** The Curandeiro sheet's Character holds, if any. */
    public static Optional<Curandeiro> heldBy(@NonNull final CombatantSheet sheet) {
        return sheet.getCharacter().getAllTitles().stream()
                .filter(Curandeiro.class::isInstance)
                .map(Curandeiro.class::cast)
                .findFirst();
    }

    /** "Centelha Abençoada" — the header Curandeiro is listed under. */
    @Override
    public TitleArchetype getArchetype() {
        return TitleArchetype.ABENCOADO;
    }

    @Override
    public String getName() {
        return "Curandeiro";
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

    /** Every trait held, the Despertar's touch first — so it can be activated like any other. */
    @Override
    public List<AventyrTitleAbility> getAllAbilities() {
        return Stream.concat(Stream.of(CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO),
                AventyrTitle.super.getAllAbilities().stream()).toList();
    }

    @Override
    public void grantAbility(final AventyrTitleAbility ability) {
        abilities.add(ability);
    }

    /**
     * Refuses anything but this Título's own two Especializações — the enforced half of "apenas
     * 'Curandeiros' podem adquirir esta especialização".
     */
    @Override
    public void grantSpecialization(final AventyrTitleSpecialization specialization) {
        if (!(specialization instanceof CurandeiroSpecialization own)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }
        specializations.add(own);
    }

    /** Whether this Título holds trait — an Especialização, Habilidade or Suprema. */
    public boolean holds(final AventyrTitleAbility trait) {
        return getAllAbilities().contains(trait);
    }

    // --- Despertar and Domínio da Cura -------------------------------------------------------------

    /** Despertar: "Você recebe Vantagem nas rolagens de 'Medicina e Cura'" — the flat +2 a Vantagem is. */
    @Override
    public int resolveSkillRollBonus(@NonNull final SkillType skillType, final CombatantSheet holder) {
        return skillType == SkillType.MEDICINA_E_CURA ? Skill.ADVANTAGE_BONUS : 0;
    }

    /**
     * Domínio da Cura: "Se Curandeiro for seu Título Primário você pode utilizar Medicina e Cura em
     * substituição à Domínio do Mana para quaisquer efeitos" — {@code Character#getEffectiveSkill}
     * takes whichever of the two has the higher Graduação (table ruling, 2026-09-25).
     */
    @Override
    public Optional<SkillType> resolveSkillSubstitute(@NonNull final SkillType skillType, final boolean primary) {
        return primary && skillType == SkillType.DOMINIO_DO_MANA
                ? Optional.of(SkillType.MEDICINA_E_CURA)
                : Optional.empty();
    }

    // --- Healing the fallen ------------------------------------------------------------------------

    /**
     * Levantar os Caídos: "Suas Magias e Habilidades de Curandeiro que permitam a recuperação de PV
     * ignoram as Reduções de Cura do estado de Coma, mas apenas quando adquiridos na mesma Cena" —
     * any Magia of the holder's, or a Habilidade de Curandeiro, on a target whose Coma began in the
     * current Cena.
     */
    @Override
    public boolean bypassesComaHealingCap(@NonNull final HealingSource source, @NonNull final CombatantSheet target) {
        return grantsComaHealingBypass(source) && target.hasEnteredComaThisScene();
    }

    /** Levantar os Caídos' healer half — held, and the heal is a Magia or a Habilidade de Curandeiro. */
    @Override
    public boolean grantsComaHealingBypass(@NonNull final HealingSource source) {
        return holds(CurandeiroAbility.LEVANTAR_OS_CAIDOS)
                && (source.spell() != null || isCurandeiroTrait(source.titleAbility()));
    }

    /**
     * Curar os Mortos: a Magia Divina or Natural, or a Habilidade de Curandeiro, reaches a target dead
     * for at most {@link #resolveRevivalWindowRounds} Rodadas — spending one of the healer's charges
     * ({@link CurarOsMortosInteraction}). Refuses, spending nothing, when any of that is missing.
     */
    @Override
    public boolean claimRevival(@NonNull final HealingSource source, @NonNull final CombatantSheet target) {
        if (!revivalQualifies(source)) {
            return false;
        }
        OptionalInt roundsSinceDeath = target.getRoundsSinceDeath();
        if (roundsSinceDeath.isEmpty()
                || roundsSinceDeath.getAsInt() > resolveRevivalWindowRounds(source.healer().getCharacter())) {
            return false;
        }
        return source.healer().consumeCharge(CurandeiroAbility.CURAR_OS_MORTOS);
    }

    /**
     * Curar os Mortos' healer half, for a dead target on another client: spends a charge now and
     * reports the window the target's own sheet then judges ({@code RelayedHealer}).
     */
    @Override
    public OptionalInt claimRevivalWindow(@NonNull final HealingSource source) {
        if (!revivalQualifies(source) || !source.healer().consumeCharge(CurandeiroAbility.CURAR_OS_MORTOS)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(resolveRevivalWindowRounds(source.healer().getCharacter()));
    }

    /** Held, a healer named, and the heal a Magia Divina/Natural or a Habilidade de Curandeiro. */
    private boolean revivalQualifies(final HealingSource source) {
        return holds(CurandeiroAbility.CURAR_OS_MORTOS) && source.healer() != null
                && (source.isSpellOfType(MagicType.DIVINA, MagicType.NATURAL) || isCurandeiroTrait(source.titleAbility()));
    }

    /**
     * "personagens que tenham morrido em até uma quantidade de Rodadas igual à metade de suas
     * Graduações em Medicina e Cura" — the holder's, floored. Benção de Boros' window is the same figure.
     */
    public int resolveRevivalWindowRounds(@NonNull final Character holder) {
        CharacterSkill medicina = holder.getSkills() == null ? null : holder.getSkills().get(SkillType.MEDICINA_E_CURA);
        return medicina == null ? 0 : medicina.getGraduation().getGraduationValue() / 2;
    }

    // --- Médico de Guerra ----------------------------------------------------------------------------

    /** Médico de Guerra: +2 on every heal the holder makes. */
    @Override
    public int resolveHealingBonus(@NonNull final HealingSource source, final CombatantSheet target) {
        return holds(CurandeiroSpecialization.MEDICO_DE_GUERRA) ? MEDICO_DE_GUERRA_HEALING_BONUS : 0;
    }

    /** Doutor de Eldur: "seus Descansos contam como uma Categoria superior". */
    @Override
    public boolean upgradesRests() {
        return holds(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR);
    }

    // --- Mártir Altruísta ----------------------------------------------------------------------------

    /** Mártir Altruísta: -1 Nível on the GD of a Magia that lets others recover PV. */
    @Override
    public int resolveCastingDifficultyReduction(@NonNull final Spell spell, final CombatantSheet caster) {
        return holds(CurandeiroSpecialization.MARTIR_ALTRUISTA) && healsOthers(spell)
                ? MARTIR_ALTRUISTA_DIFFICULTY_REDUCTION
                : 0;
    }

    /**
     * Transferir Vitalidade, for a Habilidade: a Habilidade de Curandeiro aimed at another character
     * ("apenas quando os alvos forem personagens aliados" — the activator is no ally of theirs).
     */
    @Override
    public boolean permitsHitPointPayment(@NonNull final AventyrTitleAbility ability,
                                          @NonNull final CombatantSheet activator, final CombatantSheet target) {
        return holds(MartirAltruistaAbility.TRANSFERIR_VITALIDADE) && isCurandeiroTrait(ability)
                && target != null && target != activator;
    }

    /**
     * Transferir Vitalidade, for a Magia: a single-target Magia Divina or Natural cast on another
     * character. That the target is no enemy is checked by {@code SpellCastingService} itself.
     */
    @Override
    public boolean permitsHitPointPayment(@NonNull final Spell spell, @NonNull final CombatantSheet caster,
                                          final CombatantSheet target) {
        return holds(MartirAltruistaAbility.TRANSFERIR_VITALIDADE)
                && (spell.getTree().hasMagicType(MagicType.DIVINA) || spell.getTree().hasMagicType(MagicType.NATURAL))
                && reaches(spell).anyMatch(reach -> reach == SpellReach.TOQUE || reach == SpellReach.DISTANCIA)
                && target != null && target != caster;
    }

    /**
     * Transferir Rancor: each hit the holder takes from an enemy gives every ally -1 Nível on Perícia de
     * Ataque and Domínio do Mana rolls and +1d6 dano for 1 Rodada — sourceless, so they stack. "Não é
     * ativada em Cenas de Combate em que você causou Danos a outros personagens".
     */
    @Override
    public List<Blessing> resolveDamageTakenAllyBlessings(@NonNull final CombatantSheet holder, final int finalDamage,
                                                         final CombatantSheet source,
                                                         final SceneContext sceneContext) {
        if (!holds(MartirAltruistaAbility.TRANSFERIR_RANCOR) || finalDamage <= 0 || source == null) {
            return List.of();
        }
        if (sceneContext != null && sceneContext.isCombatScene() && holder.hasDealtDamageThisScene()) {
            return List.of();
        }
        return List.of(
                new Blessing(ModifierType.ATTACK_AND_CONJURATION_DIFFICULTY_REDUCTION, RANCOR_DIFFICULTY_REDUCTION,
                        RANCOR_ROUNDS, TargetScope.ALLIES, null),
                new Blessing(ModifierType.EXTRA_DAMAGE_DICE, RANCOR_EXTRA_DICE, RANCOR_ROUNDS, TargetScope.ALLIES, null));
    }

    // --- Action-point discounts (Curandeiro Veloz, Doutor de Eldur) ----------------------------------

    /**
     * -2PA from a banked Curandeiro Veloz charge on a healing Magia Natural/Divina, and Doutor de Eldur's
     * -1PA on any Magia that heals.
     */
    @Override
    public int resolveCastingActionPointReduction(@NonNull final Spell spell, @NonNull final CombatantSheet caster) {
        int reduction = 0;
        if (velozAppliesTo(spell) && caster.getCharges(CurandeiroAbility.CURANDEIRO_VELOZ) > 0) {
            reduction += CurandeiroVelozInteraction.ACTION_POINT_REDUCTION;
        }
        if (holds(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR) && spell.getHealing().isPresent()) {
            reduction += DOUTOR_DE_ELDUR_ACTION_POINT_REDUCTION;
        }
        return reduction;
    }

    @Override
    public void consumeCastingCharges(@NonNull final Spell spell, @NonNull final CombatantSheet caster) {
        if (velozAppliesTo(spell)) {
            caster.consumeCharge(CurandeiroAbility.CURANDEIRO_VELOZ);
        }
    }

    /**
     * -2PA from a Curandeiro Veloz charge on a Habilidade de Curandeiro that heals others, and Doutor de
     * Eldur's -1PA on every Médico de Guerra activation.
     */
    @Override
    public int resolveActivationActionPointReduction(@NonNull final AventyrTitleAbility ability,
                                                     @NonNull final CombatantSheet activator) {
        int reduction = 0;
        if (healsOthers(ability) && activator.getCharges(CurandeiroAbility.CURANDEIRO_VELOZ) > 0) {
            reduction += CurandeiroVelozInteraction.ACTION_POINT_REDUCTION;
        }
        if (holds(MedicoDeGuerraAbility.DOUTOR_DE_ELDUR) && ability instanceof MedicoDeGuerraAbility) {
            reduction += DOUTOR_DE_ELDUR_ACTION_POINT_REDUCTION;
        }
        return reduction;
    }

    @Override
    public void consumeActivationCharges(@NonNull final AventyrTitleAbility ability,
                                         @NonNull final CombatantSheet activator) {
        if (healsOthers(ability)) {
            activator.consumeCharge(CurandeiroAbility.CURANDEIRO_VELOZ);
        }
    }

    // --- Benção de Boros -----------------------------------------------------------------------------

    /** Benção de Boros: a Habilidade de Curandeiro costs double PD while its holder is fallen, within the window. */
    @Override
    public int resolveDeterminationCostMultiplier(@NonNull final AventyrTitleAbility ability,
                                                  @NonNull final CombatantSheet activator) {
        return isCurandeiroTrait(ability) && inBorosWindow(activator) ? BENCAO_DE_BOROS_COST_MULTIPLIER : 1;
    }

    /** Benção de Boros: every Magia costs double PM while its holder is fallen, within the window. */
    @Override
    public int resolveManaCostMultiplier(@NonNull final Spell spell, @NonNull final CombatantSheet caster) {
        return inBorosWindow(caster) ? BENCAO_DE_BOROS_COST_MULTIPLIER : 1;
    }

    /**
     * "mesmo que você esteja inconsciente ou morto … Este efeito pode ser utilizado por uma quantidade
     * de Rodadas igual à metade das suas Graduações em Medicina e Cura" — PV at 0 or below, counted
     * from when they got there.
     */
    private boolean inBorosWindow(final CombatantSheet holder) {
        if (!holds(CurandeiroAbility.BENCAO_DE_BOROS)) {
            return false;
        }
        OptionalInt rounds = holder.getRoundsSinceFallen();
        return rounds.isPresent() && rounds.getAsInt() <= resolveRevivalWindowRounds(holder.getCharacter());
    }

    // --- Classification ------------------------------------------------------------------------------

    /** Activates Curar os Mortos — a one-line delegate to {@link #activateAbility}. */
    public InteractionResult activateCurarOsMortos(@NonNull final TitleAbilityActivationRequest request) {
        return activateAbility(CurandeiroAbility.CURAR_OS_MORTOS, request);
    }

    /** Whether ability is one of Curandeiro's own traits — "Habilidades de Curandeiro". */
    static boolean isCurandeiroTrait(final AventyrTitleAbility ability) {
        return ability instanceof CurandeiroAbility || ability instanceof CurandeiroSpecialization
                || ability instanceof CurandeiroDespertar
                || ability instanceof MedicoDeGuerraAbility || ability instanceof MartirAltruistaAbility;
    }

    /**
     * "Magias e Habilidades que permitam que outros personagens recuperarem PV" — a Magia that heals
     * and can reach beyond its caster, through either half of a dual "Alcance: Pessoal ou Toque".
     */
    static boolean healsOthers(final Spell spell) {
        return spell.getHealing().isPresent() && reaches(spell).anyMatch(reach -> reach != SpellReach.PESSOAL);
    }

    /** Every Alcance spell states — its own and, for a "Pessoal ou Toque" Magia, the other half. */
    private static Stream<SpellReach> reaches(final Spell spell) {
        return Stream.concat(Stream.ofNullable(spell.getTargeting()), spell.getAlternateTargeting().stream())
                .map(targeting -> targeting.reach());
    }

    /** The Habilidades de Curandeiro that let another character recover PV. */
    static boolean healsOthers(final AventyrTitleAbility ability) {
        return ability == CurandeiroDespertar.OS_DONS_DE_UM_CURANDEIRO
                || ability == MedicoDeGuerraAbility.ENCANTO_REGENERATIVO;
    }

    /** Curandeiro Veloz: "sua próxima Conjuração de Magias Naturais ou Divinas" that heals others. */
    private static boolean velozAppliesTo(final Spell spell) {
        return healsOthers(spell)
                && (spell.getTree().hasMagicType(MagicType.NATURAL) || spell.getTree().hasMagicType(MagicType.DIVINA));
    }
}
