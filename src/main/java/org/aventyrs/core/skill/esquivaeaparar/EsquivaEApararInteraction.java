package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.character.services.DefenseService;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemType;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Requests an Esquiva e Aparar Perícia test. See {@link AbstractSkillInteraction} for everything
 * every Perícia's roll has in common; this class holds the two things a <i>defense</i> roll needs
 * on top of that, both reached through its own 4-arg {@link #applyTo(CharacterSheet,
 * SceneContext, SkillRoll, DefenseType)} overload — the same "this roll needs to see the other
 * side of the exchange" shape {@code AtaqueADistanciaInteraction}'s own 4-arg overload
 * established.
 *
 * <ol>
 *   <li><b>The Defesa (DF or DM).</b> A Defesa in this ruleset isn't a passive score an attacker
 *   rolls against — this game's dice are always rolled by the player, so an incoming attack is
 *   resolved as this roll, against the Grau de Dificuldade the attack presents. {@link
 *   DefenseType} therefore selects <i>which pool of bonuses feeds the roll</i>, and {@link
 *   DefenseService} sums it: Defesa-typed ability modifiers, the equipped items' DF/DM columns
 *   and Favores, and the sheet's own {@code DEFESAS}-typed {@code TemporaryBonus}es. See {@code
 *   org.aventyrs.core.combat.AttackReceiver} for the entry point that drives the whole exchange.</li>
 *   <li><b>The armor-Categoria Destreza penalty</b> — see {@link #armorCategoryPenalty}.</li>
 * </ol>
 *
 * <p>Passing {@code defenseType == null} makes the 4-arg overload behave exactly like the 3-arg
 * one, so the shorter inherited overloads (which all delegate down with {@code null}) keep
 * computing a plain, un-typed Esquiva e Aparar test — useful for a roll that isn't resisting an
 * attack at all.
 *
 * <p><b>Note on {@code reachedDifficultyLevel}:</b> {@code super.applyTo} resolves it from the
 * bonus it knew about, so this overload recomputes it after adding its own adjustments — a stale
 * tier would otherwise be reported for exactly the rolls this class exists to handle. {@code
 * AtaqueADistanciaInteraction} has the same staleness for its own {@code attackRollBonus} and is
 * deliberately left alone here; fixing it is its own change.
 */
public class EsquivaEApararInteraction extends AbstractSkillInteraction {

    private final DefenseService defenseService;

    public EsquivaEApararInteraction() {
        this(new DefenseServiceImpl());
    }

    public EsquivaEApararInteraction(final DefenseService defenseService) {
        super(SkillType.ESQUIVA_E_APARAR);
        this.defenseService = defenseService;
    }

    public EsquivaEApararInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        this(characterSkillService, modifierResolver, new DefenseServiceImpl(modifierResolver));
    }

    public EsquivaEApararInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver, final DefenseService defenseService) {
        super(SkillType.ESQUIVA_E_APARAR, characterSkillService, modifierResolver);
        this.defenseService = defenseService;
    }

    /**
     * Same as {@link #applyTo(CharacterSheet, SceneContext, SkillRoll)}, but also given
     * defenseType — which Defesa this roll is resisting with. Adds {@link
     * DefenseService#getTotalDefense(CharacterSheet, DefenseType)} and {@link
     * #armorCategoryPenalty} onto the inherited {@code skillRollBonus}, then recomputes {@code
     * reachedDifficultyLevel} against the adjusted total (preserving the Especialização branch,
     * so a roll naming a held {@code SkillSpecialization} still thresholds against each tier's
     * easier {@code expertValue}).
     *
     * <p>A {@code null} defenseType skips the Defesa half entirely; the armor penalty still
     * applies, since it's a property of what the character is wearing, not of what they're
     * resisting.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final SkillRoll skillRoll, final DefenseType defenseType) {
        InteractionResult result = super.applyTo(target, sceneContext, skillRoll);
        Character character = target.getCharacter();

        int adjustment = defenseType == null ? 0 : defenseService.getTotalDefense(target, defenseType);
        adjustment -= armorCategoryPenalty(character);
        if (adjustment == 0) {
            return result;
        }

        int adjustedBonus = result.getSkillRollBonus() + adjustment;
        InteractionResult.InteractionResultBuilder adjusted = result.toBuilder().skillRollBonus(adjustedBonus);
        if (skillRoll != null) {
            boolean expert = skillRoll.getRequestedAbility() instanceof SkillSpecialization;
            int total = adjustedBonus + skillRoll.getTotal();
            Optional<DifficultyLevel> reached = expert
                    ? DifficultyLevel.reachedByAsExpert(total)
                    : DifficultyLevel.reachedBy(total);
            adjusted.reachedDifficultyLevel(reached.orElse(null));
        }
        return adjusted.build();
    }

    /**
     * How much of the character's Destreza their equipped armor costs them on this roll, as a
     * positive number to subtract: the rules text has an Equipamento's Categoria Natural reduce
     * or zero out Esquiva e Aparar's Destreza contribution — full value with Leve or nothing
     * equipped, <i>half</i> with Média, <i>none</i> with Pesada. {@link
     * EsquivaEApararCompetencyAbility#ENCOURACADO_E_VELOZ} shifts that one bracket lighter (full
     * with Média, half with Pesada).
     *
     * <p>The bracket is the <b>heaviest</b> {@link ItemWeightClass} among equipped items of
     * {@link ItemType#DEFENSIVE} — wearing a Pesada breastplate isn't excused by also wearing
     * light boots, and an offensive item's weight isn't armor at all. Halving rounds down, the
     * same integer division {@code DamageServiceImpl}'s {@code HALF_DAMAGE} stage uses.
     *
     * <p>Simplification worth stating rather than hiding: this always subtracts <b>Destreza</b>,
     * matching the rules text literally. In the edge case where a substitution ({@code
     * PERITO_TEORICO}, an {@code ACROBATA}-style {@code getSubstituteAttributeDomain}) means a
     * different Attribute actually fed the roll, the wrong Attribute's total is subtracted.
     * Resolving which domain governed is private to {@code AbstractSkillInteraction#applyTo},
     * and duplicating that resolution chain here would be worse than the TODO — so:
     * TODO: subtract the Attribute that actually governed this roll, once {@code
     * AbstractSkillInteraction} exposes it, rather than assuming Destreza.
     */
    private int armorCategoryPenalty(final Character character) {
        ItemWeightClass heaviest = heaviestArmorWeightClass(character);
        if (heaviest == ItemWeightClass.LIGHT) {
            return 0;
        }
        boolean lessened = SkillCompetencyAbility.allFor(character)
                .contains(EsquivaEApararCompetencyAbility.ENCOURACADO_E_VELOZ);
        int dexterity = character.getAttributes().getAttribute(AttributeDomain.DEXTERITY).getTotal();
        if (heaviest == ItemWeightClass.MEDIUM) {
            return lessened ? 0 : dexterity / 2;
        }
        return lessened ? dexterity / 2 : dexterity;
    }

    /**
     * The heaviest {@link ItemWeightClass} among character's equipped {@link ItemType#DEFENSIVE}
     * items — {@link ItemWeightClass#LIGHT} when they have none, since wearing nothing costs
     * exactly as much Destreza as wearing Leve armor: nothing.
     */
    private ItemWeightClass heaviestArmorWeightClass(final Character character) {
        ItemWeightClass heaviest = ItemWeightClass.LIGHT;
        for (Item item : character.getEquipment()) {
            if (item.getType() != ItemType.DEFENSIVE) {
                continue;
            }
            if (item.getWeightClass().ordinal() > heaviest.ordinal()) {
                heaviest = item.getWeightClass();
            }
        }
        return heaviest;
    }
}
