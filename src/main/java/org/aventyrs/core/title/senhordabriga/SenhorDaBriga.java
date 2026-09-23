package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.AgarrarEDerrubar;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.ForcedTargeting;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleAttackModifiers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_CHOICE_LOCKED;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Senhor da Briga — "Despertos colocando seu corpo a prova … utilizando apenas as armas que a
 * natureza lhes deu", the second concrete Título Aventyr. Where Santo acts on Defesas and Auras,
 * this one acts on the <b>attack itself</b>, so most of what it holds reaches play through the
 * Título scans on {@link AventyrTitle} rather than through activations: the Margem Crítica
 * ({@code CriticalServiceImpl}), the Dano Crítico, the Dano Base ({@code DamageBaseServiceImpl}),
 * the Defesas ({@code DefenseServiceImpl}), and the attack and dano rolls ({@code
 * AbstractSkillInteraction}). What those cannot apply — dice, a PA price, the Defesa rolled
 * against, the damage type, a Corrente — is reported by {@link #resolveAttackModifiers} for the
 * caller, through {@link TitleAttackModifiers#resolve}.
 *
 * <p>Almost every clause is scoped to <b>Armas Naturais</b>, always judged by {@code
 * Character#treatsAsNaturalWeapon} so a Talento reclassifying a weapon reaches them too. An
 * Ataque Desarmado counts because it is authored as one ({@code NaturalWeapon#ATAQUE_DESARMADO}):
 * an attack that names no weapon at all is not one.
 */
public class SenhorDaBriga implements AventyrTitle {

    private static final String BASE_EFFECT_DESCRIPTION =
            "Você nunca recebe Desvantagens em rolagens de Perícias de Ataques com Armas Naturais ou " +
            "com Armas Improvisadas. Você nunca recebe Desvantagens em rolagens de Defesas por estar " +
            "sem nenhum Equipamento Defensivo.";

    private static final String PRIMARY_TITLE_BONUS_DESCRIPTION =
            "Se Senhor da Briga for seu Título Primário o Dano Base de seus ataques com Armas Naturais " +
            "aumentam em +1, suas Defesas aumentam em +2 enquanto estiver armado apenas com suas Armas " +
            "Naturais.";

    /** Título Primário: "suas Defesas aumentam em +2 enquanto estiver armado apenas com suas Armas Naturais". */
    static final int PRIMARY_DEFESAS_BONUS = 2;
    /** Título Primário: "o Dano Base de seus ataques com Armas Naturais aumentam em +1". */
    static final int PRIMARY_DAMAGE_BASE_INCREASE = 1;
    /** Fantasma do Ringue's Efeito Passivo, "+1 em suas Defesas". */
    static final int FANTASMA_BASE_DEFESAS = 1;
    /** Defesa Fantasma, "+1 enquanto você estiver armado apenas com suas Armas Naturais". */
    static final int FANTASMA_NATURAL_WEAPONS_DEFESAS = 1;
    /** Defesa Fantasma, "+3 enquanto você não utilizar Equipamentos Defensivos". */
    static final int FANTASMA_UNARMORED_DEFESAS = 3;
    /** Punho Inigualável, "Margem Crítica Menor aumentada em +2 números". */
    static final int PUNHO_INIGUALAVEL_MARGIN = 2;
    /** Campeão da Taverna, "A Margem Crítica Menor … aumenta em +1". */
    static final int CAMPEAO_MARGIN = 1;
    /** Fantasma do Ringue, "Suas rolagens de Defesas tem a Margem Crítica Menor aumentada em +2". */
    static final int FANTASMA_DEFENSE_MARGIN = 2;
    /** Cruz de Sangue, "aumenta sua Margem Crítica Menor de Defesas e Ataque Corpo-a-Corpo … em +2". */
    static final int CRUZ_DE_SANGUE_MARGIN = 2;
    /** Campeão da Taverna, "Seu Dano Crítico Menor aumenta em +2". */
    static final int CAMPEAO_MINOR_CRITICAL_DAMAGE = 2;
    /** Campeão da Taverna, "seu Dano Crítico Maior aumenta em +1d6". */
    static final int CAMPEAO_MAJOR_CRITICAL_DICE = 1;
    /** Chamar pra Briga, "sendo obrigados a te atacar pelas próximas 2 Rodadas". */
    static final int CHAMAR_PRA_BRIGA_ROUNDS = 2;

    /** "Escolha um Elemento entre Fogo, Magma, Terra, Água, Gelo, Ar, Eletricidade ou Natural" — Ar is VENTO. */
    public static final Set<ElementalType> IMPACTO_ELEMENTAL_ELEMENTS = EnumSet.of(ElementalType.FOGO,
            ElementalType.MAGMA, ElementalType.TERRA, ElementalType.AGUA, ElementalType.GELO,
            ElementalType.VENTO, ElementalType.ELETRICIDADE, ElementalType.NATURAL);

    /** Chamar pra Briga's "não afeta um mesmo alvo duas vezes mesma Cena", keyed per provoker. */
    record ChamarPraBrigaMark(UUID holderId) {
    }

    private final List<SenhorDaBrigaSpecialization> specializations;
    private final List<AventyrTitleAbility> abilities;
    private ElementalType impactoElementalElement;

    /**
     * {@code abilities} is typed as the shared {@link AventyrTitleAbility}, so it holds this
     * Título's own {@link SenhorDaBrigaAbility} constants and both Especializações' gated ones
     * alike; both lists are stored as mutable copies for {@link #grantAbility}/{@link
     * #grantSpecialization} to append to.
     */
    public SenhorDaBriga(@NonNull final List<SenhorDaBrigaSpecialization> specializations,
                         @NonNull final List<AventyrTitleAbility> abilities) {
        this(specializations, abilities, null);
    }

    /**
     * As {@link #SenhorDaBriga(List, List)}, restoring an Impacto Elemental element already chosen
     * — for a caller rebuilding a held Título from persistence.
     */
    public SenhorDaBriga(@NonNull final List<SenhorDaBrigaSpecialization> specializations,
                         @NonNull final List<AventyrTitleAbility> abilities,
                         final ElementalType impactoElementalElement) {
        this.specializations = new ArrayList<>(specializations);
        this.abilities = new ArrayList<>(abilities);
        if (impactoElementalElement != null) {
            chooseImpactoElementalElement(impactoElementalElement);
        }
    }

    /**
     * The Senhor da Briga sheet's Character holds, if any — what an activation reads its own
     * Título off, since a Duração counting the holder's Habilidades, or an element chosen once, is
     * a fact about <em>their</em> Título rather than about any instance merely in scope.
     */
    public static Optional<SenhorDaBriga> heldBy(@NonNull final CombatantSheet sheet) {
        return sheet.getCharacter().getAllTitles().stream()
                .filter(SenhorDaBriga.class::isInstance)
                .map(SenhorDaBriga.class::cast)
                .findFirst();
    }

    /**
     * {@link #heldBy}, refusing a sheet that holds none.
     *
     * @throws IllegalOperationException {@code REQUIRED_TITLE_TRAIT_NOT_HELD}
     */
    static SenhorDaBriga requireHeldBy(final CombatantSheet sheet) {
        return heldBy(sheet).orElseThrow(() -> new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD));
    }

    /** "Centelha Bruta" — the header Senhor da Briga is listed under. */
    @Override
    public TitleArchetype getArchetype() {
        return TitleArchetype.BRUTO;
    }

    @Override
    public String getName() {
        return "Senhor da Briga";
    }

    /**
     * Despertar. Both clauses hold <b>vacuously</b>: this core imposes no Desvantagem for attacking
     * with an Arma Natural, nor for rolling a Defesa without Equipamento Defensivo, so there is
     * nothing to be exempt from.
     *
     * <p>TODO the "Armas Improvisadas" half: no such weapon exists here — nothing marks a table leg
     * as a weapon. And no Desvantagem is tagged as one (a malus is just a signed −2), so if a
     * source is ever added, exempting a holder from it needs that tag first.
     */
    @Override
    public String getBaseEffectDescription() {
        return BASE_EFFECT_DESCRIPTION;
    }

    @Override
    public String getPrimaryTitleBonusDescription() {
        return PRIMARY_TITLE_BONUS_DESCRIPTION;
    }

    @Override
    public List<AventyrTitleSpecialization> getSpecializations() {
        return specializations.stream().map(AventyrTitleSpecialization.class::cast).toList();
    }

    @Override
    public List<AventyrTitleAbility> getAbilities() {
        return abilities;
    }

    @Override
    public void grantAbility(final AventyrTitleAbility ability) {
        abilities.add(ability);
    }

    /**
     * Refuses anything but this Título's own two Especializações — the enforced half of "apenas
     * Senhores da Briga podem adquirir esta especialização".
     */
    @Override
    public void grantSpecialization(final AventyrTitleSpecialization specialization) {
        if (!(specialization instanceof SenhorDaBrigaSpecialization own)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }
        specializations.add(own);
    }

    /** Whether this Título holds trait — an Especialização, Habilidade or Suprema. */
    public boolean holds(final AventyrTitleAbility trait) {
        return getAllAbilities().contains(trait);
    }

    /** Whether the Punho Inigualável Habilidades held receive their Grande Mestre das Brigas upgrades. */
    public boolean holdsGrandeMestreDasBrigas() {
        return holds(PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS);
    }

    /** Whether the Fantasma do Ringue Habilidades held receive their Malícia de Valentão upgrades. */
    public boolean holdsMaliciaDeValentao() {
        return holds(FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
    }

    /** The Impacto Elemental element chosen, if one has been. */
    public Optional<ElementalType> getImpactoElementalElement() {
        return Optional.ofNullable(impactoElementalElement);
    }

    /**
     * Records Impacto Elemental's element — "depois de escolhido não é possível mudar o Elemento".
     * Choosing the same element again is harmless; a different one, or one the clause does not
     * offer ({@link #IMPACTO_ELEMENTAL_ELEMENTS}), is refused. Not gated on holding the Habilidade,
     * so a caller may record the pick in the same step that grants it.
     *
     * @throws IllegalOperationException {@code TITLE_ABILITY_CHOICE_LOCKED}
     */
    public void chooseImpactoElementalElement(@NonNull final ElementalType element) {
        if (!IMPACTO_ELEMENTAL_ELEMENTS.contains(element)
                || (impactoElementalElement != null && impactoElementalElement != element)) {
            throw new IllegalOperationException(TITLE_ABILITY_CHOICE_LOCKED);
        }
        impactoElementalElement = element;
    }

    /**
     * Impacto Elemental's Duração in attacks — "2+ o número de outras Habilidades e Supremas de
     * Punho Inigualável que você possuir".
     */
    public int resolveImpactoElementalAttacks() {
        long others = abilities.stream()
                .filter(ability -> ability instanceof PunhoInigualavelAbility)
                .filter(ability -> ability != PunhoInigualavelAbility.IMPACTO_ELEMENTAL)
                .count();
        return 2 + (int) others;
    }

    /** Whether Finalização's "Nesta Rodada" is open on holder. */
    public boolean isFinalizacaoActive(@NonNull final CombatantSheet holder) {
        return holder.hasActivationWindow(SenhorDaBrigaAbility.FINALIZACAO);
    }

    /**
     * Despertar's Título-Primário Defesas, Fantasma do Ringue's conditional passive, and Campeão da
     * Taverna's stacked crits — all read at the moment a Defesa is calculated, so each follows what
     * the holder has in hand and on their back with nothing to grant or revoke.
     */
    @Override
    public int resolveBaseDefesasBonus(final SceneContext sceneContext, final Character holder,
                                       final CombatantSheet sheet, final boolean primary) {
        int total = 0;
        boolean naturalWeaponsOnly = holder != null && holder.isArmedOnlyWithNaturalWeapons();
        if (primary && naturalWeaponsOnly) {
            total += PRIMARY_DEFESAS_BONUS;
        }
        if (holds(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE)) {
            total += FANTASMA_BASE_DEFESAS;
            if (naturalWeaponsOnly) {
                total += FANTASMA_NATURAL_WEAPONS_DEFESAS;
            }
            if (holder != null && !holder.usesDefensiveEquipment()) {
                total += FANTASMA_UNARMORED_DEFESAS;
            }
        }
        if (sheet != null && holds(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)) {
            total += sheet.getCombatCounter(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA);
        }
        return total;
    }

    @Override
    public int resolveDamageBaseIncrease(final Character holder, final Weapon weapon, final boolean primary) {
        return primary && holder != null && holder.treatsAsNaturalWeapon(weapon) ? PRIMARY_DAMAGE_BASE_INCREASE : 0;
    }

    @Override
    public int resolveCriticalMarginIncrease(final SkillType skillType, final AttackSource attackSource,
                                             final CombatantSheet holder) {
        if (skillType == null || holder == null) {
            return 0;
        }
        int total = 0;
        boolean cruzDeSangue = holder.hasActivationWindow(FantasmaDoRingueAbility.CRUZ_DE_SANGUE);
        if (skillType.isAttackSkill() && isNaturalWeaponAttack(holder, attackSource)) {
            if (holds(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL)) {
                total += PUNHO_INIGUALAVEL_MARGIN;
            }
            if (holds(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)) {
                total += CAMPEAO_MARGIN;
            }
            if (cruzDeSangue && skillType == SkillType.ATAQUE_CORPO_A_CORPO) {
                total += CRUZ_DE_SANGUE_MARGIN;
            }
        }
        if (skillType == SkillType.ESQUIVA_E_APARAR) {
            if (holds(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE)) {
                total += FANTASMA_DEFENSE_MARGIN;
            }
            if (cruzDeSangue) {
                total += CRUZ_DE_SANGUE_MARGIN;
            }
        }
        return total;
    }

    /** Campeão da Taverna: "A Margem Crítica … Maior de suas Armas Naturais aumenta em +1". */
    @Override
    public int resolveMajorCriticalMarginIncrease(final SkillType skillType, final AttackSource attackSource,
                                                  final CombatantSheet holder) {
        return holder != null && skillType != null && skillType.isAttackSkill()
                && holds(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA) && isNaturalWeaponAttack(holder, attackSource)
                ? CAMPEAO_MARGIN : 0;
    }

    /** Punho Inigualável: "Seus ataques com Armas Naturais recebem Guilhotina como Efeito Crítico Adicional". */
    @Override
    public List<CriticalEffectType> resolveAdditionalCriticalEffects(final SkillType skillType,
                                                                     final AttackSource attackSource,
                                                                     final CombatantSheet holder) {
        return holder != null && holds(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL)
                && isNaturalWeaponAttack(holder, attackSource)
                ? List.of(CriticalEffectType.GUILHOTINA) : List.of();
    }

    /**
     * Finalização's Corrente, while its Rodada lasts: one extra application of the Arma Natural's
     * own Efeito Crítico — at Menor on a hit that isn't critical, once more on one that is.
     */
    @Override
    public int resolveExtraNaturalCriticalEffectApplications(final CombatantSheet holder,
                                                             final AttackSource attackSource) {
        return holder != null && isFinalizacaoActive(holder) && isNaturalWeaponAttack(holder, attackSource) ? 1 : 0;
    }

    /**
     * Fantasma do Ringue's "recebem Ímpeto Defensivo como Efeito Crítico adicional", and Cruz de
     * Sangue's "Suas rolagens de Defesas recebem Contra-atacante como um Efeito Crítico adicional"
     * while its Rodada lasts.
     */
    @Override
    public List<DefensiveCriticalEffectType> resolveAdditionalDefensiveCriticalEffects(final CombatantSheet holder) {
        List<DefensiveCriticalEffectType> added = new ArrayList<>();
        if (holds(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE)) {
            added.add(DefensiveCriticalEffectType.IMPETO_DEFENSIVO);
        }
        if (holder != null && holder.hasActivationWindow(FantasmaDoRingueAbility.CRUZ_DE_SANGUE)) {
            added.add(DefensiveCriticalEffectType.CONTRA_ATACANTE);
        }
        return added;
    }

    @Override
    public CriticalDamage resolveCriticalDamage(final SkillType skillType, final AttackSource attackSource,
                                                final CriticalResult criticalResult, final CombatantSheet holder) {
        if (holder == null || criticalResult == null || !holds(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)
                || !isNaturalWeaponAttack(holder, attackSource)) {
            return CriticalDamage.NONE;
        }
        return switch (criticalResult) {
            case ACERTO_CRITICO_MENOR -> CriticalDamage.ofFlat(CAMPEAO_MINOR_CRITICAL_DAMAGE);
            case ACERTO_CRITICO_MAIOR -> CriticalDamage.ofDice(CAMPEAO_MAJOR_CRITICAL_DICE);
            default -> CriticalDamage.NONE;
        };
    }

    /**
     * One Vantagem for an Arma Natural attack while any of Impacto Elemental, Agarrar e Derrubar or
     * Grande Mestre's Rolamento Ofensivo is in force — three clauses granting the same "Vantagem em
     * rolagens de Perícia de Ataque com Armas Naturais", which is one Vantagem, not three. And
     * Malícia de Valentão's separate Vantagem against a foe Fingir Fraquezas bound to this holder.
     */
    @Override
    public int resolveAttackRollBonus(final SkillType skillType, final AttackSource attackSource,
                                      final CombatantSheet holder, final CombatantSheet attackTarget,
                                      final SceneContext sceneContext) {
        if (skillType == null || !skillType.isAttackSkill() || holder == null) {
            return 0;
        }
        int bonus = 0;
        if (isNaturalWeaponAttack(holder, attackSource)
                && (holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL) > 0
                        || holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR) > 0
                        || holder.hasActivationWindow(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO))) {
            bonus += Skill.ADVANTAGE_BONUS;
        }
        if (isBoundByFingirFraquezas(holder, attackTarget)) {
            bonus += Skill.ADVANTAGE_BONUS;
        }
        return bonus;
    }

    /** Malícia de Valentão's "Vantagem em … Danos contra alvos encantados por Fingir Fraquezas". */
    @Override
    public int resolveDamageRollBonus(final SkillType skillType, final AttackSource attackSource,
                                      final CombatantSheet holder, final CombatantSheet attackTarget,
                                      final SceneContext sceneContext) {
        if (skillType == null || !skillType.isAttackSkill() || holder == null) {
            return 0;
        }
        return isBoundByFingirFraquezas(holder, attackTarget) ? Skill.ADVANTAGE_BONUS : 0;
    }

    /**
     * Punho de Ferro's first-Arma-Natural-attack clauses, Impacto Elemental's retyping, Agarrar e
     * Derrubar's Corrente and Cruz de Sangue's counter-attack die — see {@link
     * TitleAttackModifiers} for why each is the caller's to apply.
     */
    @Override
    public TitleAttackModifiers resolveAttackModifiers(final CombatantSheet holder, final AttackSource attackSource,
                                                       final CombatantSheet attackTarget,
                                                       final SceneContext sceneContext) {
        if (holder == null) {
            return TitleAttackModifiers.NONE;
        }
        int extraDice = 0;
        int actionPointReduction = 0;
        DefenseType defenseType = null;
        DamageDescriptor descriptor = null;
        CriticalEffectType criticalEffect = null;
        List<EffectChain> chains = new ArrayList<>();

        if (holder.getRemainingEnhancedAttacks(FantasmaDoRingueAbility.CRUZ_DE_SANGUE) > 0) {
            extraDice++;
        }
        if (isNaturalWeaponAttack(holder, attackSource)) {
            if (holds(SenhorDaBrigaAbility.PUNHO_DE_FERRO) && isFirstNaturalWeaponAttackOfRound(holder)) {
                if (isOddRound(sceneContext)) {
                    actionPointReduction++;
                } else {
                    extraDice++;
                }
            }
            if (impactoElementalElement != null
                    && holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.IMPACTO_ELEMENTAL) > 0) {
                if (holdsGrandeMestreDasBrigas()) {
                    // "rolados contra a DM de seus alvos, causam danos mágicos": ELEMENTAL is this
                    // core's magical elemental damage — keeping the chosen element, which MAGICO
                    // cannot carry. A reading, flagged in docs/senhor-da-briga.md.
                    defenseType = DefenseType.MAGIC;
                    descriptor = new DamageDescriptor(DamageType.ELEMENTAL, impactoElementalElement);
                    criticalEffect = CriticalEffectType.CATACLISMO;
                } else {
                    descriptor = new DamageDescriptor(DamageType.FISICO_ELEMENTAL, impactoElementalElement);
                }
            }
            if (holder.getRemainingEnhancedAttacks(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR) > 0) {
                chains.add(new AgarrarEDerrubar(holder));
            }
        }
        return new TitleAttackModifiers(extraDice, actionPointReduction, defenseType, descriptor, criticalEffect,
                chains);
    }

    /**
     * Spends the budgets an attack uses: Impacto Elemental's and Agarrar e Derrubar's only on an
     * Arma Natural attack (the only kind they enhance), Cruz de Sangue's counter-attack on any.
     */
    @Override
    public void consumeAttackCharges(final CombatantSheet holder, final AttackSource attackSource) {
        if (holder == null) {
            return;
        }
        holder.consumeEnhancedAttack(FantasmaDoRingueAbility.CRUZ_DE_SANGUE);
        if (isNaturalWeaponAttack(holder, attackSource)) {
            holder.consumeEnhancedAttack(PunhoInigualavelAbility.IMPACTO_ELEMENTAL);
            holder.consumeEnhancedAttack(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR);
        }
    }

    /**
     * Campeão da Taverna's "Sempre que desencadear um Acerto Crítico, suas Defesas aumentam em +1 até
     * o final da Cena" — the caller reports each crit holder lands; the bonus stacks one per crit
     * and lapses at {@code Scene#endCombat}. Returns the stacks now held (0 without the Suprema).
     */
    public int recordCriticalHit(@NonNull final CombatantSheet holder) {
        if (!holds(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA)) {
            return 0;
        }
        return holder.incrementCombatCounter(SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA);
    }

    /**
     * Chamar pra Briga, run by the caller after holder lands a hit on target: records it when made
     * with an Arma Natural, and — if target was also hit that way the Rodada before, is armed, and
     * has not been provoked by this holder yet this combat — casts a 2-Rodada {@link
     * ForcedTargeting} on them through {@link CombatantSheet#applyEnchantment}.
     *
     * @return whether a compulsion landed — {@code false} for an immune target too
     */
    public boolean resolveChamarPraBriga(@NonNull final CombatantSheet holder, @NonNull final CombatantSheet target,
                                         final AttackSource attackSource, final int round) {
        if (!holds(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA) || !isNaturalWeaponAttack(holder, attackSource)) {
            return false;
        }
        holder.recordNaturalWeaponHit(target, round);
        ChamarPraBrigaMark mark = new ChamarPraBrigaMark(holder.getId());
        if (!holder.hasHitWithNaturalWeaponInConsecutiveRounds(target, round)
                || target.getCharacter() == null || !target.getCharacter().isWieldingAWeapon()
                || target.isAffectedThisCombat(mark)) {
            return false;
        }
        boolean landed = target.applyEnchantment(new ForcedTargeting(holder, CHAMAR_PRA_BRIGA_ROUNDS));
        if (landed) {
            target.markAffectedThisCombat(mark);
        }
        return landed;
    }

    /**
     * Agarrar e Derrubar's Grande Mestre upgrade — "Uma vez por Rodada você pode desferir um ataque
     * com Arma Natural contra um alvo Caído como Ação Livre". A permission the caller offers; the
     * once-per-Rodada limit is the caller's to keep, like every other Ação Livre here.
     */
    public boolean grantsFreeAttackAgainstFallen(@NonNull final CombatantSheet target, final SceneContext sceneContext) {
        return holdsGrandeMestreDasBrigas() && holds(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR)
                && target.hasCondition(ConditionType.CAIDO, sceneContext);
    }

    private static boolean isNaturalWeaponAttack(final CombatantSheet holder, final AttackSource attackSource) {
        return attackSource instanceof Weapon weapon && holder.getCharacter().treatsAsNaturalWeapon(weapon);
    }

    /** No Arma Natural attack yet in the holder's per-Rodada log. */
    private static boolean isFirstNaturalWeaponAttackOfRound(final CombatantSheet holder) {
        return holder.getActionsThisRound().stream()
                .filter(action -> action.skill() != null && action.skill().isAttackSkill())
                .map(CombatantAction::attackSource)
                .noneMatch(source -> isNaturalWeaponAttack(holder, source));
    }

    /**
     * "Rodada Ímpar" — the table counts from 1, this core from 0, so round 0 is the first (odd)
     * Rodada. No context reads as the first Rodada.
     */
    private static boolean isOddRound(final SceneContext sceneContext) {
        int round = sceneContext == null ? 0 : sceneContext.getCurrentRound();
        return round % 2 == 0;
    }

    /**
     * Whether attackTarget is held by a compulsion this holder cast through Fingir Fraquezas, for
     * Malícia de Valentão. Read as "bound to this holder <em>and</em> caught by Fingir Fraquezas
     * since their last Descanso" — the ForcedTargeting carries no record of which trait cast it.
     */
    private boolean isBoundByFingirFraquezas(final CombatantSheet holder, final CombatantSheet attackTarget) {
        if (attackTarget == null || !holdsMaliciaDeValentao() || !holds(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS)) {
            return false;
        }
        return attackTarget.isAffectedUntilRest(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS)
                && attackTarget.getForcedTargeting()
                        .map(compulsion -> compulsion.getEnchanter().getId().equals(holder.getId()))
                        .orElse(false);
    }
}
