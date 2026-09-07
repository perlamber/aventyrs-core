package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.AttributeAbilityService;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.CONSELHEIRO_STRENGTH_ABILITY_REQUIREMENT_NOT_MET;

/**
 * The acquired, per-character form of {@link AnaoFeat#CONSELHEIRO_DE_GUERRA_YMIRIANO}, carrying
 * the {@link StrengthAbility} the player chose ("1 Habilidade de Força que você cumpra os
 * requisitos"). Grant <em>this</em> in {@code Character#feats} in place of the bare enum
 * constant — the same catalog-vs-acquired split {@link FocoEmPericiaFeat} keeps against {@code
 * PeritoFeat#FOCO_EM_PERICIA}, and {@link ArmamentoDraconicoFeat} against {@code
 * DraconicoFeat#ARMAMENTO_DRACONICO}.
 *
 * <p>Two mechanics, both real:
 * <ul>
 *   <li>"Bônus Racial de +1 em Gnose" — {@link #resolveAttributeBonus}. Now that every
 *       Atributo-total reader routes through {@code Character#getEffectiveAttributeTotal}, this
 *       reaches PV/PM/PD/Defesa/Conjuração/Rest and every Gnose-governed Perícia roll — not just
 *       the roll path {@code VampiricoFeat#MESTRE_VAMPIRO} was limited to when the hook was new.
 *   <li>"1 Habilidade de Força" — {@link #getGrantedAttributeAbilities}, folded into {@code
 *       Character#getAttributeAbilities()} without consuming an {@code AttributeAbilityService}
 *       slot. Only the ability's passive / {@code resolve*} hooks apply — see that hook's
 *       javadoc; every {@code StrengthAbility} constant is passive, so nothing is lost here.
 * </ul>
 *
 * <p>{@link #catalogEntry()} returns the enum constant, so {@code Feat#isEligible}'s {@code
 * requiredFeat} check and {@code FeatCatalog#availableFor}'s "already held" filter still see it
 * as the catalog constant.
 */
@Getter
public final class ConselheiroDeGuerraYmirianoFeat extends AbstractFeat {

    private static final int GNOSE_RACIAL_BONUS = 1;

    private final StrengthAbility chosenAbility;

    public ConselheiroDeGuerraYmirianoFeat(@NonNull final StrengthAbility chosenAbility) {
        super(AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO.getFeatCategory(),
                AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO.getDescription(),
                AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO.getFeatRequirements());
        this.chosenAbility = chosenAbility;
    }

    /**
     * The validated factory — use this at grant time. Enforces the Descrição's "que você cumpra
     * os requisitos": a Habilidade de Força needs at least one Força ability slot, i.e. Força
     * <b>base</b> ≥ {@link AttributeAbilityService#FIRST_ABILITY_ATTRIBUTE_BASE}. Throws {@link
     * IllegalOperationException} otherwise.
     *
     * <p>The raw {@link #ConselheiroDeGuerraYmirianoFeat(StrengthAbility) constructor} stays
     * unvalidated for the builder-bypass convention (fixtures, tests staging a state directly) —
     * the same split {@code Character#grantFeat} / {@code FeatService#grantFeat} keeps.
     */
    public static ConselheiroDeGuerraYmirianoFeat of(@NonNull final Character holder,
                                                     @NonNull final StrengthAbility chosenAbility) {
        int strengthBase = holder.getAttributes().getAttribute(AttributeDomain.STRENGTH).getBase();
        if (strengthBase < AttributeAbilityService.FIRST_ABILITY_ATTRIBUTE_BASE) {
            throw new IllegalOperationException(CONSELHEIRO_STRENGTH_ABILITY_REQUIREMENT_NOT_MET);
        }
        return new ConselheiroDeGuerraYmirianoFeat(chosenAbility);
    }

    /**
     * The Habilidade de Força a character chose for this Talento, if they hold it — mirrors
     * {@link FocoEmPericiaFeat#chosenBy}, for any future dependent constant.
     */
    public static Optional<StrengthAbility> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(ConselheiroDeGuerraYmirianoFeat.class::isInstance)
                .map(ConselheiroDeGuerraYmirianoFeat.class::cast)
                .map(ConselheiroDeGuerraYmirianoFeat::getChosenAbility)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return AnaoFeat.CONSELHEIRO_DE_GUERRA_YMIRIANO;
    }

    /** "Você adquire Bônus Racial de +1 em Gnose." */
    @Override
    public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return domain == AttributeDomain.GNOSE ? GNOSE_RACIAL_BONUS : 0;
    }

    /** "1 Habilidade de Força (que você cumpra os requisitos)." */
    @Override
    public List<AttributeAbility> getGrantedAttributeAbilities(final Character character) {
        return List.of(chosenAbility);
    }
}
