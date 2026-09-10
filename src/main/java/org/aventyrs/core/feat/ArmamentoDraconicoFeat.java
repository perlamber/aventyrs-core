package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.NaturalWeapon;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The acquired, per-character form of {@link DraconicoFeat#ARMAMENTO_DRACONICO}, carrying the
 * two Armas Naturais the player chose ("escolha duas armas entre: Chifres Poderosos, Cauda
 * Chicote, Garras Afiadas e Presas Longas"). Grant <em>this</em> in {@code Character#feats} in
 * place of the bare enum constant — the same split {@link FocoEmPericiaFeat} keeps against
 * {@code PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p>{@link #catalogEntry()} returns {@link DraconicoFeat#ARMAMENTO_DRACONICO}, so {@code
 * Feat#isEligible}'s {@code requiredFeat} check and {@code FeatCatalog#availableFor}'s
 * "not already held" filter both still see it as the catalog constant.
 */
@Getter
public final class ArmamentoDraconicoFeat extends AbstractFeat {

    /** The four Armas Naturais the rules text lets a Nascido do Dragão pick from. */
    public static final Set<NaturalWeapon> ALLOWED_CHOICES = EnumSet.of(
            NaturalWeapon.CHIFRES_PODEROSOS,
            NaturalWeapon.CAUDA_CHICOTE,
            NaturalWeapon.GARRAS_AFIADAS,
            NaturalWeapon.PRESAS_LONGAS);

    private static final int REQUIRED_CHOICES = 2;

    private final Set<NaturalWeapon> chosenWeapons;

    public ArmamentoDraconicoFeat(@NonNull final Set<NaturalWeapon> chosenWeapons) {
        super(DraconicoFeat.ARMAMENTO_DRACONICO.getFeatCategory(),
                DraconicoFeat.ARMAMENTO_DRACONICO.getDescription(),
                DraconicoFeat.ARMAMENTO_DRACONICO.getFeatRequirements());
        if (chosenWeapons.size() != REQUIRED_CHOICES || !ALLOWED_CHOICES.containsAll(chosenWeapons)) {
            throw new IllegalArgumentException(
                    "Armamento Dracônico must pick exactly " + REQUIRED_CHOICES + " of " + ALLOWED_CHOICES
                            + ", got " + chosenWeapons);
        }
        this.chosenWeapons = EnumSet.copyOf(chosenWeapons);
    }

    public static ArmamentoDraconicoFeat of(@NonNull final NaturalWeapon first, @NonNull final NaturalWeapon second) {
        return new ArmamentoDraconicoFeat(EnumSet.of(first, second));
    }

    @Override
    public Feat catalogEntry() {
        return DraconicoFeat.ARMAMENTO_DRACONICO;
    }

    /** "Você possui as Armas Naturais escolhidas." */
    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
        return List.copyOf(chosenWeapons);
    }
}
