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
 * Defines what the Elfos race can do under each rule-set. Three of this race's traits are
 * mechanically real today — {@link #getFixedAttributeBonuses()} (+1 Destreza), the choosable
 * {@link #getChoosableAttributeBonusPoints()}/{@link #getChoosableAttributes()} pair (+1 more,
 * Foco or Gnose — the existing choosable-bonus mechanism {@code
 * org.aventyrs.core.character.services.CharacterCreationServiceImpl#allocateAttributes}
 * already validates against, unlike Anões' bonus, which is entirely
 * fixed), and {@link #getRacialAbilities()} granting {@link
 * ElfosRacialAbility#SENTIDOS_ABSOLUTOS} — everything else needs a system this core doesn't
 * have yet, same discipline as {@code Anao}:
 *
 * <ul>
 *   <li><b>Categoria de Tamanho 0</b> — no override needed, {@link SizeCategory#ZERO} is
 *   already {@link Character}'s own default.</li>
 *   <li><b>Idiomas</b> (Élfico + reino natal ou Continental) — same "no Language/Idioma
 *   concept exists" gap as {@code Anao}.</li>
 *   <li><b>Longevidade</b> (500+ anos, slow physical/social maturing) — same "no age/lifespan
 *   concept" gap as {@code Anao}; purely narrative today.</li>
 *   <li><b>1 Talento Racial Élfico + 1 Talento</b> (chosen among Élfico, Arqueirismo — this
 *   rules text's own naming doesn't match any existing {@code
 *   org.aventyrs.core.feat.FeatCategory} constant; the race's own earlier flavor text says
 *   "técnicas únicas de Artilharia" instead, so this may be the same category under a
 *   different name in this revision of the text, but that's an inference, not confirmed —
 *   flagging it rather than guessing silently, the same "get the source text before modeling"
 *   lesson {@code Range}'s own history already taught this codebase — Duelista or
 *   Metamágico) — same "no Feat catalog, no {@code Character.feats} list" gap as Anões' free
 *   Talento.</li>
 *   <li><b>Especialização adicional em Perícia</b> (Conhecimentos restrito a Metamágico ou
 *   Natureza, "Foco", ou uma Perícia de Ataque) — same "{@link Race} has no hook for granting
 *   starting Perícia training" gap as Anões' Profissão training; "Foco" here doesn't match any
 *   known Perícia or Especialização in this codebase either (it's not {@code
 *   org.aventyrs.core.skill.attention.AttentionSpecialization}, which has no such constant) —
 *   flagged, not guessed.</li>
 *   <li><b>Origem mística</b> (a starting Habilidade de Competência of the player's choice,
 *   from Conhecimentos or Domínio do Mana, gated on being trained in that Perícia) — the
 *   *ability itself* is already fully expressible today (it's an ordinary {@code
 *   ConhecimentosCompetencyAbility}/{@code DominioDoManaCompetencyAbility} constant, just like
 *   any acquired one), but *granting an extra acquisition slot restricted to two named
 *   Perícias' catalogs* isn't — same shape, and same gap, as Anões' Pequenos Gigantes (no
 *   notion of a race-granted extra slot, nor of restricting a slot's choice to a named
 *   subset). The "apenas se forem treinados" condition itself is deliberately left unenforced,
 *   same restraint as every other unenforced acquisition prerequisite in this codebase.</li>
 *   <li><b>Visão no escuro</b> (monochromatic darkvision) — same "no vision/senses concept"
 *   gap as Anões.</li>
 *   <li><b>Linhagem Feérica</b> (+2 DM racial bonus, +5 vs. Encantamentos) — the flat +2 half
 *   is now expressible: {@code ModifierType#MAGIC_DEFENSE} exists and {@code
 *   character.services.DefenseService} sums it across racial abilities like any other. What it
 *   still lacks is somewhere to hang it — {@code Elfo} has no {@code *RacialAbility} enum, and
 *   one exists only once a trait's shape genuinely fits (it now does). The narrower "+5 apenas
 *   contra Encantamentos" case stays blocked on this codebase's familiar "doesn't track what a
 *   roll/effect is *for*" gap.</li>
 *   <li><b>Conexão com o Mana</b> (Talentos Metamágicos cost 2.5 EXP instead of 3) — {@link
 *   Race#getNewFeatCost(org.aventyrs.core.feat.FeatCategory)} returns a plain {@code int},
 *   which can't represent a genuinely fractional 2.5 — the same int-vs-fractional mismatch
 *   already flagged for {@code SkillGraduationService}'s Graduação-upgrade cost (a real {@code
 *   BigDecimal} there, per CLAUDE.md's "Attribute base/Perícia Graduação" section). Changing
 *   {@code Race#getNewFeatCost}'s return type would ripple to every implementor/caller, so
 *   that's deliberately not attempted just to close this one gap.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as Anões: "normalmente"
 * bondosos ou neutros is advisory, not a hard rule.
 */
public class Elfo implements Race {

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.HUMANOIDE;
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
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS);
    }
}
