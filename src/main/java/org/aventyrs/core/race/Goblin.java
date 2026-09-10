package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines what the Goblins race can do under each rule-set — a stateless race, like {@code
 * Anao}/{@code Elfo}. Five of its traits are mechanically real today:
 *
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Destreza, "devido a sua habilidade
 *   natural".</li>
 *   <li><b>{@link #getChoosableAttributeBonusPoints()}/{@link #getChoosableAttributes()}</b> — 1
 *   further point, "de Foco ou Gnose, a escolha do jogador". Exactly {@code Elfo}'s own shape,
 *   validated at creation by {@code CharacterCreationServiceImpl.allocateAttributes}.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#MONSTRUOSO}; the rules text calls
 *   Goblins "a menor raça monstruosa inteligente".</li>
 *   <li><b>{@link #getBaseSizeCategory()}/{@link #generateEmptyCharacter}</b> — {@link
 *   SizeCategory#MINUS_ONE}.</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — both halves of Poder dos Números and the
 *   Desvantagem half of Autodesconfiança em Combate, in {@link GoblinsRacialAbility}. That enum's
 *   own javadoc explains why the two constants can never fire at once, and which narrowings of
 *   each are still deferred.</li>
 * </ul>
 *
 * <p>Everything else needs a system this core doesn't have yet:
 * <ul>
 *   <li><b>Aprendizado Rápido</b> (2 Perícias Treinadas chosen at creation get their 2nd and 3rd
 *   Graduação at -0.5 EXP) — identical trait, name and gap to {@code Human}'s own Aprendizado
 *   Rápido: {@code SkillGraduationService#getUpgradeCost} takes no {@link Race} and has no notion
 *   of a race-specific discount, and nothing records <i>which</i> 2 Perícias were chosen at
 *   creation for the discount to scope itself to. The 0.5 is additionally fractional, though
 *   unlike {@link #getNewFeatCost} that particular half is not a blocker — Graduação cost is
 *   already a {@code BigDecimal}.</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core, same gap {@code
 *   Anao}'s/{@code Elfo}'s own cite.</li>
 *   <li><b>Criatividade Superior</b> (an extra Especialização or Habilidade de Competência when
 *   trained in Profissões) — same "grant an extra acquisition slot" shape and gap as {@code
 *   Elfo}'s Origem Mística and {@code Anao}'s Pequenos Gigantes, here conditioned on being
 *   trained in one named Perícia rather than on an Atributo base.</li>
 *   <li><b>Idiomas</b> (Goblinóide + um adicional per Antecedente) — same "no Language/Idioma
 *   concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (maioridade aos 12, ~35 anos) — same "no age/lifespan concept" gap as
 *   every other race; purely narrative today.</li>
 *   <li><b>Uma Especialização ou Habilidade de Competência em até duas Perícias (Furtividade e
 *   uma qualquer) + 1 Talento adicional</b> (entre {@code
 *   org.aventyrs.core.feat.FeatCategory#MOBILIDADE}, {@code #PERITO} and {@code #ASSASSINO}) —
 *   {@link Race} has no hook to grant starting Perícia training/abilities nor a {@code Feat} at
 *   creation, same gap as every other race's free Talentos/Especializações.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race — "é
 * improvável encontrar um Samaritano entre eles" is advisory, not a hard rule, and is exactly the
 * kind of "raramente" wording {@code Anao}'s own javadoc already declines to enforce.
 */
public class Goblin implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.DEXTERITY, 1);
    }

    @Override
    public int getChoosableAttributeBonusPoints() {
        return 1;
    }

    @Override
    public Set<AttributeDomain> getChoosableAttributes() {
        return Set.of(AttributeDomain.FOCUS, AttributeDomain.GNOSE);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return SizeCategory.MINUS_ONE;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(GoblinsRacialAbility.PODER_DOS_NUMEROS, GoblinsRacialAbility.AUTODESCONFIANCA_EM_COMBATE);
    }
}
