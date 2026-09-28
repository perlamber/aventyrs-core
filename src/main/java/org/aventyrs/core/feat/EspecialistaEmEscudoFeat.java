package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link EscudeiroFeat#ESPECIALISTA_EM_ESCUDO}, carrying the
 * {@link ShieldSpecialty} chosen ("um item escolhido do tipo ‘Escudo’"). Grant <em>this</em> in {@code
 * Character#feats} in place of the bare enum constant — the same split {@link
 * EspecialistaEmArmaFeat} keeps against {@code DuelistaFeat#ESPECIALISTA_EM_ARMA}.
 *
 * <p>The choice is a <b>catalog entry</b>, not a copy: "especialista" in a kind of Escudo, so any
 * equipped copy forged from the chosen {@link ShieldItem} counts ({@link #isChosenShield}). Or it is
 * {@link ShieldSpecialty#ASAS_ADAMANTINAS} — the holder's own wings, a Escudo while they are not
 * flying (table ruling, 2026-09-27) — which only a sheet can judge, since flight is sheet state ({@link
 * #isUsingChosenShield(CombatantSheet)}). {@code EscudeiroFeat#DEFESA_TARTARUGA}'s "um escudo que você
 * seja especialista" reads the choice the same way.
 */
@Getter
public final class EspecialistaEmEscudoFeat extends AbstractFeat {

    /** "Este Bônus aumenta para +2" at this many Graduações em Esquiva e Aparar. */
    private static final int SECOND_TIER_GRADUATION = 4;

    /** "… se tiver 7 ou mais Graduações este Bônus aumenta para +3". */
    private static final int THIRD_TIER_GRADUATION = 7;

    private final ShieldSpecialty chosenSpecialty;

    public EspecialistaEmEscudoFeat(@NonNull final ShieldSpecialty chosenSpecialty) {
        super(EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getFeatCategory(),
                EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getDescription(),
                EscudeiroFeat.ESPECIALISTA_EM_ESCUDO.getFeatRequirements());
        this.chosenSpecialty = chosenSpecialty;
    }

    public EspecialistaEmEscudoFeat(@NonNull final ShieldItem chosenShield) {
        this(ShieldSpecialty.of(chosenShield));
    }

    public static EspecialistaEmEscudoFeat of(@NonNull final ShieldItem chosenShield) {
        return new EspecialistaEmEscudoFeat(chosenShield);
    }

    public static EspecialistaEmEscudoFeat of(@NonNull final ShieldSpecialty chosenSpecialty) {
        return new EspecialistaEmEscudoFeat(chosenSpecialty);
    }

    /** The catalog Escudo chosen, or {@code null} when the choice is the wings. */
    public ShieldItem getChosenShield() {
        return chosenSpecialty.getShield();
    }

    /** The catalog Escudo a character chose, if they hold this Talento for one (not the wings). */
    public static Optional<ShieldItem> chosenBy(final Character character) {
        return specialtyOf(character).map(ShieldSpecialty::getShield);
    }

    /** What a character is Especialista in, wings included, if they hold this Talento. */
    public static Optional<ShieldSpecialty> specialtyOf(final Character character) {
        return character.getFeats().stream()
                .filter(EspecialistaEmEscudoFeat.class::isInstance)
                .map(EspecialistaEmEscudoFeat.class::cast)
                .map(EspecialistaEmEscudoFeat::getChosenSpecialty)
                .findFirst();
    }

    /**
     * Whether item is the chosen Escudo: the catalog entry itself, or a copy forged from it
     * ({@code AbstractItem#getTemplate()}).
     */
    public static boolean isChosenShield(final Item item, final ShieldItem chosen) {
        return chosen != null && (item == chosen || item instanceof AbstractItem copy && copy.getTemplate() == chosen);
    }

    /**
     * Whether character has the Escudo they are Especialista in among their equipment. The wings can't
     * be told from a {@link Character} alone — whether they fly is sheet state — so they read as not
     * in use here; ask the sheet form.
     */
    public static boolean isUsingChosenShield(final Character character) {
        return chosenBy(character)
                .map(chosen -> character.getEquipment().stream().anyMatch(item -> isChosenShield(item, chosen)))
                .orElse(false);
    }

    /** {@link #isUsingChosenShield(Character)} with the wings judged too: held Asas Adamantinas, not flying. */
    public static boolean isUsingChosenShield(final CombatantSheet sheet) {
        Optional<ShieldSpecialty> specialty = specialtyOf(sheet.getCharacter());
        if (specialty.isEmpty()) {
            return false;
        }
        return specialty.get().isWings()
                ? EscudeiroFeat.holdsWings(sheet.getCharacter()) && !sheet.isFlying()
                : isUsingChosenShield(sheet.getCharacter());
    }

    @Override
    public Feat catalogEntry() {
        return EscudeiroFeat.ESPECIALISTA_EM_ESCUDO;
    }

    /**
     * "+1 em suas Defesas enquanto utilizar um item escolhido do tipo ‘Escudo’", +2 at 4
     * Graduações em Esquiva e Aparar and +3 at 7. "Defesas" names both, so both {@link
     * DefenseType}s get it. A destroyed copy still counts as "utilizado" — the bonus is the
     * character's skill with the shape, not a column of the item. The wings need the sheet.
     */
    @Override
    public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
        return resolveDefenseBonus(defenseType, character, null, null);
    }

    @Override
    public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                   final SceneContext sceneContext, final CombatantSheet holder) {
        boolean using = holder != null ? isUsingChosenShield(holder) : isUsingChosenShield(character);
        if (!using) {
            return 0;
        }
        int graduation = character.getEffectiveGraduation(SkillType.ESQUIVA_E_APARAR);
        return graduation >= THIRD_TIER_GRADUATION ? 3 : graduation >= SECOND_TIER_GRADUATION ? 2 : 1;
    }
}
