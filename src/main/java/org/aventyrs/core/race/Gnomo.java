package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines what the Gnomos race can do under each rule-set. Three of this race's traits are
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Carisma), the choosable
 * {@link #getChoosableAttributeBonusPoints()}/{@link #getChoosableAttributes()} pair (+1 more,
 * Destreza or Foco — same mechanism {@code Elfos} already uses for its own choosable point),
 * and {@link #generateEmptyCharacter} seeding {@link SizeCategory#MINUS_ONE} — everything else
 * needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Idiomas</b> (Continental + Arcano/dialeto Feérico) — same "no Language/Idioma
 *   concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~250 anos) — same "no age/lifespan concept" gap as every other
 *   race; purely narrative today.</li>
 *   <li><b>Especialização adicional em até 3 Perícias diferentes</b> — same "{@link Race} has
 *   no hook for granting starting Perícia training" gap as every other race's free
 *   Especializações.</li>
 *   <li><b>Domínio de Conhecimento</b> (a Habilidade de Competência from each of the 3
 *   Perícias trained via the racial benefit above) — same shape and gap as {@code Elfos}'
 *   Origem Mística/{@code Anao}' Pequenos Gigantes/Humanos' Prodígios (a race-granted *extra*
 *   acquisition slot), here three slots at once, tied to whichever 3 Perícias were chosen for
 *   the Especialização benefit — compounding the "no persisted record of creation-time Perícia
 *   choices" gap those already cite.</li>
 *   <li><b>Aprendizado Rápido</b> ("duas Perícias Treinadas por benefício Racial" get their
 *   2nd/3rd Graduação upgrade at -0.5 EXP) — same gap as Humanos'/{@code Pequenino}'s own
 *   Aprendizado Rápido ({@code SkillGraduationService#getUpgradeCost} takes no {@link Race},
 *   no persisted creation-time Perícia-pair choice exists). Worth flagging on its own: this
 *   text says *duas* (two) Perícias, while Gnomos' own "Perícias e Talentos"/Domínio de
 *   Conhecimento above grant benefits across *três* (three) — an inconsistency in the source
 *   text itself, not resolved here; whichever count is correct, the underlying gap is
 *   identical either way.</li>
 *   <li><b>Sangue de Fada</b> (may acquire exactly 1 Talento Feérico; doing so after creation
 *   costs 2.5 EXP instead of 3) — three separate gaps at once: {@link
 *   #getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code int}, the
 *   same int-vs-fractional mismatch already flagged on {@code Elfos}'/Humanos'/{@code
 *   Pequenino}'s own Talento-discount traits; "Talento Feérico" doesn't match any existing
 *   {@code org.aventyrs.core.feat.FeatCategory} constant (the existing {@code Type.RACIAL}
 *   ones are {@code MONSTRUOSO}/{@code FERRICO}/{@code ELFICO}/{@code BESTIAL} — flagged, not
 *   guessed, same "get the source text before modeling" discipline as {@code Range}'s own
 *   history); and there's no mechanism for a race to grant eligibility for exactly N Talentos
 *   from a restricted category in the first place.</li>
 *   <li><b>Mente Veloz, Sorriso Veloz</b> (Vantagem on the first Carisma-based Perícia roll
 *   each Cena) — needs Cena-scoped roll-history tracking this core doesn't have (a step
 *   further than {@code Pequenino}'s own Turn-scoped "which Perícias were already rolled"
 *   gap — this one needs to remember across an entire Cena, and reset the count when a new
 *   Cena starts, neither of which this core tracks), and, like {@code Gigantes}' Cuidado para
 *   não Quebrar, spans every {@code SkillType} governed by Carisma rather than naming one, so
 *   it doesn't fit {@code SkillCompetencyAbility#getSkillType()}'s shape either way.</li>
 *   <li><b>Criatividade Superior</b> (trained in Profissões grants an extra Especialização or
 *   Habilidade de Competência) — same shape and gap as Humanos' own Ofício Hereditário, here
 *   scoped to just Profissão rather than Profissão-or-Conhecimentos.</li>
 * </ul>
 *
 * <p>None of the five racial traits above fit {@code SkillCompetencyAbility}'s shape well
 * enough today to catalog in a {@code GnomosRacialAbility} enum (same reasoning as {@code
 * Gigantes}/{@code Pequenino}/Humanos) — so {@link #getRacialAbilities()} is left at {@link
 * Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente são Neutros" is advisory, not a hard rule.
 */
public class Gnomo implements Race {

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.CHARISMA, 1);
    }

    @Override
    public int getChoosableAttributeBonusPoints() {
        return 1;
    }

    @Override
    public Set<AttributeDomain> getChoosableAttributes() {
        return Set.of(AttributeDomain.DEXTERITY, AttributeDomain.FOCUS);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(SizeCategory.MINUS_ONE);
    }
}
