package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link SobrevivenciaFeat#ESPECIALISTA_EM_ARMADURAS},
 * carrying the armour weight chosen ("um tipo de armadura entre Leve, Média ou Pesada"). Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant.
 *
 * <p>"Uma armadura do tipo escolhido" is an equipped {@link ItemCategory#ARMOR} whose authored
 * {@link Item#getWeightClass()} is the chosen tier. The authored column, not {@code
 * getEffectiveWeightClass()}, since the tier names the kind of armour. {@link
 * SobrevivenciaFeat#DOMINAR_ARMADURAS} widens the bonus to any armour and reads the choice via
 * {@link #chosenBy}.
 */
@Getter
public final class EspecialistaEmArmadurasFeat extends AbstractFeat {

    private final ItemWeightClass chosenWeight;

    public EspecialistaEmArmadurasFeat(@NonNull final ItemWeightClass chosenWeight) {
        super(SobrevivenciaFeat.ESPECIALISTA_EM_ARMADURAS.getFeatCategory(),
                SobrevivenciaFeat.ESPECIALISTA_EM_ARMADURAS.getDescription(),
                SobrevivenciaFeat.ESPECIALISTA_EM_ARMADURAS.getFeatRequirements());
        this.chosenWeight = chosenWeight;
    }

    public static EspecialistaEmArmadurasFeat of(@NonNull final ItemWeightClass chosenWeight) {
        return new EspecialistaEmArmadurasFeat(chosenWeight);
    }

    /** The armour weight a character chose, if they hold this Talento. */
    public static Optional<ItemWeightClass> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(EspecialistaEmArmadurasFeat.class::isInstance)
                .map(EspecialistaEmArmadurasFeat.class::cast)
                .map(EspecialistaEmArmadurasFeat::getChosenWeight)
                .findFirst();
    }

    /** Whether character wears any armour at all. */
    public static boolean wearsArmor(final Character character) {
        return character.getEquipment().stream().anyMatch(item -> item.getCategory() == ItemCategory.ARMOR);
    }

    /** Whether character wears an armour of weight. */
    public static boolean wearsArmorOf(final Character character, final ItemWeightClass weight) {
        return character.getEquipment().stream()
                .anyMatch(item -> item.getCategory() == ItemCategory.ARMOR && item.getWeightClass() == weight);
    }

    @Override
    public Feat catalogEntry() {
        return SobrevivenciaFeat.ESPECIALISTA_EM_ARMADURAS;
    }

    /**
     * "+1 em suas Defesas enquanto utilizar uma armadura do tipo escolhido", then +2/+3/+5 at 4/7/10
     * Graduações em Esquiva e Aparar. Held alongside {@link SobrevivenciaFeat#DOMINAR_ARMADURAS} it
     * applies to "qualquer armadura que vestir".
     */
    @Override
    public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
        boolean armored = character.getFeats().contains(SobrevivenciaFeat.DOMINAR_ARMADURAS)
                ? wearsArmor(character) : wearsArmorOf(character, chosenWeight);
        if (!armored) {
            return 0;
        }
        int graduation = character.getEffectiveGraduation(SkillType.ESQUIVA_E_APARAR);
        return graduation >= 10 ? 5 : graduation >= 7 ? 3 : graduation >= 4 ? 2 : 1;
    }
}
