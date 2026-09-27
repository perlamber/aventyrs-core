package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.DevorarInteiro;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.OgricoFeat;
import org.aventyrs.core.race.Ogro;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Devoured;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.aventyrs.core.util.TranslatableMessages.BOCARRA_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_DETERMINATION_POINTS;

public class DevourServiceImpl implements DevourService {

    /** Dois Estômagos: "considerado como tendo Vigor +2 para calcular a quantidade de alvos". */
    private static final int DOIS_ESTOMAGOS_CAPACITY = 2;

    private final CharacterSizeService characterSizeService;
    private final DeterminationPointsService determinationPointsService;
    private final HitPointsService hitPointsService;

    public DevourServiceImpl() {
        this(new CharacterSizeServiceImpl(), new DeterminationPointsServiceImpl(), new HitPointsServiceImpl());
    }

    public DevourServiceImpl(@NonNull final CharacterSizeService characterSizeService,
                             @NonNull final DeterminationPointsService determinationPointsService,
                             @NonNull final HitPointsService hitPointsService) {
        this.characterSizeService = characterSizeService;
        this.determinationPointsService = determinationPointsService;
        this.hitPointsService = hitPointsService;
    }

    @Override
    public boolean hasBocarra(@NonNull final CombatantSheet sheet) {
        Character character = sheet.getCharacter();
        return character != null && character.getRace() instanceof Ogro
                && !sheet.getRacialTraitSuppression().suppressesPhysicalTraits();
    }

    @Override
    public BocarraBite declareBite(@NonNull final CombatantSheet ogre) {
        if (!hasBocarra(ogre)) {
            throw new IllegalOperationException(BOCARRA_NOT_HELD);
        }
        Character character = ogre.getCharacter();
        if (determinationPointsService.getCurrentDeterminationPoints(character, ogre) < BITE_DETERMINATION_POINT_COST) {
            throw new IllegalOperationException(NOT_ENOUGH_DETERMINATION_POINTS);
        }
        ogre.spendDeterminationPoints(BITE_DETERMINATION_POINT_COST);
        return new BocarraBite(BITE_ACTION_POINT_INCREASE, Skill.ADVANTAGE_BONUS, devoratrizLifeSteal(character),
                new DevorarInteiro(ogre, this));
    }

    @Override
    public int getStomachCapacity(@NonNull final Character ogre) {
        return vigorOf(ogre) + (holds(ogre, OgricoFeat.DOIS_ESTOMAGOS) ? DOIS_ESTOMAGOS_CAPACITY : 0);
    }

    @Override
    public OptionalInt getVigorOccupied(@NonNull final CombatantSheet ogre, @NonNull final CombatantSheet victim) {
        int smallerBy = characterSizeService.getEffectiveSizeCategory(ogre).getCategory()
                - characterSizeService.getEffectiveSizeCategory(victim).getCategory();
        boolean mandibula = holds(ogre.getCharacter(), OgricoFeat.MANDIBULA_DESARTICULADA);
        int occupied;
        if (smallerBy >= 2) {
            occupied = 1;
        } else if (smallerBy == 1) {
            occupied = 2;
        } else if (mandibula && smallerBy == 0) {
            occupied = 3;
        } else if (mandibula && smallerBy == -1) {
            occupied = 4;
        } else {
            return OptionalInt.empty();
        }
        if (holds(ogre.getCharacter(), OgricoFeat.PODEROSO_GLUTAO)) {
            occupied = Math.max(1, occupied - 1);
        }
        return OptionalInt.of(occupied);
    }

    @Override
    public int getVigorInUse(@NonNull final CombatantSheet ogre) {
        return ogre.getDevouredVictims().stream()
                .mapToInt(victim -> victim.getDevoured().map(Devoured::getVigorOccupied).orElse(0))
                .sum();
    }

    @Override
    public boolean swallow(@NonNull final CombatantSheet ogre, @NonNull final CombatantSheet victim) {
        if (ogre == victim || victim.getDevoured().isPresent()) {
            return false;
        }
        OptionalInt occupied = getVigorOccupied(ogre, victim);
        if (occupied.isEmpty()
                || getVigorInUse(ogre) + occupied.getAsInt() > getStomachCapacity(ogre.getCharacter())) {
            return false;
        }
        victim.applyCondition(new Devoured(ogre, occupied.getAsInt()));
        if (victim.getDevoured().isEmpty()) {
            return false;
        }
        ogre.addDevouredVictim(victim);
        return true;
    }

    @Override
    public int getDigestionDamage(@NonNull final Character ogre) {
        return 1 + vigorOf(ogre) / 2;
    }

    @Override
    public List<Digestion> digest(@NonNull final CombatantSheet ogre) {
        Character character = ogre.getCharacter();
        boolean devoratriz = holds(character, OgricoFeat.DEVORATRIZ);
        int damage = getDigestionDamage(character);
        List<Digestion> digestions = new ArrayList<>();
        for (CombatantSheet victim : ogre.getDevouredVictims()) {
            victim.applyDamage(damage);
            int recovered = 0;
            boolean killed = false;
            if (devoratriz) {
                recovered += heal(ogre, devoratrizLifeSteal(character));
                int maxHitPoints = hitPointsService.getMaxHitPoints(victim.getCharacter(), victim);
                int currentHitPoints = maxHitPoints - victim.getDamageTaken();
                if (currentHitPoints <= 0) {
                    // "Morrem instantaneamente": down to -max PV, the threshold getStatus calls DEAD.
                    victim.applyDamage(currentHitPoints + maxHitPoints);
                    killed = true;
                    recovered += heal(ogre, vigorOf(victim.getCharacter()));
                    regurgitate(ogre, victim);
                }
            }
            digestions.add(new Digestion(victim, damage, killed, recovered));
        }
        return digestions;
    }

    @Override
    public int getEscapeDamage(@NonNull final Character ogre) {
        return 2 * vigorOf(ogre);
    }

    @Override
    public boolean recordDamageFromInside(@NonNull final CombatantSheet ogre, @NonNull final CombatantSheet victim,
                                          final int damage) {
        Devoured devoured = victim.getDevoured().filter(held -> held.getSource() == ogre).orElse(null);
        if (devoured == null) {
            return false;
        }
        if (devoured.recordDamageFromInside(damage) >= getEscapeDamage(ogre.getCharacter())) {
            regurgitate(ogre, victim);
            return true;
        }
        return false;
    }

    @Override
    public void regurgitate(@NonNull final CombatantSheet ogre, @NonNull final CombatantSheet victim) {
        if (victim.getDevoured().filter(held -> held.getSource() == ogre).isPresent()) {
            victim.removeCondition(ConditionType.DEVORADO);
        }
        ogre.removeDevouredVictim(victim);
    }

    @Override
    public int getDigestionHours(@NonNull final CombatantSheet ogre, @NonNull final CombatantSheet victim) {
        int occupied = victim.getDevoured().filter(held -> held.getSource() == ogre)
                .map(Devoured::getVigorOccupied)
                .orElseGet(() -> getVigorOccupied(ogre, victim).orElse(0));
        int hours = 2 * occupied;
        return holds(ogre.getCharacter(), OgricoFeat.PODEROSO_GLUTAO) ? hours / 2 : hours;
    }

    @Override
    public int getRegurgitationActionPoints(final int hoursInside) {
        return Math.max(1, hoursInside);
    }

    /** Devoratriz: "Seu Roubo de Vida é igual a sua quantidade de Título Aventyrs Despertos." */
    private static int devoratrizLifeSteal(final Character character) {
        return holds(character, OgricoFeat.DEVORATRIZ) ? character.getAllTitles().size() : 0;
    }

    private static int heal(final CombatantSheet ogre, final int amount) {
        return amount > 0 ? ogre.heal(amount, new HealingSource(OgricoFeat.DEVORATRIZ, true, ogre, null, null)) : 0;
    }

    private static boolean holds(final Character character, final Feat talento) {
        return character != null && character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == talento);
    }

    private static int vigorOf(final Character character) {
        return character == null ? 0 : character.getEffectiveAttributeTotal(AttributeDomain.VIGOR);
    }
}
