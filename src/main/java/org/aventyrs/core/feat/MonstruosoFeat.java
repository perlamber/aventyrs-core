package org.aventyrs.core.feat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.combat.Retaliation;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.race.Bestial;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Ogro;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.race.RacialTraitSuppression;
import org.aventyrs.core.sheet.FormType;

/**
 * Talentos Monstruosos — the open tree any Monstruoso race can draw on, from unusual anatomy to
 * extra heads to acid blood.
 *
 * <p>Six constants carry real effects: {@link #ANATOMIA_INCOMUM} grants one instance of
 * Resistência a Críticos; {@link #PELE_RIJA} grants DF and RD together;
 * {@link #OSSOS_OCOS} is the catalog's <b>first Talento to apply a real malus</b> — a −1
 * Multiplicador de PV paid for by a +1UD Movimento Base; {@link #SELVAGERIA} raises the Dano
 * Base of an Arma Natural by +1; and {@link #FEROCIDADE} adds a Título-scaled flat bonus to an
 * Arma Natural dano roll. The last two are expressible since {@code Feat#resolveDamageBaseIncrease}
 * / {@code resolveDamageBonus} began seeing the {@code AttackSource} and {@code NaturalWeapon}
 * began authoring the weapons {@code Character#treatsAsNaturalWeapon} recognises. {@link
 * #SANGUE_ACIDO} deals its acid back to a melee attacker through {@code Feat#resolveRetaliation}.
 * {@link #ALFA}, {@link #DUAS_CABECAS} and {@link #CERBERO} grant their chosen (or left-over)
 * Bônus Racial through {@link AtributoRacialEscolhidoFeat}.
 *
 * <p>Gated through {@code FeatRequirements#requiredCreatureType}, since "Raça Monstruosa" spans
 * Aviano, Goblin, Ogro, Guampo, Indômito, Troll and Bestial with no common supertype.
 */
public enum MonstruosoFeat implements Feat {

    /**
     * "Receba +1 de bônus Racial em um dos atributos cedidos por sua Raça ou no atributo 'Força'…
     * Adicionalmente você recebe vantagem em rolagens de Persuasão e Atenção: Discernir Motivação
     * efetuadas contra outros indivíduos de sua raça que não possuam este Talento."
     */
    // The Atributo half is real, through AtributoRacialEscolhidoFeat: the options are every
    // Atributo carrying a Bônus Racial for this holder (the same reading
    // requiredAnyRacialAttributeValue takes of "cedidos por sua Raça"), plus Força.
    // TODO: the Vantagem is scoped to the *target* — same race, and lacking this same Talento —
    //  and resolveSkillRollBonus carries no opponent. Nothing anywhere lets a roll bonus inspect
    //  who is being rolled against except the attack-specific resolveAttackRollBonus.
    // "Qualquer atributo que receba bônus Racial com valor Base igual à 5" is enforced now,
    // through FeatRequirements#requiredAnyRacialAttributeValue — the narrow form of the
    // any-Atributo clause, which additionally demands the Atributo actually carry a Bônus Racial.
    ALFA(
            "Receba +1 de bônus Racial em um dos atributos cedidos por sua Raça ou no atributo "
                    + "'Força', a sua escolha. Adicionalmente você recebe vantagem em rolagens de "
                    + "Persuasão e Atenção: Discernir Motivação efetuadas contra outros indivíduos "
                    + "de sua raça que não possuam este Talento.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredAnyRacialAttributeValue(5)
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            List<AttributeDomain> options = Arrays.stream(AttributeDomain.values())
                    .filter(domain -> domain == AttributeDomain.STRENGTH
                            || holder.getAttributes().getAttribute(domain).getRacialBonus() > 0)
                    .toList();
            return List.of(FeatChoice.ofOne(AttributeDomain.class, options));
        }
    },

    /**
     * "Você recebe Resistência a Críticos. Você ignora o primeiro Efeito Crítico Menor que sofrer
     * em cada Cena de Combate."
     */
    // The Resistência a Críticos is real: an unconditional grant of one instance, summed by
    // CombatantSheet#getTotalCriticalResistance and subtracted from an attacker's Margem Crítica
    // Menor widening. The clause states no figure, so it grants exactly one instance — see
    // CombatantSheet#CRITICAL_RESISTANCE_INSTANCE.
    // TODO: ignoring an Efeito Crítico is close to expressible but not quite — CriticalEffect
    //  #applicableTo already filters a victim's immunities, but it keys on CriticalEffectType
    //  (which effect) and this clause keys on *severity* (Menor vs Maior), which the filter does
    //  not carry. It also needs a per-Cena counter, which nothing tracks.
    // "Apenas personagens não-humanos" is enforced now, through FeatRequirements#forbiddenRace —
    // the mirror of requiredRace, isInstance and all.
    ANATOMIA_INCOMUM(
            "Você recebe Resistência a Críticos. Você ignora o primeiro Efeito Crítico Menor que "
                    + "sofrer em cada Cena de Combate. Você ignora um Efeito Crítico Menor "
                    + "adicional para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .forbiddenRace(Human.class)
                    .build()) {
        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public int resolveCriticalResistance(final Character character, final SceneContext sceneContext) {
            return CombatantSheet.CRITICAL_RESISTANCE_INSTANCE;
        }
    },

    /** "Você é imune a Efeitos Críticos Menores. Sua resistência às Correntes de Efeitos aumenta em +2." */
    // The immunity is real as of 0.0.43: CriticalEffect#applicableTo now keys on severity as well
    // as on CriticalEffectType, and drops the whole chain of a Menor critical against a holder.
    // Unconditional, needing neither a Scene nor a per-Cena counter — which is exactly why this
    // clause, and not ANATOMIA_INCOMUM's, is the one that fits the widened filter.
    // TODO: "resistência às Correntes de Efeitos" is a stat this core does not compute —
    //  EffectChainService resolves whether a Corrente triggers, with nothing to resist it by.
    ANATOMIA_UNICA(
            "Você é imune a Efeitos Críticos Menores. Sua resistência às Correntes de Efeitos "
                    + "aumenta em +2.",
            FeatRequirements.builder()
                    .requiredFeat(ANATOMIA_INCOMUM)
                    .build()) {
        @Override
        public boolean ignoresMinorCriticalEffects() {
            return true;
        }
    },

    /**
     * "Você adquire Bônus Racial de +1 em Gnose ou Instinto, a sua escolha, mas sofre desvantagem
     * em rolagens de perícia baseadas 'Carisma' quando roladas contra criaturas não monstruosas."
     */
    // The Atributo half is real, through AtributoRacialEscolhidoFeat (Gnose or Instinto).
    // TODO: the Desvantagem is scoped two ways this core cannot express at once — by
    //  AttributeDomain ("baseadas em Carisma") with a named Especialização carve-out
    //  (Persuasão: Intimidação), and by the target's CreatureType.
    // The disjunctive Pré-requisito is real — Bestiais or Ogros outright, or any other Monstruosa
    // with a Título Desperto — three FeatRequirements#anyOf branches.
    DUAS_CABECAS(
            "Você possui 2 cabeças, duas pensam melhor do que uma. Você adquire Bônus Racial de "
                    + "+1 em Gnose ou Instinto, a sua escolha, mas sofre desvantagem em rolagens "
                    + "de perícia baseadas 'Carisma' (exceto Persuasão: Intimidação) quando "
                    + "roladas contra criaturas não monstruosas, devido temor ou repulsa que sua "
                    + "aparência causa.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder()
                            .requiredRace(Bestial.class)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredRace(Ogro.class)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredCreatureType(CreatureType.MONSTRUOSO)
                            .requiredAwakenedTitles(1)
                            .build())
                    .build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AttributeDomain.class, DUAS_CABECAS_OPTIONS));
        }
    },

    /**
     * "Seus ossos são mais leves que o normal, porém mais frágeis. Você adquire vantagem em suas
     * rolagens de Perícia baseadas em Destreza e seu Movimento Base aumenta em +1UD, mas seu
     * multiplicador de PV é reduzido em -1." Both the Movimento and the PV malus are real.
     *
     * <p><b>The first Talento in the catalog to apply a malus for real</b>, and it does so because
     * the trade the rules text frames — lighter, therefore faster, therefore frailer — is
     * expressible on both sides. That is what distinguishes it from {@code GiganteFeat}'s Clã
     * Talentos and {@code GorgonaFeat#CABELO_SERPENTINO}, which are withheld whole precisely
     * because only their malus could be expressed.
     */
    // TODO: the Destreza Vantagem is scoped by AttributeDomain rather than naming a Perícia, and
    //  resolveSkillRollBonus takes a SkillType — "whichever Perícias Destreza currently governs"
    //  is itself live data once a substitution ability is held. Same gap CavalariaFeat
    //  #GRANDE_GINETE cites. Note this leaves the holder with one unpaid bonus, which is the
    //  reason the pair above is granted and the third clause is not: the movement/PV trade stands
    //  on its own.
    OSSOS_OCOS(
            "Seus ossos são mais leves que o normal, porém mais frágeis. Você adquire vantagem em "
                    + "suas rolagens de Perícia baseadas em Destreza e seu Movimento Base aumenta "
                    + "em +1UD, mas seu multiplicador de PV é reduzido em -1.",
            FeatRequirements.builder().build()) {
        /** "Apenas … recém-criados" — only a starting Talento slot can take it. */
        @Override
        public boolean isAcquirableOnlyAtCreation() {
            return true;
        }

        @Override
        public int resolveMovementIncrease(final Character character) {
            return 1;
        }

        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return -1;
        }
    },

    /** "Você recebe Bônus Racial de +2 em DF e RDS." Both halves real. */
    // The disjunctive Pré-requisito is real — "Raças Monstruosa ou Vigor 4", two
    // FeatRequirements#anyOf branches, so a Monstruoso with Vigor 3 now qualifies as written.
    PELE_RIJA(
            "Você recebe Bônus Racial de +2 em DF e RDS.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder()
                            .requiredCreatureType(CreatureType.MONSTRUOSO)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.VIGOR)
                            .requiredAttributeValue(4)
                            .build())
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.PHYSICAL ? PELE_RIJA_BONUS : 0;
        }

        @Override
        public int resolveDamageReduction(final Character character) {
            return PELE_RIJA_BONUS;
        }
    },

    /**
     * "Sempre que for atingido por um ataque Corpo-a-Corpo, o atacante sofre 1 ponto de Dano
     * Físico Elemental: Natural."
     */
    // Real through Feat#resolveRetaliation: reported on a landed melee hit, dealt by the caller.
    // The +2 applies when the attacker struck with a Weapon they treat as an Arma Natural
    // (Character#treatsAsNaturalWeapon).
    // TODO: the Desarmado half of the +2 — on the Perícia-roll path an Ataque Desarmado has no
    //  AttackSource, so a null source means "unarmed" and "caller didn't say" alike, and is not
    //  read as either.
    SANGUE_ACIDO(
            "Sempre que for atingido por um ataque Corpo-a-Corpo, o atacante sofre 1 ponto de "
                    + "Dano Físico Elemental: Natural. Este dano aumenta em +2 se o atacante tiver "
                    + "utilizado de Armas Naturais ou Desarmado.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()) {
        @Override
        public Retaliation resolveRetaliation(final Character holder, final AttackSource attackSource,
                                              final Character attacker, final boolean criticalHit) {
            boolean naturalWeapon = attacker != null && attackSource instanceof Weapon weapon
                    && attacker.treatsAsNaturalWeapon(weapon);
            return new Retaliation(SANGUE_ACIDO_RETALIATION + (naturalWeapon ? SANGUE_ACIDO_NATURAL_WEAPON_BONUS : 0),
                    new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.NATURAL), null, 0);
        }
    },

    /**
     * "Uma terceira cabeça nasce em ti… Alvos de seus ataques e personagens intimidados por você
     * perdem temporariamente 1 ponto de Autocontrole."
     */
    // The Atributo half is real: "o Atributo faltante" is whichever of Gnose and Instinto
    // DUAS_CABECAS' acquired form did not pick (AtributoRacialEscolhidoFeat#chosenBy). Holding
    // DUAS_CABECAS without its choice recorded grants nothing.
    // TODO: draining a target's temporary Autocontrole is close: CombatantSheet#spendEgoPoints is
    //  exactly the raw-drain entry point (the one Primor uses, deliberately distinct from a
    //  holder's own deliberate use). What is missing is the trigger — nothing fires off "this
    //  character was the target of your attack", which is the "this one delivered attack" scoping
    //  gap. The recovery clause needs a per-Descanso hook RestService does not expose either.
    CERBERO(
            "Uma terceira cabeça nasce em ti. Você adquire bônus racial de +1 no Atributo "
                    + "faltante, aquele que não foi escolhido no talento 'Duas Cabeças', entre "
                    + "Instinto e Gnose. Alvos de seus ataques (físicos e mágicos) e personagens "
                    + "intimidados por você perdem temporariamente 1 ponto de Autocontrole. "
                    + "Pontos de Autocontrole perdidos desta forma são recuperados 1 a cada "
                    + "descanso completo, efeito não cumulativo.",
            FeatRequirements.builder()
                    .requiredFeat(DUAS_CABECAS)
                    .requiredAwakenedTitles(2)
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return AtributoRacialEscolhidoFeat.chosenBy(character, DUAS_CABECAS)
                    .filter(chosen -> DUAS_CABECAS_OPTIONS.contains(domain) && domain != chosen)
                    .map(chosen -> CERBERO_RACIAL_BONUS)
                    .orElse(0);
        }
    },

    /**
     * "Você pode mudar sua aparência, assumindo uma forma humana comum."
     *
     * <p>Tagged {@code Aventyr/Feérico/Monstruoso}; filed here rather than in {@code FeericoFeat}
     * because the source document prints it under the Monstruoso heading. Its Pré-requisito
     * covers both populations, and only the Monstruoso branch is enforced (next note).
     */
    // "Perde características raciais físicas, como escamas, chifres, garras" is real:
    // RacialTraitSuppression.PHYSICAL while its holder is in FormType.HUMANA, so their racial
    // Armas Naturais, anatomy (RC + Efeito Crítico immunities) and base Categoria de Tamanho fall
    // silent while their Habilidades Raciais and Atributo bonuses do not — a Gnomo passing for
    // human has no horns to show but is no less clever for it.
    // ⚠️ The base Categoria de Tamanho is an *inference*: the clause enumerates appendages and
    // never mentions stature, so it is suppressed on the strength of "assumindo uma forma humana
    // comum". See RacialTraitSuppression#PHYSICAL — it is the one trait on that rung the text
    // does not name.
    // Creature type is deliberately NOT suppressed: this constant's own Pré-requisito is
    // requiredCreatureType(MONSTRUOSO|FEERICO), so silencing it would make the Talento
    // retroactively ineligible for its own holder. Looking human is not being human.
    // TODO: no activation transaction — "Monstruosos precisam utilizar 2PD para ativar este
    //  efeito, personagens Feéricos utilizam 2PM" prices the same ability differently per
    //  CreatureType, and ActiveAbility#getDeterminationPointCost/getMagicPointCost take no
    //  arguments, so one ability cannot answer both. FormaActiveAbility does not fit either (it
    //  hardcodes 3PA + 3PD, a 3-Rodada Duração and a Descanso Longo reactivation gate, none of
    //  which this clause states). Until a per-holder cost exists, the shape is entered through
    //  the bare CombatantSheet#enterForm mutator — a caller's call, as every Forma that no
    //  ActiveAbility grants already is — and the suppression above applies from there.
    // The disjunctive Pré-requisito is real — "Monstruoso com 1 Título Desperto, ou Feérico" —
    // two FeatRequirements#anyOf branches, each carrying its own Título count.
    MIMETIZAR_FORMA_HUMANA(
            "Você pode mudar sua aparência, assumindo uma forma humana comum. Você perde "
                    + "características raciais físicas, como escamas, chifres, garras etc. Sua "
                    + "aparência base é similar, o máximo possível, de sua forma natural e é "
                    + "sempre igual, você não consegue se passar por outras pessoas com esta "
                    + "habilidade. Personagens Monstruosos precisam utilizar 2PD para ativar este "
                    + "efeito, personagens Feéricos utilizam 2PM.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder()
                            .requiredCreatureType(CreatureType.MONSTRUOSO)
                            .requiredAwakenedTitles(1)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredCreatureType(CreatureType.FEERICO)
                            .build())
                    .build()) {
        /** "Você perde características raciais físicas" — while wearing the human shape only. */
        @Override
        public RacialTraitSuppression resolveRacialTraitSuppression(final Character character,
                                                                    final CombatantSheet sheet) {
            return sheet != null && sheet.isInForm(FormType.HUMANA)
                    ? RacialTraitSuppression.PHYSICAL
                    : RacialTraitSuppression.NONE;
        }
    },

    /**
     * "Você recebe Bônus de +1 em rolagens de danos de suas Armas Naturais, este Bônus aumenta
     * cumulativamente em +1 para cada Título Aventyr que você tenha Desperto."
     *
     * <p><b>Real.</b> A flat dano bonus of {@code 1 + }Títulos Despertos on any attack the holder
     * makes with a weapon their {@code Character#treatsAsNaturalWeapon} recognises — resolved
     * through the trailing-{@code AttackSource} overload of {@link Feat#resolveDamageBonus}, which
     * {@code AbstractSkillInteraction} now hands the delivery channel. Untyped, so it flattens to
     * {@code FISICO} in {@code DamageBonus#total}, the established reading of "+N em rolagens de
     * Danos". {@code SELVAGERIA} later converts this into a Dano Base increase — the same
     * exclusive-conversion shape as {@code AtaqueCorpoACorpoCompetencyAbility#BRUTALIDADE} — but
     * that half stays blocked on the Ferocidade Característica Racial having no representation.
     */
    FEROCIDADE(
            "Você recebe Bônus de +1 em rolagens de danos de suas Armas Naturais, este Bônus "
                    + "aumenta cumulativamente em +1 para cada Título Aventyr que você tenha "
                    + "Desperto.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource) {
            if (actor == null || !(attackSource instanceof Weapon weapon) || !actor.treatsAsNaturalWeapon(weapon)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(FEROCIDADE_BASE_BONUS + actor.getAllTitles().size(), DamageType.FISICO));
        }
    },

    /**
     * "Você sente a presença de personagens que não sejam Monstros ou Monstruosos em Distância
     * Curta, identificando suas posições automaticamente."
     */
    // TODO: automatic detection with no roll — nothing models "knowing where someone is", and
    //  this core has no visibility or detection state for the clause to bypass.
    // TODO: "não recebe Desvantagens para atacar às cegas" suppresses a Desvantagem that is
    //  itself unmodelled — Skill#DISADVANTAGE_MALUS is applied by a caller, and nothing can
    //  cancel one. Same shape as CavalariaFeat#GRANDE_GINETE's own suppression clause.
    FAREJAR_O_MEDO(
            "Você sente a presença de personagens que não sejam Monstros ou Monstruosos em "
                    + "Distância Curta, identificando suas posições automaticamente, dispensando "
                    + "quaisquer testes de Perícia para encontrá-los. Você não recebe Desvantagens "
                    + "em Perícias para atacar às cegas os personagens identificados desta forma.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredFeat(DuelistaFeat.COMBATER_AS_CEGAS)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "O Dano Base de todas as suas Armas Naturais aumenta em +1. Os Bônus em danos concedidos
     * por Ferocidade são convertidos em Aumento de Dano Base."
     *
     * <p>The Dano Base half is <b>real</b> now that {@link Feat#resolveDamageBaseIncrease(Character,
     * Weapon)} sees the weapon: it scopes the +1 with {@code Character#treatsAsNaturalWeapon},
     * the per-character view that also catches a Talento-reclassified weapon, so a wielded blade
     * or an Ataque Desarmado (a {@code null} weapon) gets nothing.
     */
    // TODO: "os Bônus de Ferocidade são convertidos em Dano Base" stays unbuilt — Ferocidade
    //  (the Característica Racial) has no representation, and this is a *conversion* of the
    //  FEROCIDADE Talento's own dano bonus (now real), not that bonus itself. Model it on
    //  AtaqueCorpoACorpoCompetencyAbility#BRUTALIDADE's exclusive-conversion shape once it exists.
    SELVAGERIA(
            "O Dano Base de todas as suas Armas Naturais aumenta em +1. Os Bônus em danos "
                    + "concedidos por Ferocidade são convertidos em Aumento de Dano Base.",
            FeatRequirements.builder()
                    .requiredFeat(FEROCIDADE)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.BRUTO)
                    .build()) {
        @Override
        public int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
            return character.treatsAsNaturalWeapon(weapon) ? 1 : 0;
        }
    };

    private static final int PELE_RIJA_BONUS = 2;

    /** DUAS_CABECAS' "Gnose ou Instinto" — and the pair CERBERO grants the other half of. */
    private static final List<AttributeDomain> DUAS_CABECAS_OPTIONS =
            List.of(AttributeDomain.INSTINCT, AttributeDomain.GNOSE);

    /** CERBERO's "bônus racial de +1 no Atributo faltante". */
    private static final int CERBERO_RACIAL_BONUS = 1;

    /** SANGUE_ACIDO's "1 ponto de Dano Físico Elemental: Natural". */
    private static final int SANGUE_ACIDO_RETALIATION = 1;

    /** SANGUE_ACIDO's "aumenta em +2 se o atacante tiver utilizado de Armas Naturais". */
    private static final int SANGUE_ACIDO_NATURAL_WEAPON_BONUS = 2;
    private static final int FEROCIDADE_BASE_BONUS = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    MonstruosoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.MONSTRUOSO;
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
