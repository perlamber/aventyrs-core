package org.aventyrs.core.title.curandeiro;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
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

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Curandeiro — "Devotos fiéis dos Deuses, Curandeiros são aqueles que devotam sua vida em ajudar e
 * proteger os vulneráveis". A <b>minimal</b> scaffold: the Título, its two Especializações and every
 * Habilidade/Suprema are authored, but only the two that bend the limits on healing the fallen are
 * real — Levantar os Caídos ({@link #bypassesComaHealingCap}) and Curar os Mortos ({@link
 * #claimRevival}). Everything else carries a TODO naming what it waits on.
 *
 * <p>Both hooks are asked of the <b>healer's</b> Títulos by {@link CombatantSheet#heal(int,
 * HealingSource)}, so "Suas Magias" needs no check that the healer is this Título's holder: a scan
 * over someone else's Títulos never reaches this instance.
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

    // TODO: Despertar's "Vantagem nas rolagens de 'Medicina e Cura'" — no Título skill-roll bonus hook
    //  exists (AventyrTitle#resolveAttackRollBonus is scoped to attacks).
    // TODO: Despertar's heal activation — the Descanso Curto/Longo heal and its "não pode afetar um mesmo
    //  personagem … até que você passe por um Descanso Longo" need a Despertar activation constant (the
    //  GiganteEnfurecidoDespertar shape) and a per-target ledger cleared by a Rest; once built it heals
    //  through HealingSource.titleAbility, and so is a Habilidade de Curandeiro for both hooks here.
    // TODO: Domínio da Cura — no Título hook substitutes one Perícia for another (see the
    //  ability-acquisition-and-substitution skill).

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

    /**
     * Levantar os Caídos: "Suas Magias e Habilidades de Curandeiro que permitam a recuperação de PV
     * ignoram as Reduções de Cura do estado de Coma, mas apenas quando adquiridos na mesma Cena" —
     * any Magia of the holder's, or a Habilidade de Curandeiro, on a target whose Coma began in the
     * current Cena.
     */
    @Override
    public boolean bypassesComaHealingCap(@NonNull final HealingSource source, @NonNull final CombatantSheet target) {
        return holds(CurandeiroAbility.LEVANTAR_OS_CAIDOS)
                && (source.spell() != null || isCurandeiroTrait(source.titleAbility()))
                && target.hasEnteredComaThisScene();
    }

    /**
     * Curar os Mortos: a Magia Divina or Natural, or a Habilidade de Curandeiro, reaches a target dead
     * for at most {@link #resolveRevivalWindowRounds} Rodadas — spending one of the healer's revival
     * charges ({@link CurarOsMortosInteraction}). Refuses, spending nothing, when any of that is
     * missing.
     */
    @Override
    public boolean claimRevival(@NonNull final HealingSource source, @NonNull final CombatantSheet target) {
        if (!holds(CurandeiroAbility.CURAR_OS_MORTOS) || source.healer() == null) {
            return false;
        }
        if (!source.isSpellOfType(MagicType.DIVINA, MagicType.NATURAL) && !isCurandeiroTrait(source.titleAbility())) {
            return false;
        }
        OptionalInt roundsSinceDeath = target.getRoundsSinceDeath();
        if (roundsSinceDeath.isEmpty()
                || roundsSinceDeath.getAsInt() > resolveRevivalWindowRounds(source.healer().getCharacter())) {
            return false;
        }
        return source.healer().consumeRevivalCharge(CurandeiroAbility.CURAR_OS_MORTOS);
    }

    /**
     * "personagens que tenham morrido em até uma quantidade de Rodadas igual à metade de suas
     * Graduações em Medicina e Cura" — the holder's, floored.
     */
    public int resolveRevivalWindowRounds(@NonNull final Character holder) {
        CharacterSkill medicina = holder.getSkills().get(SkillType.MEDICINA_E_CURA);
        return medicina == null ? 0 : medicina.getGraduation().getGraduationValue() / 2;
    }

    /** Activates Curar os Mortos — a one-line delegate to {@link #activateAbility}. */
    public InteractionResult activateCurarOsMortos(@NonNull final TitleAbilityActivationRequest request) {
        return activateAbility(CurandeiroAbility.CURAR_OS_MORTOS, request);
    }

    /** Whether ability is one of Curandeiro's own traits — "Habilidades de Curandeiro". */
    static boolean isCurandeiroTrait(final AventyrTitleAbility ability) {
        return ability instanceof CurandeiroAbility || ability instanceof CurandeiroSpecialization
                || ability instanceof MedicoDeGuerraAbility || ability instanceof MartirAltruistaAbility;
    }
}
