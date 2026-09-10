package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillTrait;

import java.util.List;
import java.util.Set;

/**
 * The acquired form of any Talento whose <em>whole</em> payload is "you get one or more
 * Habilidades de Competência / Especializações of your choosing" — {@code
 * PeritoFeat#TREINADO_EM_PERICIAS} ("uma Especialização ou Habilidade de Competência de cada uma
 * destas Perícias", three of them), {@code GnomoFeat#SABICHAO} and {@code
 * GnomoFeat#MIMETIZAR_COMPETENCIA} ("uma Habilidade de Competência de uma Perícia treinada").
 * Grant <em>this</em> in {@code Character#feats} in place of the bare enum constant.
 *
 * <p><b>No delegation, and that is the entry condition.</b> Unlike {@link HerancaBestialFeat},
 * which forwards its constant's own Atributo and Arma Natural clauses, every constant this class
 * serves grants the traits and nothing else — so replacing the constant loses nothing. A Talento
 * that grants traits <i>alongside</i> another effect needs its own class (or a forwarding one
 * like the Bestial Heranças'); do not widen this one with delegation, because the effects that
 * would need forwarding differ per tree and a blanket forward would make it impossible to drop
 * a clause deliberately.
 *
 * <p>How many traits may legally be chosen, and whether each belongs to a Perícia the holder is
 * trained in — "de uma Perícia Treinada qual tenha pelo menos 2 Graduações" — is not validated
 * here: the usual builders-aren't-gatekeepers restraint, the same one {@link AdotadoPorSylphFeat}
 * applies to its own per-Título count.
 */
@Getter
public final class ChosenSkillTraitsFeat extends AbstractFeat {

    private final Feat talento;
    private final Set<SkillTrait> chosenTraits;

    public ChosenSkillTraitsFeat(@NonNull final Feat talento, @NonNull final Set<SkillTrait> chosenTraits) {
        super(talento.getFeatCategory(), talento.getDescription(), talento.getFeatRequirements());
        this.talento = talento;
        this.chosenTraits = Set.copyOf(chosenTraits);
    }

    public static ChosenSkillTraitsFeat of(@NonNull final Feat talento,
                                           @NonNull final SkillTrait... chosenTraits) {
        return new ChosenSkillTraitsFeat(talento, Set.of(chosenTraits));
    }

    /** The traits a character chose for talento, empty if they don't hold it in acquired form. */
    public static Set<SkillTrait> chosenBy(final Character character, final Feat talento) {
        return character.getFeats().stream()
                .filter(ChosenSkillTraitsFeat.class::isInstance)
                .map(ChosenSkillTraitsFeat.class::cast)
                .filter(held -> held.getTalento() == talento)
                .map(ChosenSkillTraitsFeat::getChosenTraits)
                .findFirst()
                .orElseGet(Set::of);
    }

    @Override
    public Feat catalogEntry() {
        return talento;
    }

    @Override
    public List<SkillTrait> getGrantedSkillTraits(final Character character) {
        return List.copyOf(chosenTraits);
    }
}
