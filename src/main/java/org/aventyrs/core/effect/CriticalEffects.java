package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.character.EgoDomain;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the Efeito Crítico a {@link CriticalEffectType} names — the single place an identity from
 * the catalog becomes something that can be applied. What {@code AttackDelivery} and {@code
 * AttackReceiver} use for a weapon's or a Magia's own Efeito Crítico, a Título's additional one, and
 * an {@code EmpoweredAttack}'s.
 *
 * <p>Two reasons an identity yields nothing:
 * <ul>
 *   <li><b>No mechanism</b> — {@link #UNIMPLEMENTED}: Desmembrar needs a body with limbs, and this
 *   core models none.</li>
 *   <li><b>No dice</b> — {@link #requiresDice(CriticalEffectType, CriticalEffectContext)}: an effect that throws its own dice is not built from
 *   a context carrying no {@code DiceRoller}, because this core never rolls.</li>
 * </ul>
 * Either way the caller is told by an empty result, never handed a half-built effect.
 */
public final class CriticalEffects {

    /** Identities with no mechanism behind them. */
    public static final Set<CriticalEffectType> UNIMPLEMENTED = EnumSet.of(CriticalEffectType.DESMEMBRAR);

    /** Identities whose effect throws dice of its own at both severities. */
    private static final Set<CriticalEffectType> DICE_BEARING = EnumSet.of(
            CriticalEffectType.AMALDICOAR, CriticalEffectType.OFERENDA_MALDITA, CriticalEffectType.POTENCIALIZAR,
            CriticalEffectType.PRIMOR);

    /** Identities whose Menor alone picks at random — the Maior hits everything it names. */
    private static final Set<CriticalEffectType> MINOR_DICE_BEARING = EnumSet.of(
            CriticalEffectType.DILACERAR, CriticalEffectType.ESTILHACADOR);

    private CriticalEffects() {
    }

    /** Whether building type at context's severity needs a {@code DiceRoller}. */
    public static boolean requiresDice(@NonNull final CriticalEffectType type,
                                       @NonNull final CriticalEffectContext context) {
        return DICE_BEARING.contains(type) || (MINOR_DICE_BEARING.contains(type) && !context.isMajor());
    }

    /** Whether {@link #create} would build type for context — without building it, so without dice. */
    public static boolean canCreate(@NonNull final CriticalEffectType type, @NonNull final CriticalEffectContext context) {
        return !UNIMPLEMENTED.contains(type) && !(requiresDice(type, context) && context.dice() == null);
    }

    /**
     * The Efeito Crítico type names, built for context — empty when it has no mechanism, or when it
     * throws dice and context carries no roller.
     */
    public static Optional<CriticalEffect> create(@NonNull final CriticalEffectType type,
                                                  @NonNull final CriticalEffectContext context) {
        if (!canCreate(type, context)) {
            return Optional.empty();
        }
        return Optional.of(switch (type) {
            case SANGRAMENTO -> new Sangramento(context.criticalResult());
            case PURGA_DE_MANA -> new ManaPurge(context.criticalResult());
            // "definido aleatoriamente": a d6, 1–3 Sorte, 4–6 Autocontrole.
            case PRIMOR -> new Primor(context.criticalResult(),
                    context.dice().rollD6() <= 3 ? EgoDomain.SORTE : EgoDomain.AUTOCONTROLE);
            case SABOTAGEM -> new Sabotage(context.criticalResult());
            case EXECUCAO_REAL -> new RealExecution(context.criticalResult());
            case AMALDICOAR -> new Amaldicoar(context);
            case AMENIZAR -> new Amenizar(context);
            case ATORDOANTE -> new Atordoante(context);
            case CATACLISMO -> new Cataclismo(context);
            case DILACERAR -> new Dilacerar(context);
            case EMPALAR -> new Empalar(context);
            case ESTILHACADOR -> new Estilhacador(context);
            case EXCRUCIANTE -> new Excruciante(context);
            case FERIDA_PROFUNDA -> new FeridaProfunda(context);
            case FORTALECER -> new Fortalecer(context);
            case GUILHOTINA -> new Guilhotina(context);
            case IMUNIZAR -> new Imunizar(context);
            case INFLAMAR -> new Inflamar(context);
            case OFERENDA_MALDITA -> new OferendaMaldita(context);
            case POTENCIALIZAR -> new Potencializar(context);
            case PREVENIR -> new Prevenir(context);
            case TOQUE_DO_AETHER -> new ToqueDoAether(context);
            case DESMEMBRAR -> throw new IllegalStateException("unreachable: DESMEMBRAR is UNIMPLEMENTED");
        });
    }
}
