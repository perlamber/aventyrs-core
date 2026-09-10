package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.AttributeAbilityService;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Optional;

import static org.aventyrs.core.util.TranslatableMessages.CHOSEN_ATTRIBUTE_ABILITY_REQUIREMENT_NOT_MET;

/**
 * The acquired, per-character form of the three {@link DestinoFeat} constants whose whole payload
 * is "escolha um Atributo, você recebe uma das Habilidades do Atributo escolhido" — {@link
 * DestinoFeat#PRODIGIO}, {@link DestinoFeat#GENIALIDADE} and {@link
 * DestinoFeat#GENIALIDADE_DESPERTA}. Grant <em>this</em> in {@code Character#feats} in place of
 * the bare enum constant.
 *
 * <p><b>The chosen Atributo needs no field of its own</b> — an {@link AttributeAbility} already
 * reports its own {@link AttributeAbility#getAttributeDomain()}, so recording the ability records
 * the Atributo with it, and the two can never disagree. That is what makes one class serve all
 * three constants; the only thing that varies is Genialidade Desperta's extra Bônus Racial.
 *
 * <p>Same mechanism as {@link ConselheiroDeGuerraYmirianoFeat}'s free Habilidade de Força: the
 * ability is folded into {@code Character#getAttributeAbilities()} by {@code
 * Feat#getGrantedAttributeAbilities} without consuming an {@code AttributeAbilityService} slot,
 * and only its passive / {@code resolve*} hooks apply — see that hook's javadoc for what a
 * granted ability does <em>not</em> run.
 */
@Getter
public final class HabilidadeDeAtributoEscolhidaFeat extends AbstractFeat {

    /** Genialidade Desperta's own "+1 de Bônus Racial no Atributo escolhido". */
    private static final int GENIALIDADE_DESPERTA_RACIAL_BONUS = 1;

    private final DestinoFeat talento;
    private final AttributeAbility chosenAbility;

    public HabilidadeDeAtributoEscolhidaFeat(@NonNull final DestinoFeat talento,
                                             @NonNull final AttributeAbility chosenAbility) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenAbility = chosenAbility;
    }

    /**
     * The validated factory — use this at grant time. Enforces the shared "você ainda precisa
     * preencher requisitos de Atributos Mínimos" clause: a Habilidade of some Atributo needs at
     * least one slot in it, i.e. that Atributo's <b>base</b> ≥ {@link
     * AttributeAbilityService#FIRST_ABILITY_ATTRIBUTE_BASE}.
     *
     * <p>{@link DestinoFeat#PRODIGIO}'s own "um Atributo que você possua valor Base 2 ou superior"
     * is the same floor stated outright, so no constant needs a second check. The raw constructor
     * stays unvalidated for the builder-bypass convention, exactly as {@code
     * ConselheiroDeGuerraYmirianoFeat} splits its own two entry points.
     */
    public static HabilidadeDeAtributoEscolhidaFeat of(@NonNull final Character holder,
                                                       @NonNull final DestinoFeat talento,
                                                       @NonNull final AttributeAbility chosenAbility) {
        int base = holder.getAttributes().getAttribute(chosenAbility.getAttributeDomain()).getBase();
        if (base < AttributeAbilityService.FIRST_ABILITY_ATTRIBUTE_BASE) {
            throw new IllegalOperationException(CHOSEN_ATTRIBUTE_ABILITY_REQUIREMENT_NOT_MET);
        }
        return new HabilidadeDeAtributoEscolhidaFeat(talento, chosenAbility);
    }

    /** The ability a character chose for talento, if they hold it in its acquired form. */
    public static Optional<AttributeAbility> chosenBy(final Character character, final DestinoFeat talento) {
        return character.getFeats().stream()
                .filter(HabilidadeDeAtributoEscolhidaFeat.class::isInstance)
                .map(HabilidadeDeAtributoEscolhidaFeat.class::cast)
                .filter(held -> held.getTalento() == talento)
                .map(HabilidadeDeAtributoEscolhidaFeat::getChosenAbility)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    /** "Você recebe uma das Habilidades do Atributo escolhido." */
    @Override
    public List<AttributeAbility> getGrantedAttributeAbilities(final Character character) {
        return List.of(chosenAbility);
    }

    /**
     * "+1 de Bônus Racial no Atributo escolhido" — {@link DestinoFeat#GENIALIDADE_DESPERTA} only;
     * its two siblings grant the ability alone. Modelled as a {@code Feat#resolveAttributeBonus}
     * grant rather than a write to {@code AttributeValue#racialBonus}, the same reading every
     * other "recebe Bônus Racial de +1" Talento takes — see {@code BestialFeat}'s Heranças.
     */
    @Override
    public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return talento == DestinoFeat.GENIALIDADE_DESPERTA
                && domain == chosenAbility.getAttributeDomain()
                ? GENIALIDADE_DESPERTA_RACIAL_BONUS : 0;
    }
}
