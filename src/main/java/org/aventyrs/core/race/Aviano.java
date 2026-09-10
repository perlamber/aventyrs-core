package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.skill.SkillCompetencyAbility;

import java.util.List;
import java.util.Map;

/**
 * Defines what the Avianos race can do under each rule-set. Like {@code Agastias}' Linhagem, an
 * Aviano carries one acquisition-time choice — its {@link Subtipo}, "escolhido no momento da
 * criação do personagem, feita a escolha não é possível alterá-la" — so this is an instance-based
 * race with a constructor field, the {@code ArtesAprimorarComArteAbility} pattern rather than
 * {@code AcquiredChoice} (the choice feeds this object's own {@link #getFixedAttributeBonuses()}).
 * It is <b>not</b> a Mestiço: nothing about it picks a parent {@link Race}.
 *
 * <p>Three of this race's traits are mechanically real today:
 * <ul>
 *   <li><b>{@link #getFixedAttributeBonuses()}</b> — +1 Destreza for every Aviano, plus the
 *   chosen {@link Subtipo}'s own: +1 Força for a {@link Subtipo#RAPINANTE}, +1 Vigor for a
 *   {@link Subtipo#CORRENUVENS}.</li>
 *   <li><b>{@link #getCreatureType()}</b> — {@link CreatureType#MONSTRUOSO}. The rules text says
 *   so outright ("apesar de serem considerados monstros"), and the Império's hostility "a raças
 *   não humanoides" settles the other direction.</li>
 *   <li><b>{@link #getRacialAbilities()}</b> — {@link
 *   AvianosRacialAbility#VISAO_ALEM_DO_ALCANCE}'s Vantagem em Atenção, with the "percepções
 *   visuais" narrowing documented there as an accepted simplification.</li>
 * </ul>
 *
 * <p>Categoria de Tamanho 0 needs no override — {@link
 * org.aventyrs.core.character.SizeCategory#ZERO} is already {@link Character}'s own default, same
 * treatment as {@code Human}/{@code Orc}/{@code Gorgona}. Everything else needs a system this
 * core doesn't have yet:
 *
 * <ul>
 *   <li><b>Movimento Base while flying</b> (+2UD for a Rapinante, +1UD for a Correnuvens) — the
 *   amount is exact and {@code ModifierType#MOVEMENT} is real, but the <i>condition</i> isn't:
 *   flight is a state this core has no concept of, and a no-arg {@code @Modifier} could not read
 *   it even if it did (see CLAUDE.md's "A no-arg @Modifier method can't see context"). Granting
 *   it flat would raise every Aviano's ground Movimento Base, which the clause plainly doesn't —
 *   unlike {@code HomensFeraRacialAbility#FORTALECIMENTO_FERAL}, whose +1UD half genuinely is
 *   unconditional. So this one is withheld entirely rather than over-granted.</li>
 *   <li><b>Braços Alados</b> (asas in place of arms; both hands count as "membros inábeis para
 *   rolagens de Perícia"; flying in a Cena de Combate costs 1PD and lasts 1d6 + metade da Destreza
 *   Rodadas) — three separate missing pieces: no limb/anatomy concept to mark a member inábil and
 *   no per-Perícia penalty keyed on one, the same missing flight state as above, and no
 *   "spend a resource to enter a timed state" transaction (Pontos de Determinação are spent
 *   through {@code CombatantSheet}, but nothing converts a spend into a {@code TemporaryEffect}
 *   the way this needs — the same gap {@code Gorgona}'s Monstros em pele de Fada cites). This
 *   core also never rolls the 1d6 itself.</li>
 *   <li><b>Pés Hábeis e Poderosos</b> (holding items and performing manual actions with the feet)
 *   — the flip side of Braços Alados' penalty, so it is blocked on the same missing limb concept;
 *   with neither modeled, the pair currently nets out to no mechanical effect at all.</li>
 *   <li><b>Garras Afiadas</b> (the feet count as the Arma Natural "Garras Afiadas") — {@code
 *   org.aventyrs.core.item.Weapon} exists, but no weapon <i>catalog</i> does (only {@code
 *   ArmorItem} is authored), and nothing marks a weapon as an Arma Natural in the first place —
 *   the same two-markers-missing gap CLAUDE.md's "Classifying an attack as Desarmado/Arma
 *   Natural" row names.</li>
 *   <li><b>Visão no Escuro</b> — no vision/senses concept exists in this core, same gap {@code
 *   Anao}'s/{@code Elfo}'s own already cite. Carried in {@link
 *   AvianosRacialAbility#VISAO_ALEM_DO_ALCANCE}'s description text rather than dropped.</li>
 *   <li><b>Idiomas</b> (dialeto Aviano, Continental, e um adicional per Antecedente) — same "no
 *   Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~100 anos) — same "no age/lifespan concept" gap as every other race.</li>
 *   <li><b>1 Talento Geral adicional + Ossos Ocos como Talento adicional + uma Especialização e
 *   uma Habilidade de Competência em até uma mesma Perícia treinada</b> — {@link Race} has no hook
 *   to grant a {@code Feat} at creation nor to grant starting Perícia training/abilities, same gap
 *   as every other race's free Talentos/Especializações. Ossos Ocos is additionally not a Talento
 *   the {@code org.aventyrs.core.feat} catalog authors yet ({@code FeatCategory#AVIANO} has no
 *   enum), so there would be nothing to grant even with a hook.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "costumam ser Neutros" is advisory, not a hard rule.
 */
@Getter
public class Aviano implements Race {

    /**
     * The two Avianos subtypes, each fixing the second half of {@link
     * #getFixedAttributeBonuses()}. A nested enum on the race, following {@code
     * Agastias.Linhagem}'s own shape — the choice space is small, fixed at compile time, and
     * meaningless outside this race.
     *
     * <p>The flight-Movimento figure each subtype's rules text names is deliberately <b>not</b> a
     * field here: it would be authored data no caller can reach, since the flight state that
     * gates it doesn't exist (see this class's own javadoc). Add it alongside the mechanism.
     */
    @Getter
    @AllArgsConstructor
    public enum Subtipo {

        /** "Mais fortes e raramente possuem bicos... favorece voos curtos e rápidos." */
        RAPINANTE(AttributeDomain.STRENGTH),

        /** "Corpos mais resistentes, capazes de voar por horas sem parar." */
        CORRENUVENS(AttributeDomain.VIGOR);

        private final AttributeDomain attributeBonus;
    }

    private final Subtipo subtipo;

    public Aviano(@NonNull final Subtipo subtipo) {
        this.subtipo = subtipo;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.MONSTRUOSO;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.DEXTERITY, 1, subtipo.getAttributeBonus(), 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE);
    }
}
