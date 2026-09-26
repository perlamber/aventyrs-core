package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link EscudeiroFeat#ESPECIALISTA_EM_ESCUDO}, carrying the
 * {@link ShieldItem} chosen ("um item escolhido do tipo ‘Escudo’"). Grant <em>this</em> in {@code
 * Character#feats} in place of the bare enum constant — the same split {@link
 * EspecialistaEmArmaFeat} keeps against {@code DuelistaFeat#ESPECIALISTA_EM_ARMA}.
 *
 * <p>The choice is a <b>catalog entry</b>, not a copy: "especialista" in a kind of Escudo, so any
 * equipped copy forged from the chosen {@link ShieldItem} counts ({@link #isChosenShield}).
 * {@code EscudeiroFeat#DEFESA_TARTARUGA}'s "um escudo que você seja especialista" reads the choice
 * via {@link #chosenBy}.
 */
@Getter
public final class EspecialistaEmEscudoFeat extends AbstractFeat {

    /** "Este Bônus aumenta para +2" at this many Graduações em Esquiva e Aparar. */
    private static final int SECOND_TIER_GRADUATION = 4;

    /** "… se tiver 7 ou mais Graduações este Bônus aumenta para +3". */
    private static final int THIRD_TIER_GRADUATION = 7;

    private final ShieldItem chosenShield;

    public EspecialistaEmEscudoFeat(@NonNull final ShieldItem chosenShield) {
        super(EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getFeatCategory(),
                EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getDescription(),
                EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getFeatRequirements());
        this.chosenShield = chosenShield;
    }

    public static EspecialistaEmEscudoFeat of(@NonNull final ShieldItem chosenShield) {
        return new EspecialistaEmEscudoFeat(chosenShield);
    }

    /** The Escudo a character chose, if they hold this Talento. Mirrors {@link EspecialistaEmArmaFeat#chosenBy}. */
    public static Optional<ShieldItem> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(EspecialistaEmEscudoFeat.class::isInstance)
                .map(EspecialistaEmEscudoFeat.class::cast)
                .map(EspecialistaEmEscudoFeat::getChosenShield)
                .findFirst();
    }

    /**
     * Whether item is the chosen Escudo: the catalog entry itself, or a copy forged from it
     * ({@code AbstractItem#getTemplate()}).
     */
    public static boolean isChosenShield(final Item item, final ShieldItem chosen) {
        return item == chosen || item instanceof AbstractItem copy && copy.getTemplate() == chosen;
    }

    /** Whether character has the Escudo they are Especialista in among their equipment. */
    public static boolean isUsingChosenShield(final Character character) {
        return chosenBy(character)
                .map(chosen -> character.getEquipment().stream().anyMatch(item -> isChosenShield(item, chosen)))
                .orElse(false);
    }

    @Override
    public Feat catalogEntry() {
        return EscudeiroFeat.ESPECIALISTA_EM_ESCUDO;
    }

    /**
     * "+1 em suas Defesas enquanto utilizar um item escolhido do tipo ‘Escudo’", +2 at 4
     * Graduações em Esquiva e Aparar and +3 at 7. "Defesas" names both, so both {@link
     * DefenseType}s get it. A destroyed copy still counts as "utilizado" — the bonus is the
     * character's skill with the shape, not a column of the item.
     */
    @Override
    public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
        if (!character.getEquipment().stream().anyMatch(item -> isChosenShield(item, chosenShield))) {
            return 0;
        }
        int graduation = character.getEffectiveGraduation(SkillType.ESQUIVA_E_APARAR);
        return graduation >= THIRD_TIER_GRADUATION ? 3 : graduation >= SECOND_TIER_GRADUATION ? 2 : 1;
    }
}
