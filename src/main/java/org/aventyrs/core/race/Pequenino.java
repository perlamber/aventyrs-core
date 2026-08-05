package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Pequenino race can do under each rule-set. Two of this race's traits are
 * mechanically real today — the unconditional half of {@link #getFixedAttributeBonuses()}
 * (+1 Destreza) and {@link #generateEmptyCharacter} seeding {@link SizeCategory#MINUS_ONE} —
 * everything else needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>+1 Instinto (machos) ou +1 Carisma (fêmeas)</b> — the other half of the Atributos
 *   bonus is conditioned on {@link Character.Sexo}, but {@link Race#getFixedAttributeBonuses()}
 *   takes no Character/Sexo parameter at all to condition on, and this isn't just a missing
 *   method parameter: {@code
 *   org.aventyrs.core.character.services.CharacterCreationServiceImpl#allocateAttributes}
 *   calls it to produce the {@code CharacterAttributes} value object *before* the
 *   {@link Character} itself (which is what actually carries {@code sexo}) is assembled — the
 *   data this bonus needs to condition on doesn't exist yet at the point in the creation flow
 *   where fixed racial bonuses are currently resolved.</li>
 *   <li><b>Idiomas</b> (Tom Menor ou dialeto Pequenino, conforme a tribo, + Continental + um
 *   adicional per Antecedente) — same "no Language/Idioma concept exists" gap as every other
 *   race.</li>
 *   <li><b>Longevidade</b> (~70 anos) — same "no age/lifespan concept" gap as every other
 *   race; purely narrative today.</li>
 *   <li><b>1 Talento Geral + 2 Especializações adicionais</b> (divided between 2 chosen
 *   Perícias) — same "no Feat catalog, no {@code Character.feats} list, {@link Race} has no
 *   hook for granting starting Perícia training" gap as every other race's free Talentos/
 *   Especializações.</li>
 *   <li><b>Aprendizado Rápido</b> (2 Perícias chosen at creation get their 2nd/3rd Graduação
 *   upgrade at -0.5 EXP) — identical text to Humanos' own Aprendizado Rápido; same gap: {@code
 *   org.aventyrs.core.character.services.SkillGraduationService#getUpgradeCost} takes no
 *   {@link Race} at all, has no notion of a race-specific discount, and there's no persisted
 *   record of *which* 2 Perícias a character chose at creation for this racial ability to
 *   scope itself to (the {@code AcquiredChoice} mechanism records a single choice per ability
 *   instance, not a pair).</li>
 *   <li><b>Adaptação</b> (Talentos cost 2.5 EXP instead of 3) — identical text to Humanos'/
 *   Elfos' own Adaptação/Conexão com o Mana; same {@link Race#getNewFeatCost(org.aventyrs.core.feat.FeatCategory)}
 *   int-vs-fractional mismatch already flagged on {@code Elfos}.</li>
 *   <li><b>Ligeiro</b> (+2UD ao Movimento Base) and <b>Sempre Veloz</b> (never loses movement
 *   to difficult terrain) — this core has no aggregated "Movimento Base" character stat at
 *   all (unlike Reações/Ações Livres/Pontos de Ação — see CLAUDE.md's "Character-level stats
 *   aggregated from abilities" section — {@link SizeCategory#getMovementPerActionPoint()} is a
 *   pure function of size alone, with no bonus sources feeding into it), nor any terrain-
 *   difficulty concept for Sempre Veloz to cancel.</li>
 *   <li><b>Distraído e Motivado</b> (Desvantagem on a Perícia rolled in consecutive Rodadas;
 *   Vantagem from the 2nd roll of a Turn on, but only when it's a different Perícia than any
 *   rolled earlier that Turn) — needs this core to remember *which Perícias were already
 *   rolled*, this Turn and the previous one; no roll-history/Turn-tracking concept exists
 *   anywhere in this core (a step further than the already-documented "this core doesn't
 *   track what a roll is for" gap — this one needs it to remember what *previous* rolls
 *   were).</li>
 * </ul>
 *
 * <p>None of the five racial traits above fit {@code SkillCompetencyAbility}'s shape well
 * enough today to catalog in a {@code PequeninoRacialAbility} enum (same reasoning as {@code
 * Gigantes}) — so {@link #getRacialAbilities()} is left at {@link Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "são frequentemente bondosos" is advisory, not a hard rule (this race just has none of the
 * "raramente"/malignant-tendency caveats other races' own advisory text carries).
 */
public class Pequenino implements Race {

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.DEXTERITY, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(SizeCategory.MINUS_ONE);
    }
}
