package org.aventyrs.core.title.giganteenfurecido;

import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DelayedEgoGrant;
import org.aventyrs.core.sheet.Exhaustion;
import org.aventyrs.core.sheet.Frenzy;
import org.aventyrs.core.sheet.FrenzyMode;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import org.aventyrs.core.title.TitleAttackModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.FRENZY_CANNOT_END_VOLUNTARILY;
import static org.aventyrs.core.util.TranslatableMessages.FRENZY_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.REQUIRED_TITLE_TRAIT_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_ABILITY_PREREQUISITE_NOT_MET;

/**
 * Gigante Enfurecido — "Paixão, raiva e instinto … confiam sua vida aos seus instintos e emoções", the
 * third concrete Título Aventyr and the third <em>shape</em> of one. Santo acts through activations,
 * Senhor da Briga through the attack itself; this one acts through a <b>state</b>: the {@link Frenzy}
 * its Despertar starts ({@link GiganteEnfurecidoDespertar#FRENESI}), which its Especializações and
 * Supremas extend with modes, and which every other trait reads.
 *
 * <p>Two more things set it apart. It pays in <b>Autocontrole</b> ({@code EgoCost}) rather than only
 * PD, with a ledger of what a Frenesi spent ({@link #recordFrenzySpend}) that comes back by the hour
 * or, under Uno com a Ira, after two Rodadas. And three of its traits are Reações on events no other
 * Título reacts to — being hit, an adjacent enemy attacking someone else, and a hit that would drop the
 * holder to 0 PV.
 *
 * <p>What the caller must do beyond activating is small and named: call {@link #recordHitTaken} after
 * a hit damages a Berserker, {@link #recordTriumph} after a kill or a critical, and honour {@code
 * CombatantSheet#isCompelledToAttackNearest()} when picking a target.
 */
public class GiganteEnfurecido implements AventyrTitle {

    private static final String BASE_EFFECT_DESCRIPTION = GiganteEnfurecidoDespertar.FRENESI.getDescription();

    private static final String PRIMARY_TITLE_BONUS_DESCRIPTION =
            "Se este for o seu título primário seu 'Frenesi' tem a duração aumentada em +2 Rodadas.";

    /** "Bônus Variável de +2 em 'Força' e 'Iniciativa'". */
    static final int FRENZY_STRENGTH_BONUS = 2;
    static final int FRENZY_INITIATIVE_BONUS = 2;
    /** "uma quantidade de Rodadas igual 2+ Metade do seu 'Vigor'". */
    static final int FRENZY_BASE_ROUNDS = 2;
    /** Título Primário: "tem a duração aumentada em +2 Rodadas". */
    static final int PRIMARY_EXTRA_ROUNDS = 2;
    /** "recuperados 1 a cada 2 horas passadas fora deste estado". */
    static final int HOURS_PER_RECOVERED_POINT = 2;
    /** Uno com a Ira: "Os 2 primeiros pontos … são recuperados após 2 Rodadas". */
    static final int UNO_COM_A_IRA_POINTS = 2;
    static final int UNO_COM_A_IRA_ROUNDS = 2;
    /** Titã Enlouquecido: "sua Categoria de Tamanho e seu Multiplicador de PV aumentam ambos em +1". */
    static final int TITA_SIZE = 1;
    static final int TITA_LIFE_MULTIPLIER = 1;
    /** Colosso Enfurecido: Tamanho +1 more, PD e PM Multiplicadores +1. */
    static final int COLOSSO_SIZE = 1;
    static final int COLOSSO_MULTIPLIERS = 1;
    /** Berserker: "Bônus de +1 em Força e sua Margem Crítica Menor aumenta em +1 número". */
    static final int BERSERKER_STRENGTH = 1;
    static final int BERSERKER_MARGIN = 1;
    /** Berserker, when hit: "Redutor de -2 em suas Defesas e Bônus de +1 em Força". */
    static final int BERSERKER_HIT_DEFESAS = -2;
    static final int BERSERKER_HIT_STRENGTH = 1;
    /** Desprezar Danos: "você recebe RA" — one instance. */
    static final int DESPREZAR_ABSOLUTE_REDUCTION = 2;

    /** Berserker's "A cada Cena … apenas 1 vez para cada inimigo atacante", keyed per attacker. */
    record BerserkerHit(UUID attackerId) {
    }

    /** A "ao derrotar um inimigo … ou após desferir um Acerto Crítico" the caller reported. */
    enum Triumph { MARK }

    /** "apenas uma vez a cada Rodada", keyed per trait, Rodada and (where it says so) enemy. */
    record OncePerRound(Object trait, int round, UUID target) {
    }

    private final List<GiganteEnfurecidoSpecialization> specializations;
    private final List<AventyrTitleAbility> abilities;

    public GiganteEnfurecido(@NonNull final List<GiganteEnfurecidoSpecialization> specializations,
                             @NonNull final List<AventyrTitleAbility> abilities) {
        this.specializations = new ArrayList<>(specializations);
        this.abilities = new ArrayList<>(abilities);
    }

    /** The Gigante Enfurecido sheet's Character holds, if any. */
    public static Optional<GiganteEnfurecido> heldBy(@NonNull final CombatantSheet sheet) {
        return sheet.getCharacter().getAllTitles().stream()
                .filter(GiganteEnfurecido.class::isInstance)
                .map(GiganteEnfurecido.class::cast)
                .findFirst();
    }

    /** {@link #heldBy}, refusing a sheet that holds none. */
    static GiganteEnfurecido requireHeldBy(final CombatantSheet sheet) {
        return heldBy(sheet).orElseThrow(() -> new IllegalOperationException(REQUIRED_TITLE_TRAIT_NOT_HELD));
    }

    /** "Centelha Bruta" — the header Gigante Enfurecido is listed under. */
    @Override
    public TitleArchetype getArchetype() {
        return TitleArchetype.BRUTO;
    }

    @Override
    public String getName() {
        return "Gigante Enfurecido";
    }

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

    /** What was acquired — never the Despertar, which is held from Awakening and counts toward no prerequisite. */
    @Override
    public List<AventyrTitleAbility> getAbilities() {
        return abilities;
    }

    /** Every trait held, the Despertar's Frenesi first — so it can be activated like any other. */
    @Override
    public List<AventyrTitleAbility> getAllAbilities() {
        return Stream.concat(Stream.of(GiganteEnfurecidoDespertar.FRENESI),
                AventyrTitle.super.getAllAbilities().stream()).toList();
    }

    @Override
    public void grantAbility(final AventyrTitleAbility ability) {
        abilities.add(ability);
    }

    /** Refuses anything but this Título's own two — "apenas 'Gigantes Enfurecidos' podem adquirir". */
    @Override
    public void grantSpecialization(final AventyrTitleSpecialization specialization) {
        if (!(specialization instanceof GiganteEnfurecidoSpecialization own)) {
            throw new IllegalOperationException(TITLE_ABILITY_PREREQUISITE_NOT_MET);
        }
        specializations.add(own);
    }

    /** Whether this Título holds trait. */
    public boolean holds(final AventyrTitleAbility trait) {
        return getAllAbilities().contains(trait);
    }

    /** "2+ Metade do seu 'Vigor'", +2 as the holder's Título Primário. */
    public int resolveFrenzyRounds(@NonNull final CombatantSheet holder) {
        Character character = holder.getCharacter();
        int rounds = FRENZY_BASE_ROUNDS + character.getEffectiveAttributeTotal(AttributeDomain.VIGOR, holder) / 2;
        return character.getPrimaryTitle() == this ? rounds + PRIMARY_EXTRA_ROUNDS : rounds;
    }

    /**
     * Starts holder's Frenesi with modes (the Especializações activated with it), having paid
     * autocontrolePaid temporary Autocontrole for it — the one place a Frenesi is built, whether by
     * the Despertar or by Frenesi Reativo.
     */
    Frenzy startFrenzy(final CombatantSheet holder, final Set<FrenzyMode> modes, final int autocontrolePaid) {
        Frenzy frenzy = new Frenzy(holder.getId(), resolveFrenzyRounds(holder));
        frenzy.addBonus(ModifierType.STRENGTH_BONUS, FRENZY_STRENGTH_BONUS);
        frenzy.addBonus(ModifierType.INITIATIVE, FRENZY_INITIATIVE_BONUS);
        if (modes.contains(FrenzyMode.TITA_ENLOUQUECIDO)) {
            frenzy.addMode(FrenzyMode.TITA_ENLOUQUECIDO);
            frenzy.addBonus(ModifierType.SIZE_CATEGORY, TITA_SIZE);
            frenzy.addBonus(ModifierType.LIFE_MULTIPLIER, TITA_LIFE_MULTIPLIER);
            frenzy.liftConcentrationBlock();
            if (holds(TitaEnlouquecidoAbility.COLOSSO_ENFURECIDO)) {
                frenzy.addBonus(ModifierType.SIZE_CATEGORY, COLOSSO_SIZE);
                frenzy.addBonus(ModifierType.DETERMINATION_MULTIPLIER, COLOSSO_MULTIPLIERS);
                frenzy.addBonus(ModifierType.MANA_MULTIPLIER, COLOSSO_MULTIPLIERS);
            }
        }
        if (modes.contains(FrenzyMode.BERSERKER)) {
            frenzy.addMode(FrenzyMode.BERSERKER);
            frenzy.addBonus(ModifierType.STRENGTH_BONUS, BERSERKER_STRENGTH);
            frenzy.addBonus(ModifierType.LESSER_CRITICAL_MARGIN, BERSERKER_MARGIN);
        }
        if (holds(BerserkerAbility.DESPREZAR_DANOS)) {
            frenzy.addBonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION, DESPREZAR_ABSOLUTE_REDUCTION);
            frenzy.markScornsDamage();
        }
        holder.startFrenzy(frenzy);
        recordFrenzySpend(holder, autocontrolePaid);
        return frenzy;
    }

    /**
     * Books points of temporary Autocontrole holder just spent on a Frenesi effect: owed back "1 a
     * cada 2 horas passadas fora deste estado", and — under Uno com a Ira — the part of it falling
     * within this Frenesi's first 2 points scheduled back "após 2 Rodadas, mas apenas se seu Frenesi
     * ainda estiver ativo e você estiver consciente".
     */
    void recordFrenzySpend(final CombatantSheet holder, final int points) {
        if (points <= 0) {
            return;
        }
        holder.oweHourlyEgoRecovery(EgoDomain.AUTOCONTROLE, points, HOURS_PER_RECOVERED_POINT);
        Optional<Frenzy> own = holder.getOwnFrenzy();
        if (own.isEmpty()) {
            return;
        }
        Frenzy frenzy = own.get();
        int before = frenzy.getAutocontroleSpent();
        int after = frenzy.recordAutocontroleSpent(points);
        int returned = Math.min(after, UNO_COM_A_IRA_POINTS) - Math.min(before, UNO_COM_A_IRA_POINTS);
        if (returned > 0 && holds(GiganteEnfurecidoAbility.UNO_COM_A_IRA)) {
            holder.scheduleTemporaryEgoPointGrant(new DelayedEgoGrant(EgoDomain.AUTOCONTROLE,
                    GiganteEnfurecidoAbility.UNO_COM_A_IRA, returned, UNO_COM_A_IRA_ROUNDS,
                    sheet -> sheet.getOwnFrenzy().filter(running -> running == frenzy).isPresent()
                            && isConscious(sheet),
                    true));
        }
    }

    /** "você estiver consciente" — anything short of Coma. */
    static boolean isConscious(final CombatantSheet sheet) {
        CharacterStatus status = new HitPointsServiceImpl().getStatus(sheet);
        return status != CharacterStatus.COMMA && status != CharacterStatus.DEAD;
    }

    /**
     * Uno com a Ira: "Você agora pode encerrar seu Frenesi voluntariamente, mas encerra-lo de
     * antecipadamente te deixa física e mentalmente exausto … até que passe por um Descanso Curto
     * Verdadeiro".
     *
     * @throws IllegalOperationException {@code FRENZY_CANNOT_END_VOLUNTARILY} without Uno com a Ira,
     *         {@code FRENZY_REQUIRED} with no Frenesi running
     */
    public void endFrenzyVoluntarily(@NonNull final CombatantSheet holder) {
        if (!holds(GiganteEnfurecidoAbility.UNO_COM_A_IRA)) {
            throw new IllegalOperationException(FRENZY_CANNOT_END_VOLUNTARILY);
        }
        if (!holder.endFrenzy()) {
            throw new IllegalOperationException(FRENZY_REQUIRED);
        }
        holder.applyEffectUntilTrueRest(new Exhaustion(GiganteEnfurecidoAbility.UNO_COM_A_IRA), RestType.CURTO);
    }

    /**
     * Berserker: "Sempre que for alvo de um ataque bem-sucedido e sofrer danos você recebe Redutor de -2
     * em suas Defesas e Bônus de +1 em Força. A cada Cena esta habilidade pode ser ativada apenas 1 vez
     * para cada inimigo atacante." The caller calls this once the damage has landed; a no-op unless
     * holder's own Frenesi runs with Berserker active and attacker has not triggered it this Cena.
     *
     * @return whether it stacked
     */
    public boolean recordHitTaken(@NonNull final CombatantSheet holder, @NonNull final CombatantSheet attacker) {
        Optional<Frenzy> frenzy = holder.getOwnFrenzy().filter(running -> running.hasMode(FrenzyMode.BERSERKER));
        BerserkerHit mark = new BerserkerHit(attacker.getId());
        if (frenzy.isEmpty() || holder.isAffectedThisCombat(mark)) {
            return false;
        }
        holder.markAffectedThisCombat(mark);
        frenzy.get().addBonus(ModifierType.DEFESAS, BERSERKER_HIT_DEFESAS);
        frenzy.get().addBonus(ModifierType.STRENGTH_BONUS, BERSERKER_HIT_STRENGTH);
        return true;
    }

    /**
     * Reports that holder just defeated an enemy or landed an Acerto Crítico — what Frenesi Assustador
     * "só pode ser ativada" after. Opens the window for the rest of holder's Turn.
     */
    public void recordTriumph(@NonNull final CombatantSheet holder) {
        holder.openActivationWindow(Triumph.MARK, 1);
    }

    /** Whether a {@link #recordTriumph} is still open on holder. */
    public static boolean hasTriumph(final CombatantSheet holder) {
        return holder.hasActivationWindow(Triumph.MARK);
    }

    /** The Rodada a sceneContext is in, or 0 outside one. */
    static int currentRound(final SceneContext sceneContext) {
        return sceneContext == null ? 0 : sceneContext.getCurrentRound();
    }

    /**
     * Retaliação Furiosa's attack: "o dano causado será 1d6+Metade da Força, independente da arma que
     * esteja utilizando. Se seus PV forem menores ou iguais a zero este dano muda para 2d6+Força
     * integral" — only for the attack against the enemy retaliated against.
     */
    @Override
    public TitleAttackModifiers resolveAttackModifiers(final CombatantSheet holder, final AttackSource attackSource,
                                                       final CombatantSheet attackTarget,
                                                       final SceneContext sceneContext) {
        if (holder == null || attackTarget == null
                || !RetaliacaoFuriosaInteraction.hasRetaliationAgainst(holder, attackTarget)) {
            return TitleAttackModifiers.NONE;
        }
        int strength = holder.getCharacter().getEffectiveAttributeTotal(AttributeDomain.STRENGTH, holder);
        TitleAttackModifiers.DamageOverride damage = holder.isAtOrBelowZeroHitPoints()
                ? new TitleAttackModifiers.DamageOverride(2, strength)
                : new TitleAttackModifiers.DamageOverride(1, strength / 2);
        return new TitleAttackModifiers(0, 0, null, null, null, List.of(), damage);
    }

    /** Spends the retaliation attack, once made. */
    @Override
    public void consumeAttackCharges(final CombatantSheet holder, final AttackSource attackSource) {
        holder.consumeEnhancedAttack(BerserkerAbility.RETALIACAO_FURIOSA);
    }
}
