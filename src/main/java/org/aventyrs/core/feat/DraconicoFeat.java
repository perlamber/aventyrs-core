package org.aventyrs.core.feat;

import java.util.List;

import org.aventyrs.core.character.AttributeDomain;
import java.util.Optional;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.character.Character;
import java.util.Set;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.race.NascidoDoDragao;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Dracônicos — the Nascido do Dragão's own tree, and the ruleset's route to the
 * physical inheritance of a true Dragon: natural weapons, wings, a breath weapon, and finally a
 * full draconic form.
 *
 * <p>The Arma Natural half of the tree is now real: {@link #ARMAMENTO_DRACONICO} (through {@link
 * ArmamentoDraconicoFeat}) and {@link #SOPRO_DE_DRAGAO} grant entries of the {@link NaturalWeapon}
 * catalog, surfaced by {@code Character#getNaturalWeapons()}, and {@link #SOPRO_DE_DRAGAO}'s "+1
 * Margem Crítica Menor" applies for real, scoped to a Sopro attack. What is still blocked hangs
 * off <b>no flight or form state</b> (recorded on {@code NascidoDoDragao} itself), the missing
 * <b>elemental damage type</b> and this core rolling <b>no dice</b> (the "+1d6 … para cada
 * Título Aventyr Desperto" riders). {@link #ASAS_DE_DRAGAO} is fully real: its +2 Defesas is
 * unconditional because the wings are always there, and so is the Capa restriction that pays for
 * it — a permanent {@code Feat#getForbiddenEquipmentCategories} entry the equipment list enforces.
 *
 * <p><b>"Recém-criados" is not modelled.</b> Two constants restrict themselves to a Nascido do
 * Dragão "recém-criado", i.e. acquirable only at character creation. Nothing anywhere tracks
 * when a Talento was acquired, so that half of their Pré-requisito is dropped and only the race
 * clause is enforced — the gate is looser than the text, never stricter, the same direction
 * every other unexpressible clause in this catalog errs in.
 */
public enum DraconicoFeat implements Feat {

    /**
     * "Assim como os Dragões você possui um repertório de Armas Naturais, escolha duas armas
     * entre: Chifres Poderosos, Cauda Chicote, Garras Afiadas e Presas Longas."
     *
     * <p><b>Real.</b> The pick-two-of-four choice is recorded by {@link ArmamentoDraconicoFeat}
     * (grant that in {@code Character#feats}, not this bare constant), which returns the two
     * chosen {@link NaturalWeapon}s from {@code Feat#getGrantedNaturalWeapons}. Their Dano Base
     * and Perícia come from the catalog; each weapon's own authored Favor / Efeito Crítico is
     * still unmodeled — see {@link NaturalWeapon}.
     */
    ARMAMENTO_DRACONICO(
            "Assim como os Dragões você possui um repertório de Armas Naturais, escolha duas "
                    + "armas entre: Chifres Poderosos, Cauda Chicote, Garras Afiadas e Presas "
                    + "Longas. Você possui as Armas Naturais escolhidas.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()),

    /**
     * "Você tem asas e possui Movimento Base de Voo… Asas de Dragão são extremamente grandes,
     * rígidas e resistentes, por isso concedem Bônus de +2 as suas Defesas, mas o impede de usar
     * Equipamentos do tipo Capa." The Defesas half is real.
     *
     * <p>Unconditional, and that is what makes it expressible where every other clause in this
     * tree is not: the wings are a permanent feature of the body, not something spent into or
     * transformed into, so the bonus applies whether or not the holder is flying. It covers
     * <b>both</b> DF and DM — the text says "suas Defesas", the broad form.
     */
    // TODO: the flight half needs a flight state and a Movimento Base de Voo, neither of which
    //  exists — see Aviano's own Braços Alados. Note the PD cost, its per-Título reduction and
    //  the 1d6 + metade do Vigor Duração are all exact figures with nothing to apply them to.
    // "Impede de usar Equipamentos do tipo Capa" is real — a permanent
    // Feat#getForbiddenEquipmentCategories entry, refused by CharacterSheet#equip/canEquip and
    // caught on an already-assembled loadout by validateEquipmentLoadout. So the malus that pays
    // for the +2 Defesas is no longer free.
    ASAS_DE_DRAGAO(
            "Você tem asas e possui Movimento Base de Voo. Iniciar uma ação de voo em situações "
                    + "estressantes, como as Cenas de Combate, exige o uso de 4PD. Este Custo é "
                    + "reduzido em -2 para cada Título Aventyr que o personagem possua. A Duração "
                    + "do Efeito de Voo é igual à 1d6+Metade do Vigor Rodadas. Asas de Dragão são "
                    + "extremamente grandes, rígidas e resistentes, por isso concedem Bônus de +2 "
                    + "as suas Defesas, mas o impede de usar Equipamentos do tipo Capa.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return ASAS_DEFENSE_BONUS;
        }

        /**
         * "Mas o impede de usar Equipamentos do tipo Capa." Permanent: the wings are always
         * there, which is the same reason the Defesas bonus above is unconditional.
         */
        @Override
        public Set<ItemCategory> getForbiddenEquipmentCategories(final Character character) {
            return Set.of(ItemCategory.CLOAK);
        }
    },

    /**
     * "Você tem a Arma Natural: Arma de Sopro e é capaz de soprar energia Elemental como um
     * Dragão Verdadeiro. A Margem Crítica Menor aumenta em +1 e o dano do Sopro aumenta em +1d6
     * para cada Título Aventyr Desperto."
     *
     * <p><b>Two of three parts are real.</b> It grants {@link NaturalWeapon#ARMA_DE_SOPRO}
     * (through {@code Feat#getGrantedNaturalWeapons}), and its "+1 Margem Crítica Menor" applies
     * for real — {@code resolveCriticalMarginIncrease}'s {@link AttackSource} overload scopes it
     * to an attack made <em>with</em> the Arma de Sopro, which CLAUDE.md lists as trackable
     * ("a scope of what the attack was made with").
     */
    // TODO: "o dano do Sopro aumenta em +1d6 para cada Título Aventyr Desperto" is still blocked
    //  — this core rolls no dice, and the Sopro's Elemental damage type has no representation
    //  (DamageType has no elemental breakdown; its element would be
    //  NascidoDoDragao#getElementalLineage(), which is real data with no consumer).
    SOPRO_DE_DRAGAO(
            "Você tem a Arma Natural: Arma de Sopro e é capaz de soprar energia Elemental como um "
                    + "Dragão Verdadeiro. A Margem Crítica Menor aumenta em +1 e o dano do Sopro "
                    + "aumenta em +1d6 para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()) {
        @Override
        public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
            return List.of(NaturalWeapon.ARMA_DE_SOPRO);
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                  final Character character, final AttackSource attackSource) {
            return attackSource == NaturalWeapon.ARMA_DE_SOPRO ? SOPRO_CRITICAL_MARGIN_INCREASE : 0;
        }
    },

    /**
     * "Após usar seu Sopro de Dragão você emana uma aura de energia que te acompanha por 2
     * Rodadas… personagens adjacentes sofrem 2 pontos de Dano Físico Elemental."
     */
    // TODO: triggered by "usar seu Sopro de Dragão" — SOPRO_DE_DRAGAO grants the Arma de Sopro
    //  now, but nothing models the act of attacking with it as an event this can fire from.
    // TODO: recurring damage to everyone adjacent at the start of each of the holder's Turns is
    //  an outward, area-shaped effect nothing models: DamageService only ever computes damage
    //  *to* one target *from* an attacker, CharacterSheet#startTurn is still a no-op with no
    //  hook to fire from, and Área de Efeito has no footprint resolution (CLAUDE.md's "Area de
    //  Efeito" row, part (a)).
    AURA_DRACONICA(
            "Após usar seu Sopro de Dragão você emana uma aura de energia que te acompanhada por "
                    + "2 Rodadas. Durante a ativação da aura e no início de cada um dos seus "
                    + "Turnos subsequentes, enquanto a aura estiver ativa, personagens adjacentes "
                    + "sofrem 2 pontos de Dano Físico Elemental. O dano da aura aumenta em +1 e o "
                    + "alcance em +1UD para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .requiredFeat(SOPRO_DE_DRAGAO)
                    .build()),

    /**
     * "Temporariamente você pode mudar sua forma física, se transformando em um dragão bípede,
     * abandonando quaisquer traços raciais existente."
     */
    // The transformation itself is real — FormaActiveAbility, triggered through
    // ActiveAbilityService#activate: 3PA + 3PD to enter FormType.DRACONATO for 3 Rodadas, with the
    // "não poderá ser reativado até que passe por um Descanso Longo" gate enforced through
    // ActiveAbility#getReactivationRest (cleared by RestService#applyRest).
    // The "+2 Categoria de Tamanho, Força e Foco para cada Título" is granted too, as
    // round-scoped TemporaryBonuses lasting exactly the Duração. Partial reach on the Atributo
    // half, and that is the mechanism's own limit rather than this Talento's: an <ATTR>_BONUS
    // lands on a Perícia roll governed by that Atributo and nowhere else, since PV/PM/Conjuração
    // read Character#getEffectiveAttributeTotal, which has no sheet. The Categoria de Tamanho
    // half has no such limit — CharacterSizeService gained a sheet-taking overload for it.
    // TODO: "abandonando quaisquer traços raciais" needs the form to *suppress* the holder's
    //  racial traits, which nothing can do — Race#getRacialAbilities() is read live on every roll
    //  with no way to suspend it.
    DRACONATO(
            "Temporariamente você pode mudar sua forma física, se transformando em um dragão "
                    + "bípede, abandonando quaisquer traços raciais existente. Transformar-se em "
                    + "um Draconato requer 3PA + 3PD, ao fazê-lo sua Categoria de Tamanho, Força "
                    + "e Foco aumentam em +2 para cada Título Aventyr que você possuir. A Duração "
                    + "na forma de Draconato é de 3 Rodadas, este Efeito não poderá ser reativado "
                    + "até que passe por um Descanso Longo.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        private final ActiveAbility transformation =
                new FormaActiveAbility(this, FormType.DRACONATO,
                        // "sua Categoria de Tamanho, Força e Foco aumentam em +2 para cada Título"
                        new FormaActiveAbility.Uplift(2, 0, 2,
                                List.of(AttributeDomain.STRENGTH, AttributeDomain.FOCUS)));

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(transformation);
        }
    };

    private static final int ASAS_DEFENSE_BONUS = 2;
    private static final int SOPRO_CRITICAL_MARGIN_INCREASE = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    DraconicoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DRACONICO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
