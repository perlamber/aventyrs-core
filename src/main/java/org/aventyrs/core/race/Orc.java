package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Orcs race can do under each rule-set. Only one of this race's traits is
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+2 Vigor) — everything else
 * needs a system this core doesn't have yet:
 *
 * <ul>
 *   <li><b>Categoria de Tamanho +0</b> — no override needed, {@link
 *   org.aventyrs.core.character.SizeCategory#ZERO} is already {@link Character}'s own
 *   default; the rules text calling this out explicitly (unlike most races, which are simply
 *   silent about it) doesn't change that it's a no-op here, same treatment as {@code Gigantes}'
 *   "Perícias e Talentos: nenhum".</li>
 *   <li><b>Idiomas</b> (Órquico + um adicional per Antecedente, um deles trocável por
 *   Continental) — same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~50 anos) — same "no age/lifespan concept" gap as every other
 *   race; purely narrative today.</li>
 *   <li><b>1 Talento adicional</b> (entre os Talentos de Sobrevivência) <b>+ 1 Especialização
 *   adicional em até 2 Perícias</b> — same "no Feat catalog, no {@code Character.feats} list,
 *   {@link Race} has no hook for granting starting Perícia training" gap as every other
 *   race's free Talentos/Especializações.</li>
 *   <li><b>Vigor de Epona</b> (+1 Multiplicador de PV; -1 todo Dano Físico Sofrido) — two
 *   separate gaps: the PV-multiplier half would otherwise be a plain {@code
 *   @Modifier(ModifierType.LIFE_MULTIPLIER)} method (exactly {@code
 *   VigorAbility#SOBRE_HUMANO}'s own shape), but {@code
 *   org.aventyrs.core.character.services.HitPointsServiceImpl#getLifeMultiplier} only ever
 *   scans {@code character.getAttributeAbilities()} — not {@code skillCompetencyAbilities},
 *   not unlocked {@code SkillExcellency} tiers, and not {@code
 *   character.getRace().getRacialAbilities()} either, so this racial ability has nowhere to
 *   plug into yet (same "generic scan doesn't include racial abilities" gap {@code
 *   AbstractSkillInteraction} used to have before {@code ElfosRacialAbility#SENTIDOS_ABSOLUTOS}
 *   motivated fixing it there — not attempted here without being asked). The RD half is
 *   scoped to *Dano Físico* specifically, but {@code DamageService} mitigates every damage
 *   type identically ({@link org.aventyrs.core.character.DamageType}'s own javadoc already
 *   flags "nothing in this core resolves per-type resistance/mitigation yet"), so a
 *   type-scoped RD can't be granted without over- or under-reducing the wrong damage types.</li>
 *   <li><b>Força Tectônica</b> (an extra, unrestricted {@code
 *   org.aventyrs.core.ability.StrengthAbility} once Força Base reaches 3) — same gap as {@code
 *   Anao}' Pequenos Gigantes: {@code
 *   org.aventyrs.core.character.services.AttributeAbilityService#getUnlockedAbilitySlots}
 *   already unlocks a slot at base 3, but has no notion of a race-granted *extra* one — this
 *   trait is actually *less* restrictive than Pequenos Gigantes (any {@code StrengthAbility},
 *   not 2 named ones), but the missing "extra slot" concept is identical either way.</li>
 *   <li><b>Placitude Térrea</b> (1 extra temporary Autocontrole point per game session) —
 *   needs two things this core doesn't have: a "game session" concept at all (this core
 *   tracks {@code Scene}/Rodada/Turno, nothing above that), and a temporary-points pool for
 *   Ego values in the first place — {@link org.aventyrs.core.character.EgoValue} only has
 *   {@code base}/{@code variable}, no temporary component (same gap {@code
 *   GnoseAbility#ESTABILIDADE_EMOCIONAL}'s own "1 temporary point in this Ego" already
 *   cites).</li>
 *   <li><b>Herança Celeste</b> (Cura/Defesa/Redução-de-Dano effects of Magias Elementais:
 *   Terra ou Sagrada increased +1 per Título Aventyr Desperto) — needs a Magia entity with an
 *   element/school classification (none exists — see {@code
 *   org.aventyrs.core.magic.SpellCastingService}'s own "No Magia entity/list exists yet"), and
 *   a way to count *awakened* Títulos Aventyr — {@link org.aventyrs.core.character.Title} is
 *   currently just an empty marker interface, with no catalog of concrete Títulos and no
 *   "Desperto" state at all.</li>
 *   <li><b>Agnação Ancestral</b> (spend 3PM outside combat to reduce a Perícia roll's GD by 1
 *   nível, treated as trained and Especialista even if not) — needs a "spend a resource for a
 *   one-time roll effect" transaction this core has no equivalent of (Pontos de Mana are
 *   spent on casting via {@code SpellCastingService}, never on a plain Perícia roll), and a
 *   way to temporarily override a roll as trained/Especialista — {@code
 *   CharacterSkillService}'s trained-vs-untrained lookup has no such override hook.</li>
 * </ul>
 *
 * <p>None of the five racial traits above fit {@code SkillCompetencyAbility}'s shape cleanly
 * enough today to catalog in an {@code OrcsRacialAbility} enum (same reasoning as {@code
 * Gigantes}/{@code Pequenino}/Humanos/{@code Gnomos}) — so {@link #getRacialAbilities()} is
 * left at {@link Race}'s own empty default.
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "majoritariamente Neutros" is advisory, not a hard rule.
 */
public class Orc implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.HUMANOIDE;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.VIGOR, 2);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }
}
