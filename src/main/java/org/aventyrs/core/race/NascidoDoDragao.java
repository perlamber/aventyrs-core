package org.aventyrs.core.race;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_ELEMENTAL_LINEAGE;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_INHERITED_RACIAL_ABILITIES;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PARENT_RACE;

/**
 * Defines what the Nascidos do Dragão (Dracônicos) race can do under each rule-set — the second
 * bespoke Mestiço after {@code MeioElfo}, and modeled on it directly: "Mestiço Humanoide"
 * ("durante a criação do personagem você deve escolher uma raça Humanoide que não seja mestiça,
 * você recebe 2 Características Raciais aleatórias da raça escolhida") is word-for-word that
 * race's own clause, and "Atributo Herdado" is its Atributo Herdado. It is deliberately <b>not</b>
 * an {@code AbstractMesticoRace}: that base is the shared shape of the 6 Mestiços Elementais
 * (parent-conditional "+2, ou +3" bonus, Físico Mortal, a size <i>offset</i> off a fixed
 * attribute pair), and a Nascido do Dragão shares none of it — its Atributos are flat, and its
 * Atributo Herdado is a +1 on a parent-granted Atributo rather than an inherited {@code
 * AttributeAbility}.
 *
 * <p>Six of this race's traits are mechanically real today:
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Força and +1 Foco always, plus +1 more on
 *   {@code chosenInheritedAttribute} if the player picked one for Atributo Herdado (validated in
 *   the constructor against {@code parentRace.getFixedAttributeBonuses().keySet()}, so a parent
 *   that grants no Atributo Racial simply has nothing to inherit). Merged additively, so picking
 *   Força off a Força-granting parent reads as +2, not two entries.</li>
 *   <li><b>{@link #getCreatureType()}</b> — fixed {@link CreatureType#HUMANOIDE}, since the
 *   constructor only accepts a Humanoide {@code parentRace} in the first place, exactly like
 *   {@code MeioElfo}.</li>
 *   <li><b>{@link #getBaseSizeCategory()}/{@link #generateEmptyCharacter}</b> — "estando uma
 *   Categoria de Tamanho acima" do parente não-dracônico, i.e. {@code
 *   parentRace.getBaseSizeCategory().shift(+1)}. Unlike {@code AbstractMesticoRace}'s offset,
 *   this one is reported through {@link #getBaseSizeCategory()} too, so a caller can read it
 *   without assembling a whole {@link Character}.</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — Mestiço Humanoide, resolved the same partially-real
 *   way {@code MeioElfo}'s own is: this core never rolls dice, so the constructor accepts up to 2
 *   <i>already-externally-resolved</i> abilities (validated to actually belong to {@code
 *   parentRace.getRacialAbilities()}), and every Perícia roll picks them up automatically via
 *   {@code AbstractSkillInteraction.allSkillCompetencyAbilities}. "2 aleatórias" usually has
 *   fewer than 2 to draw from — a data-catalog gap, not a randomness gap.</li>
 *   <li><b>Armadura Dracônica</b> ("seu Multiplicador de PV é aumentado em +1") — seeded by
 *   {@link #generateEmptyCharacter} as {@code HitPointsService.DEFAULT_LIFE_MULTIPLIER + 1},
 *   which {@code HitPointsServiceImpl#getLifeMultiplier} reads as its base before summing any
 *   {@code LIFE_MULTIPLIER} modifier. Same "seed it on the builder" path as Categoria de Tamanho,
 *   and equally real — {@link Character#getLifeMultiplier()} is an ordinary builder field.</li>
 *   <li><b>Escamas Cromática</b>, <i>half</i> of it — the chosen {@link ElementalType} and the
 *   element it is vulnerable to are exact authored data, held as {@link #getElementalLineage()}
 *   and derived by {@link #getOpposedElementalType()} off the rules text's own 8-row table. Per
 *   this codebase's "can't apply it yet doesn't mean can't compute it yet" discipline, the
 *   arithmetic is real and only the <i>application</i> is deferred (next list).</li>
 * </ul>
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Escamas Cromática's effect</b> (Resistência Elemental to {@link
 *   #getElementalLineage()}, vulnerability to {@link #getOpposedElementalType()}) — RE is not a
 *   stat this core computes at all, and there is no vulnerability/damage-type-scoped mitigation
 *   mechanism either ({@code DamageType} has no elemental breakdown feeding RD/RA — the same gap
 *   {@code Zumbi}'s "imune a Profanos/Naturais, -3 vs Esmagamento" cites). The <i>pair</i> is
 *   modeled; neither side of it reaches a damage calculation.</li>
 *   <li><b>Magia Dracônica</b> (mimetizar Magias Elementais spending PD in place of PM, at a
 *   {@code org.aventyrs.core.magic.BranchLevel} that climbs with each Título Aventyr Desperto:
 *   Semente/Broto at none, Muda at one, Emergente at two, Florescente at three; such casts may
 *   not have their cost reduced by Talentos or Habilidades) — the depth ladder itself would be a
 *   pure derivation off {@code Character#getAllTitles()}, but there is no mimicry concept at all
 *   to hang it on: {@code SpellCastingService#castSpell} has no PM cost step to redirect to PD,
 *   no notion of casting a Magia the caster does not know, and no cost-reduction stage for the
 *   "não podem ter seus custos reduzidos" clause to exempt itself from. A mechanism with no entry
 *   point, not a formula waiting for a reader.</li>
 *   <li><b>Idiomas</b> (o idioma dos pais; Dracônico quando criados por Dragões) — same "no
 *   Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (o triplo do parente não-dracônico, com o dobro do tempo até a
 *   maturidade) — same "no age/lifespan concept" gap as every other race; purely narrative
 *   today.</li>
 *   <li><b>2 Talentos adicionais</b> (entre {@code org.aventyrs.core.feat.FeatCategory#DRACONICO},
 *   {@code #MONSTRUOSO} and {@code #MESTICO}) — all three categories exist and the Talento catalog
 *   is real now, but {@link Race} has no hook to grant a {@code Feat} at creation ({@code
 *   FeatService#grantFeat} spends XP and validates prerequisites, which a free racial grant does
 *   neither of), same gap as every other race's free Talentos.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race — the rules
 * text's urban-versus-covil skew is advisory, not a hard rule.
 */
@Getter
public class NascidoDoDragao implements Race {

    private static final int MAX_INHERITED_RACIAL_ABILITIES = 2;
    private static final int SIZE_CATEGORY_OFFSET = 1;
    private static final int ARMADURA_DRACONICA_LIFE_MULTIPLIER_BONUS = 1;

    /**
     * Escamas Cromática's own table, verbatim: each of the 8 Elementos Base paired with the one
     * its scales are vulnerable to. Symmetric in both directions, so a single map covers both
     * halves of every row.
     *
     * <p>Kept private to this race rather than promoted to a {@code getOpposite()} on {@link
     * ElementalType} itself: this is the only rules text in the codebase that states an
     * opposition, so promoting it would be building a shared mechanism for its first consumer.
     *
     * <p>Two naming notes on the source table. It writes the fourth row's element as <i>Vento</i>
     * where the third writes <i>Ar</i> for the same element — {@link ElementalType#AR} covers
     * both. And {@link ElementalType#NATURAL} appears here as an Elemento Base while also being a
     * {@code MagicType}; that ambiguity is the source document's own and is deliberately left
     * unresolved (see {@code ElementalType}'s javadoc).
     */
    private static final Map<ElementalType, ElementalType> OPPOSED_ELEMENTS = Map.of(
            ElementalType.FOGO, ElementalType.AGUA,
            ElementalType.AGUA, ElementalType.FOGO,
            ElementalType.MAGMA, ElementalType.GELO,
            ElementalType.GELO, ElementalType.MAGMA,
            ElementalType.TERRA, ElementalType.AR,
            ElementalType.AR, ElementalType.TERRA,
            ElementalType.NATURAL, ElementalType.ELETRICIDADE,
            ElementalType.ELETRICIDADE, ElementalType.NATURAL);

    private final Race parentRace;
    private final ElementalType elementalLineage;
    private final AttributeDomain chosenInheritedAttribute;
    private final List<SkillCompetencyAbility> inheritedRacialAbilities;

    public NascidoDoDragao(@NonNull final Race parentRace, @NonNull final ElementalType elementalLineage) {
        this(parentRace, elementalLineage, null, List.of());
    }

    public NascidoDoDragao(@NonNull final Race parentRace, @NonNull final ElementalType elementalLineage,
                            final AttributeDomain chosenInheritedAttribute,
                            @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities) {
        if (parentRace.isMestico() || parentRace.getCreatureType() != CreatureType.HUMANOIDE) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        // "escolha um dos 8 Elementos para descender" — ElementalType's own TODOS is a Magia
        // catalog convenience ("o Conjurador escolhe o elemento por conjuração"), not one of the
        // 8, and has no row in Escamas Cromática's table. A genuine system boundary, same
        // restraint SkillRoll applies to its dice.
        if (!OPPOSED_ELEMENTS.containsKey(elementalLineage)) {
            throw new IllegalOperationException(INVALID_ELEMENTAL_LINEAGE);
        }
        if (chosenInheritedAttribute != null && !parentRace.getFixedAttributeBonuses().containsKey(chosenInheritedAttribute)) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (inheritedRacialAbilities.size() > MAX_INHERITED_RACIAL_ABILITIES
                || !parentRace.getRacialAbilities().containsAll(inheritedRacialAbilities)) {
            throw new IllegalOperationException(INVALID_INHERITED_RACIAL_ABILITIES);
        }
        this.parentRace = parentRace;
        this.elementalLineage = elementalLineage;
        this.chosenInheritedAttribute = chosenInheritedAttribute;
        this.inheritedRacialAbilities = inheritedRacialAbilities;
    }

    /**
     * The Elemento this Nascido do Dragão's scales are vulnerable to — the other half of Escamas
     * Cromática, derived from {@link #getElementalLineage()} rather than stored, so the two can
     * never disagree. Nothing consumes it yet; see this class's own javadoc for the missing RE/
     * vulnerability mechanism.
     */
    public ElementalType getOpposedElementalType() {
        return OPPOSED_ELEMENTS.get(elementalLineage);
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.HUMANOIDE;
    }

    @Override
    public boolean isMestico() {
        return true;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        final Map<AttributeDomain, Integer> bonuses = new HashMap<>();
        bonuses.merge(AttributeDomain.STRENGTH, 1, Integer::sum);
        bonuses.merge(AttributeDomain.FOCUS, 1, Integer::sum);
        if (chosenInheritedAttribute != null) {
            bonuses.merge(chosenInheritedAttribute, 1, Integer::sum);
        }
        return Map.copyOf(bonuses);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return parentRace.getBaseSizeCategory().shift(SIZE_CATEGORY_OFFSET);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder()
                .sizeCategory(getBaseSizeCategory())
                .lifeMultiplier(HitPointsService.DEFAULT_LIFE_MULTIPLIER + ARMADURA_DRACONICA_LIFE_MULTIPLIER_BONUS);
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return inheritedRacialAbilities;
    }
}
