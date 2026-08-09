package org.aventyrs.core.race;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_INHERITED_ATTRIBUTE_ABILITIES;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_INHERITED_RACIAL_ABILITIES;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PARENT_RACE;

/**
 * Shared shape for the 6 Mestiços Elementais (Agástias, Aquan, Colosso, Dólos, Flaminídeo,
 * Invernal) — unlike every other race in this core (each bespoke enough that a shared base
 * would mostly hold empty defaults), these 6 genuinely share identical structure: a {@code
 * parentRace} chosen at creation ("seus parentes mortais"), the same "+2, ou +3 se o parente
 * conceder este Atributo" conditional bonus shape, and the identical Mestiço Mortal/Físico
 * Mortal traits verbatim. A concrete subclass only needs to supply its own attribute pair and
 * Categoria de Tamanho offset — see {@link #getSizeCategoryOffset()}.
 *
 * <h2>Mestiço Mortal — {@link #getRacialAbilities()}</h2>
 * "Você deve escolher uma raça Feérica, Humanoide ou Monstruosa que não seja mestiça, você
 * recebe 2 Características Raciais aleatórias da raça escolhida." Same reasoning as {@code
 * MeioElfo}'s own "Mestiço Humanoide": this core deliberately never rolls dice itself ({@code
 * SkillRoll} accepts an already-rolled result), so the constructor accepts up to 2
 * already-externally-resolved {@code SkillCompetencyAbility} entries (validated against {@code
 * parentRace.getRacialAbilities()}) and returns them here — automatically picked up by every
 * Perícia roll via {@code AbstractSkillInteraction.allSkillCompetencyAbilities}, no extra wiring
 * needed. Most races today have 0-1 real entries to draw from, so "2 aleatórias" in practice
 * rarely has a full pool — a data-catalog gap, not a randomness gap.
 *
 * <h2>Físico Mortal — {@link #getInheritedAttributeAbilities()}</h2>
 * "Caso a raça de seus parentes mortais recebam Atributos Raciais adicionais, você não
 * receberá estes pontos de Atributo, ao invés disso poderá escolher uma Habilidade de Atributo
 * de cada Atributo Racial." Unlike Mestiço Mortal, this is the player's own choice, not random —
 * the constructor accepts already-chosen {@link AttributeAbility} instances (one per Atributo
 * the parent race actually grants, validated via {@code getAttributeDomain()} against {@code
 * parentRace.getFixedAttributeBonuses().keySet()}). Unlike {@link #getRacialAbilities()}, there
 * is no automatic consumer for this list — {@code Character.attributeAbilities} is a plain field
 * set once on the builder, not derived from {@link Race} — so a caller must explicitly read
 * {@link #getInheritedAttributeAbilities()} and pass it into {@code
 * Character.builder().attributeAbilities(...)} themselves. One genuine ambiguity in the rules
 * text is left as a TODO rather than guessed at: "você não receberá estes pontos" reads as if a
 * Mestiço would otherwise inherit the parent race's *own* Atributo bonuses directly — but this
 * design never copies them verbatim (only the "+2 vs +3" conditional check on the Mestiço's own
 * attribute pair reads the parent's map); the clause likely presumes a broader mestiçagem
 * baseline rule not present in this text.
 */
@Getter
public abstract class AbstractMesticoRace implements Race {

    private static final int MAX_INHERITED_RACIAL_ABILITIES = 2;

    private final Race parentRace;
    private final List<SkillCompetencyAbility> inheritedRacialAbilities;
    private final List<AttributeAbility> inheritedAttributeAbilities;

    protected AbstractMesticoRace(@NonNull final Race parentRace,
                                   @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                                   @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        if (parentRace.isMestico()) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (inheritedRacialAbilities.size() > MAX_INHERITED_RACIAL_ABILITIES
                || !parentRace.getRacialAbilities().containsAll(inheritedRacialAbilities)) {
            throw new IllegalOperationException(INVALID_INHERITED_RACIAL_ABILITIES);
        }
        validateInheritedAttributeAbilities(parentRace, inheritedAttributeAbilities);
        this.parentRace = parentRace;
        this.inheritedRacialAbilities = inheritedRacialAbilities;
        this.inheritedAttributeAbilities = inheritedAttributeAbilities;
    }

    private static void validateInheritedAttributeAbilities(final Race parentRace,
                                                              final List<AttributeAbility> inheritedAttributeAbilities) {
        final Set<AttributeDomain> alreadyChosen = new HashSet<>();
        for (final AttributeAbility ability : inheritedAttributeAbilities) {
            final AttributeDomain domain = ability.getAttributeDomain();
            if (!parentRace.getFixedAttributeBonuses().containsKey(domain) || !alreadyChosen.add(domain)) {
                throw new IllegalOperationException(INVALID_INHERITED_ATTRIBUTE_ABILITIES);
            }
        }
    }

    /** Whether {@code parentRace} grants a racial bonus on this Atributo — the "+2, ou +3" check. */
    protected boolean parentGrants(final AttributeDomain domain) {
        return parentRace.getFixedAttributeBonuses().containsKey(domain);
    }

    @Override
    public boolean isMestico() {
        return true;
    }

    /**
     * Delegates to {@code parentRace.getCreatureType()} — the rules text's own words, repeated
     * on every one of the 6 races: "Para critérios de pré-requisitos, [Raça] podem ser
     * considerados Feéricos, Humanoides ou Monstruosos, conforme sua metade não-elemental."
     */
    @Override
    public CreatureType getCreatureType() {
        return parentRace.getCreatureType();
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return inheritedRacialAbilities;
    }

    @Override
    public final Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(parentRace.getBaseSizeCategory().shift(getSizeCategoryOffset()));
    }

    /**
     * The fixed offset this race applies to {@code parentRace.getBaseSizeCategory()} — e.g. 0
     * for Agástias/Aquan/Flaminídeo, +1 for Colosso/Invernal, -1 for Dólos.
     */
    protected abstract int getSizeCategoryOffset();
}
