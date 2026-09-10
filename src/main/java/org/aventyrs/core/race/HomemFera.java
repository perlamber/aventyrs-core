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
import java.util.Set;

/**
 * Defines what the Homens-Fera race can do under each rule-set. Like {@code Aviano}/{@code Ogro}/
 * {@code Indomito}, a Homem-Fera carries one acquisition-time choice — which {@link
 * EspiritoAnimal} manifested in it — held as a constructor field.
 *
 * <p>Two of this race's traits are mechanically real today: {@link #getFixedAttributeBonuses()}
 * (+1 Instinto, the only bonus that is not form-gated) and {@link #getRacialAbilities()}'s {@link
 * HomensFeraRacialAbility#FORTALECIMENTO_FERAL} (+1UD Movimento Base). Categoria de Tamanho 0
 * needs no override — {@link org.aventyrs.core.character.SizeCategory#ZERO} is already {@link
 * Character}'s own default.
 *
 * <p><b>{@link #getCreatureType()} is {@link CreatureType#HUMANOIDE}, and that is a judgment
 * call</b> — flagged rather than silently assumed, the same way {@code Gorgona}'s own is. The
 * rules text opens by calling them "humanoides que cultuavam Gaea", says every member "nasce e
 * cresce como membro de uma raça humanoide", and Monstro Interior says they are affected by
 * silver weapons "<i>como se fossem</i> monstros" — which only reads if they are not. Their
 * Talento category ({@code FeatCategory#FERAL}) settles nothing either way. The competing reading
 * is that the origin race varies (the idioma clause offers Continental, Arcano feérico, Silvestre
 * or Élfico, implying a Feérico or Élfico upbringing), which would make this a per-character
 * value rather than a per-race one — but no clause asks the player to <i>choose</i> a parent race
 * the way a Mestiço's does, so it is not modeled as one.
 *
 * <p>Everything else needs a system this core doesn't have yet, and almost all of it hangs off
 * one missing piece — <b>the Forma</b>:
 * <ul>
 *   <li><b>Forma Híbrida</b> (3PA and 3PM to switch between Humanoide, Animal, Híbrida — Aventyrs
 *   only — and Monstruosa — GranAventyrs only; Duração 2 + metade do Instinto Rodadas, +metade do
 *   Vigor at night, doubled under a full moon) — the whole mechanism is absent. There is no form
 *   state, no "spend a resource to enter a timed state" transaction (the same gap {@code
 *   Gorgona}'s Monstros em pele de Fada and {@code Aviano}'s Braços Alados both cite), and no
 *   time-of-day or lunar-phase concept for the two Duração multipliers to read. The
 *   Aventyr/GranAventyr gating <i>is</i> expressible ({@code Character#getAllTitles()}), which
 *   makes the form state the single blocker.</li>
 *   <li><b>The form-scaled Atributo bonuses</b> (+1 Bônus Variável in one of the pair while in
 *   Forma Animal, +1 in both while Híbrida, +2 in both while Monstruosa) — blocked on the same
 *   form state, and on a second missing piece: {@code AttributeValue}'s {@code variable}
 *   component is permanent and is never summed through a {@code ModifierType}, so even a known
 *   form has no round-scoped way to raise an Atributo (CLAUDE.md's "Round-scoped Attribute
 *   bonuses" row). The pairs themselves are recorded on each {@link EspiritoAnimal} constant
 *   regardless, as exact authored data.</li>
 *   <li><b>The form-scaled Categoria de Tamanho</b> (+1 while Animal or Híbrida, +2 while
 *   Monstruosa) — {@code ModifierType#SIZE_CATEGORY} and {@code CharacterSizeService} are both
 *   real, so only the form state is missing here.</li>
 *   <li><b>Fortalecimento Feral's other two halves</b> — the doubled Movimento while transformed
 *   (form state again) and the per-{@link EspiritoAnimal} Armamentos Naturais (no weapon catalog
 *   is authored, and nothing marks a weapon as an Arma Natural). See {@link
 *   HomensFeraRacialAbility#FORTALECIMENTO_FERAL} for what is granted meanwhile.</li>
 *   <li><b>Escravos da Devoção</b> (losing every racial trait except Monstro Interior when
 *   straying from Gaea/Flora/Lacerto's dogmas or a patrono Umbral's orders) — {@link
 *   Character#getDeity()} exists, but nothing models a dogma, adherence to one, or straying from
 *   it, and no mechanism anywhere suspends a race's own traits conditionally. {@link
 *   #getRacialAbilities()} is a fixed list read live on every roll, so the <i>revocation</i> would
 *   be expressible the day adherence is trackable — it is the tracking that is absent.</li>
 *   <li><b>Monstro Interior</b> (affected by silver weapons as a monster; damage from silver or
 *   fire recovers only through a Descanso, halved if not a Descanso Verdadeiro) — needs damage to
 *   remember what dealt it, which {@code CombatantSheet#getDamageTaken} is a single figure with no
 *   provenance (the same shape {@code Troll}'s own Fogo clause needs), plus a material
 *   classification on a weapon that {@code Item} has no column for.</li>
 *   <li><b>Idiomas</b> (Continental, Arcano feérico, Silvestre ou Élfico conforme a raça de
 *   origem, + Antecedente) — same "no Language/Idioma concept exists" gap as every other race.</li>
 *   <li><b>Longevidade</b> (~80 anos, o triplo para os criados entre os Elfos) — same "no
 *   age/lifespan concept" gap as every other race.</li>
 *   <li><b>Treinamento em Ataque Corpo-a-Corpo e Atenção com as Especializações Primal e Sentidos
 *   Apurados, mais 2 Talentos</b> — {@link Race} has no hook to grant starting Perícia
 *   training/abilities nor a {@code Feat} at creation, same gap as every other race's free
 *   Talentos/Especializações. Which Talento category the first one draws from depends on the
 *   upbringing ({@code FeatCategory#AVENTYR}-style Gerais among humans, {@code #ELFICO} among
 *   elves, {@code #FEERICO} among feéricos), a further per-character variation with nowhere to
 *   live.</li>
 * </ul>
 *
 * <p>Tendência is deliberately left unconstrained, same treatment as every other race —
 * "normalmente Neutros" is advisory, not a hard rule.
 */
@Getter
public class HomemFera implements Race {

    /**
     * The animal spirits a Homem-Fera manifests, and the sub-race each defines.
     *
     * <p><b>There are seven, and the source document says six.</b> Its own prose reads "foram
     * abençoados/amaldiçoados com seis diferentes espíritos animais" and then lists seven, each
     * with its own paragraph, its own Senhor da Umbra, its own Atributo pair in the bonus table
     * and its own row in the Armamentos Naturais table. The list is transcribed as written; the
     * count in the sentence is the defect, and is left uncorrected here rather than resolved by
     * dropping one.
     *
     * <p>The prose names the first sub-race <i>Licantropos</i> while both tables name it
     * <i>Lobisomens</i>. Same lineage, two names; {@link #LICANTROPO} keeps the prose's, since
     * that is where the lineage is actually described (Lobisomem is one of its three forms).
     *
     * <p>{@code attributePair} has no consumer yet: its bonuses only apply while transformed, and
     * no form state exists. Recorded anyway, per this codebase's "can't apply it yet doesn't mean
     * can't compute it yet" discipline. The Armamentos Naturais column is deliberately <i>not</i>
     * recorded alongside it — an Atributo pair is expressible with types that exist today, while
     * a natural weapon has no type to be authored as at all.
     */
    @Getter
    @AllArgsConstructor
    public enum EspiritoAnimal {

        /** Espírito do Lobo da Montanha, das Chamas Negras. */
        LICANTROPO(Set.of(AttributeDomain.CHARISMA, AttributeDomain.STRENGTH)),

        /** Espírito do Pesadelo, dO Decapitado. */
        CENTAURO(Set.of(AttributeDomain.DEXTERITY, AttributeDomain.VIGOR)),

        /** Espírito do Dragão Cavalo Marinho, da Coruja Afogada. */
        ICTOCENTAURO(Set.of(AttributeDomain.FOCUS, AttributeDomain.VIGOR)),

        /** Espírito da Raposa de Nove Caudas, do Dragão Negro Ying Lung. */
        KITSUNE(Set.of(AttributeDomain.CHARISMA, AttributeDomain.FOCUS)),

        /** Espírito do Morcego Vampiro Gigante, do Muitos Olhos. */
        QUIROPTERA(Set.of(AttributeDomain.CHARISMA, AttributeDomain.DEXTERITY)),

        /** Espírito do Leão Dentes-de-Sabre, da Serpente-Devora-Mundos. */
        WEMIC(Set.of(AttributeDomain.STRENGTH, AttributeDomain.DEXTERITY)),

        /** Espírito do Urso Trovão da Nevasca Interminável, do Tece-Mortes. */
        TORNAT(Set.of(AttributeDomain.STRENGTH, AttributeDomain.FOCUS));

        private final Set<AttributeDomain> attributePair;
    }

    private final EspiritoAnimal espiritoAnimal;

    public HomemFera(@NonNull final EspiritoAnimal espiritoAnimal) {
        this.espiritoAnimal = espiritoAnimal;
    }

    @Override
    public CreatureType getCreatureType() {
        return CreatureType.HUMANOIDE;
    }

    /**
     * +1 Instinto, and nothing else. Every other Atributo bonus this race receives is a Bônus
     * Variável that applies only while transformed — see {@link EspiritoAnimal#getAttributePair()}
     * and this class's own javadoc for why none of those is granted here.
     */
    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.INSTINCT, 1);
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder();
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(HomensFeraRacialAbility.FORTALECIMENTO_FERAL);
    }
}
