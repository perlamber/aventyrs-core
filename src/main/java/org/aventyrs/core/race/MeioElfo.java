package org.aventyrs.core.race;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_INHERITED_RACIAL_ABILITIES;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_PARENT_RACE;

/**
 * Defines what the Meio-Elfo (half-elf) race can do under each rule-set — the first Mestiço
 * (mixed-blood) race modeled in this core. Unlike every prior race, a Meio-Elfo isn't
 * stateless: the player picks a second, already-existing, non-Mestiço {@link Race} at creation
 * ("seu parente não-élfico"), and this class carries that choice as a constructor field — the
 * same "instance-based class carrying an acquisition-time choice" shape already established by
 * {@code org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}, not {@code AcquiredChoice}
 * (that pattern is for a choice consumed by some *other* mechanism; here the choice feeds this
 * object's own {@link #getFixedAttributeBonuses()}/{@link #getCreatureType()}/
 * {@link #generateEmptyCharacter}). This needs zero changes to {@code
 * CharacterCreationServiceImpl} or the creation-flow steps in {@code package-info.java} — Step
 * 1 ("Pick a Race") just becomes {@code new MeioElfo(parentRace)} instead of {@code new
 * Human()}.
 *
 * <p>Four of this race's traits are mechanically real today:
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Destreza always, plus +1 more on
 *   {@code chosenInheritedAttribute} if the player picked one for Atributo Herdado (validated
 *   in the constructor against {@code parentRace.getFixedAttributeBonuses().keySet()}).</li>
 *   <li><b>{@link #getCreatureType()}</b> — fixed {@link CreatureType#HUMANOIDE}, since the
 *   constructor only accepts a Humanoide {@code parentRace} in the first place.</li>
 *   <li><b>{@link #generateEmptyCharacter}</b> — Categoria de Tamanho inherited from {@code
 *   parentRace.getBaseSizeCategory()} ("não herdam o tamanho de seus pais elfos, mas sim de seu
 *   parente não-élfico"), no offset.</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — "Mestiço Humanoide" (2 Características Raciais
 *   aleatórias da raça escolhida) is <em>partially</em> real, not a blocked TODO: this core
 *   deliberately never rolls dice itself ({@code SkillRoll} accepts an already-rolled result
 *   rather than rolling it) — the same discipline applies here. The constructor accepts up to 2
 *   already-externally-resolved {@code SkillCompetencyAbility} entries (validated to actually
 *   belong to {@code parentRace.getRacialAbilities()}), and returns them here. Because {@code
 *   AbstractSkillInteraction.allSkillCompetencyAbilities} already concatenates {@code
 *   character.getRace().getRacialAbilities()} into every Perícia roll, an inherited ability
 *   works automatically once captured — no extra wiring needed. What remains genuinely
 *   unreachable: most races today have 0-1 entries in {@code getRacialAbilities()} (only the
 *   mechanically-real subset of each race's fluff-level "Características Raciais" was ever
 *   modeled as data), so "2 aleatórias" in practice usually has fewer than 2 to draw from — that
 *   part is a data-catalog gap, not a randomness gap.</li>
 * </ul>
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Idiomas</b> (idiomas raciais de ambos os pais) — same "no Language/Idioma concept
 *   exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (até um século a mais que o parente não-élfico) — same "no
 *   age/lifespan concept" gap as every other race; purely narrative today.</li>
 *   <li><b>1 Talento Élfico adicional</b> + <b>até 1 Talento adicional do mesmo tipo que o
 *   parente não-élfico recebe</b> (nunca mais de um Talento Élfico do tipo Guardião) — same "no
 *   Feat catalog, no {@code Character.feats} list" gap as every other race's free Talentos;
 *   "Talento Élfico do tipo Guardião" doesn't match any named Talento this core has data for
 *   either.</li>
 *   <li><b>Especialização/Habilidade de Competência adicional</b> (se o parente não-élfico
 *   receber alguma, limitado a 1) — same "{@link Race} has no hook for granting starting
 *   Perícia training" gap as every other race's free Especializações.</li>
 *   <li><b>Aptidão Mágica Além-Élfica</b> (+1 Multiplicador de PM) — {@code
 *   MagicPointsServiceImpl#getManaMultiplier} already sums {@code
 *   ModifierType.MANA_MULTIPLIER} via {@code AttributeAbility}, but {@link Race} has no hook to
 *   grant a fixed {@code AttributeAbility} outright the way {@link #getFixedAttributeBonuses()}
 *   grants Atributo points (same "no hook for granting X at creation" shape as every other
 *   racial-grant gap).</li>
 *   <li><b>Provar Seu Valor</b> (+2 Iniciativa, 1x por sessão, dura 1 Cena) — the
 *   once-per-session half is expressible now ({@code CombatantSheet#consumeOncePerSession},
 *   whose session is the sheet's own lifetime), but the clause still has no <em>trigger</em>
 *   to hang on (a Race grants no {@code Blessing} on any event) and no Cena-scoped duration —
 *   a {@code TemporaryBonus} counts Rodadas, and nothing signals a Cena ending.</li>
 *   <li><b>Conexão com o Mana</b> (Talentos Metamágicos custam 2.5 EXP) — identical trait/name
 *   to {@code Elfo}'s own Conexão com o Mana; same int-vs-fractional mismatch already flagged
 *   there ({@link #getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code
 *   int}).</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Neutros" is advisory, not a hard rule.
 */
@Getter
public class MeioElfo implements Race {

    private static final int MAX_INHERITED_RACIAL_ABILITIES = 2;

    private final Race parentRace;
    private final AttributeDomain chosenInheritedAttribute;
    private final List<SkillCompetencyAbility> inheritedRacialAbilities;

    public MeioElfo(@NonNull final Race parentRace) {
        this(parentRace, null, List.of());
    }

    public MeioElfo(@NonNull final Race parentRace, final AttributeDomain chosenInheritedAttribute,
                     @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities) {
        if (parentRace.isMestico() || parentRace.getCreatureType() != CreatureType.HUMANOIDE) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (chosenInheritedAttribute != null && !parentRace.getFixedAttributeBonuses().containsKey(chosenInheritedAttribute)) {
            throw new IllegalOperationException(INVALID_PARENT_RACE);
        }
        if (inheritedRacialAbilities.size() > MAX_INHERITED_RACIAL_ABILITIES
                || !parentRace.getRacialAbilities().containsAll(inheritedRacialAbilities)) {
            throw new IllegalOperationException(INVALID_INHERITED_RACIAL_ABILITIES);
        }
        this.parentRace = parentRace;
        this.chosenInheritedAttribute = chosenInheritedAttribute;
        this.inheritedRacialAbilities = inheritedRacialAbilities;
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
        bonuses.merge(AttributeDomain.DEXTERITY, 1, Integer::sum);
        if (chosenInheritedAttribute != null) {
            bonuses.merge(chosenInheritedAttribute, 1, Integer::sum);
        }
        return Map.copyOf(bonuses);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(parentRace.getBaseSizeCategory());
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return inheritedRacialAbilities;
    }
}
