package org.aventyrs.core.race;

/**
 * The broad creature classification every {@link Race} belongs to — used to validate a
 * Mestiço's chosen parent {@link Race} against rules text like "escolha uma raça Feérica,
 * Humanoide ou Monstruosa que não seja mestiça" (see {@code AbstractMesticoRace}/{@code
 * MeioElfo}). "Mestiço" itself is a separate {@link Race#isMestico()} flag, not a value here.
 *
 * <p>{@link #HUMANOIDE}/{@link #FEERICO}/{@link #MONSTRUOSO} are the three "base" types every
 * living race declares. {@link #RENASCIDO} is the Morto-Vivo classification — {@code Vampiro} is
 * the first race to report it. A Renascido's rules text routinely says its <em>prerequisites</em>
 * are still checked against its life-race's type ("conforme sua raça em vida"), which is why
 * {@link Race#getPrerequisiteCreatureType()} exists as a separate hook: {@code
 * getCreatureType()} is the true classification, {@code getPrerequisiteCreatureType()} is what a
 * {@code FeatRequirements#requiredCreatureType} gate compares against.
 *
 * <p>{@code RENASCIDO} has no <em>behavioural</em> consumer yet — the "Living/undead
 * classification" gap CLAUDE.md records (no vitality tag drives healing inversion, Divine-magic
 * immunity, or the no-sleep/no-breath exemptions). It is the identity those systems will key on
 * once they exist.
 *
 * <p>{@link #DRAGAO}/{@link #ELEMENTAL}/{@link #ABISSAL}/{@link #CELESTIAL} are the essence
 * classifications no playable {@link Race} reports — added for the "um Dragão, Elemental, Abissal
 * ou Celestial sacrifique voluntariamente suas Centelhas" donor clause of {@code
 * org.aventyrs.core.feat.ArtificeFeat#ARTESAO_DE_REGALIAS_DIVINAS} (checked by {@code
 * EquipmentCraftingService.RegaliaDonation#isDivineDonor()}), and the identity a bestiary will
 * key on once one exists.
 */
public enum CreatureType {
    HUMANOIDE,
    FEERICO,
    MONSTRUOSO,
    RENASCIDO,
    DRAGAO,
    ELEMENTAL,
    ABISSAL,
    CELESTIAL
}
