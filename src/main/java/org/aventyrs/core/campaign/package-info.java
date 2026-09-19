/**
 * Campanhas and their Sessões — and the rule that a character does not progress mid-Sessão.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link org.aventyrs.core.campaign.Campaign#create(String)}, then
 *       {@link org.aventyrs.core.campaign.Campaign#addParticipant(java.util.UUID)} with each
 *       {@code CharacterSheet} id.</li>
 *   <li>{@link org.aventyrs.core.campaign.Campaign#createSession()} — numbered 1, 2, 3… in
 *       {@code CREATED}. Only one Sessão may be open (not {@code ENDED}) at a time.</li>
 *   <li>{@link org.aventyrs.core.campaign.Campaign#startSession(int)} → {@code ONGOING}, then
 *       {@link org.aventyrs.core.campaign.Campaign#endSession(int)} → {@code ENDED}. Nothing moves
 *       backwards.</li>
 * </ol>
 *
 * <h2>The progression lock is the caller's to enforce</h2>
 * While a Sessão is {@code ONGOING}, a participant may not progress. <b>None of the progression
 * services check this</b> — this library stores nothing, so any {@code Campaign} it received
 * could be out of date. Whoever holds the current Campanha calls
 * {@link org.aventyrs.core.campaign.Campaign#requireProgressionAllowed()} (or reads
 * {@link org.aventyrs.core.campaign.Campaign#isProgressionLocked()}) before any of:
 * <ul>
 *   <li>{@code FeatService#grantFeat}</li>
 *   <li>{@code SkillGraduationService#upgradeGraduation}</li>
 *   <li>{@code CharacterAttributeService#upgradeBase}</li>
 *   <li>{@code AttributeAbilityService#grantAttributeAbility}</li>
 *   <li>{@code AttributeAbilityService#grantCompetencyAbilityChoice}</li>
 *   <li>{@code AttributeAbilityService#grantSpecializationChoice}</li>
 *   <li>{@code TitleAcquisitionService#grantTitle}</li>
 *   <li>{@code TitleAbilityService#grantTitleAbility}</li>
 * </ul>
 * Character creation ({@code CharacterCreationService}, starting Talentos included) is not
 * progression and is not locked.
 *
 * <h2>When a Sessão ends</h2>
 * After {@link org.aventyrs.core.campaign.Campaign#endSession(int)}, call {@code
 * CharacterSheet#applySessionEndAcquisitions()} on each participant's sheet. It grants whatever a
 * held Talento owed "ao fim da sessão", such as the Título Primário of {@code
 * DestinoFeat#DESPERTAR_ANTECIPADO}, and returns what it granted so the caller can save the
 * changed sheet. A {@code Campaign} knows participants only by id, so it cannot make this call
 * itself. Calling it twice is harmless.
 *
 * <p>This is not the same thing as {@code CombatantSheet#consumeOncePerSession}, whose "session"
 * is the sheet object's lifetime in the running client. The two are not connected yet.
 */
package org.aventyrs.core.campaign;
