package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gigante Enfurecido's Frenesi — "um estado de Frenesi profundo" lasting a number of Rodadas, and
 * the state every other Gigante trait reads. One object rather than a handful of Blessings because
 * the traits need it as a <em>state</em>: Prolongar Descontrole lengthens it, the Especializações and
 * Supremas add modes to it, Berserker stacks onto it as its holder is hit, and all of it ends
 * together — including the drawbacks (the concentration block), which no {@link TemporaryBonus}
 * could carry.
 *
 * <p><b>Its numbers reach play through {@link CombatantSheet#getTemporaryBonus}</b>, which adds
 * {@link #bonusFor} to the {@link TemporaryBonus} sum. So every existing reader of a timed bonus —
 * the Força total, Iniciativa, the Categoria de Tamanho, the PV/PM/PD Multiplicadores, Defesas,
 * Movimento, Margem Crítica, RA — sees a Frenesi with nothing Gigante-specific of its own.
 *
 * <p>Counts down at its holder's Turn start: it is started on their own Turn (or as a Reação), and
 * "N Rodadas" then means N of their Turns begin before it ends.
 *
 * <p>An {@linkplain #isInspired() inspired} copy is Grito Inspirador's "os mesmos efeitos de seus
 * Frenesis ativos … por 1 Rodada" landing on an ally: the same bonuses, modes and drawbacks, but
 * none of the Autocontrole bookkeeping, which belongs to the Gigante's own pool.
 */
@Getter
public class Frenzy extends TemporaryEffect {

    /** Whose Frenesi this is — the Gigante's own id, also on an inspired copy. */
    private final UUID holderId;
    private final boolean inspired;
    @Getter(lombok.AccessLevel.NONE)
    private final EnumSet<FrenzyMode> modes;
    @Getter(lombok.AccessLevel.NONE)
    private final EnumSet<ElementalType> cataclysmElements;
    @Getter(lombok.AccessLevel.NONE)
    private final EnumMap<ModifierType, Integer> bonuses;
    /**
     * Whether the concentration block holds — "não pode utilizar Perícias que exijam concentração ou
     * raciocínio … perde a capacidade de Conjurar ou Mimetizar Magias". Lifted by Titã Enlouquecido.
     */
    private boolean concentrationBlocked;
    /** Desprezar Danos held: RA while in Frenesi, Meio-Dano and halved healing at 0 PV or below. */
    private boolean scornsDamage;
    /** Fanático de Cyt was used during this Frenesi. */
    private boolean fanatic;
    /** Temporary Autocontrole spent "para ativar efeitos de Frenesi" during this Frenesi. */
    private int autocontroleSpent;

    public Frenzy(@NonNull final UUID holderId, final int rounds) {
        this(holderId, rounds, false);
    }

    private Frenzy(final UUID holderId, final int rounds, final boolean inspired) {
        super(rounds, true);
        this.holderId = holderId;
        this.inspired = inspired;
        this.modes = EnumSet.noneOf(FrenzyMode.class);
        this.cataclysmElements = EnumSet.noneOf(ElementalType.class);
        this.bonuses = new EnumMap<>(ModifierType.class);
        this.concentrationBlocked = true;
    }

    /**
     * Grito Inspirador's copy for an ally: every bonus, mode and drawback this Frenesi carries now,
     * for rounds Rodadas — but not the compulsion or Fanático's state, which answer to the Gigante's
     * own Autocontrole, nor the Cataclismo's elements' damage (a clause of the Gigante's own Magias
     * and Gritos).
     */
    public Frenzy inspiredCopy(final int rounds) {
        Frenzy copy = new Frenzy(holderId, rounds, true);
        copy.modes.addAll(modes);
        copy.modes.remove(FrenzyMode.CATACLISMO_ELEMENTAL);
        copy.bonuses.putAll(bonuses);
        copy.concentrationBlocked = concentrationBlocked;
        copy.scornsDamage = scornsDamage;
        return copy;
    }

    /** How much this Frenesi adds to type right now — read by {@link CombatantSheet#getTemporaryBonus}. */
    public int bonusFor(final ModifierType type) {
        return bonuses.getOrDefault(type, 0);
    }

    /** Every bonus this Frenesi carries, for a caller describing it. */
    public Map<ModifierType, Integer> getBonuses() {
        return Collections.unmodifiableMap(bonuses);
    }

    /** Adds value to type's bonus — a mode switched on, or a Berserker stack. */
    public void addBonus(@NonNull final ModifierType type, final int value) {
        bonuses.merge(type, value, Integer::sum);
    }

    public boolean hasMode(final FrenzyMode mode) {
        return modes.contains(mode);
    }

    public Set<FrenzyMode> getModes() {
        return Collections.unmodifiableSet(modes);
    }

    public void addMode(@NonNull final FrenzyMode mode) {
        modes.add(mode);
    }

    public Set<ElementalType> getCataclysmElements() {
        return Collections.unmodifiableSet(cataclysmElements);
    }

    public void setCataclysmElements(@NonNull final Set<ElementalType> elements) {
        cataclysmElements.clear();
        cataclysmElements.addAll(elements);
    }

    /** Titã Enlouquecido: "você não perde totalmente a capacidade de raciocínio". */
    public void liftConcentrationBlock() {
        concentrationBlocked = false;
    }

    public void markScornsDamage() {
        scornsDamage = true;
    }

    public void markFanatic() {
        fanatic = true;
    }

    /** Records points spent on a Frenesi effect; returns the running total. */
    public int recordAutocontroleSpent(final int points) {
        autocontroleSpent += points;
        return autocontroleSpent;
    }

    /** Prolongar Descontrole's "+2 Rodadas". */
    public void extend(final int rounds) {
        shortenTo(getRemainingRounds() + rounds);
    }

    /** The holder's own Frenesi and an inspired copy never displace each other. */
    @Override
    int maximumSimultaneous() {
        return 1;
    }

    @Override
    Object stackingKey() {
        return inspired;
    }
}
