package org.aventyrs.core.campaign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.aventyrs.core.sheet.IllegalOperationException;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static org.aventyrs.core.util.TranslatableMessages.PROGRESSION_LOCKED_DURING_SESSION;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_ALREADY_ONGOING;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_ALREADY_OPEN;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_NOT_FOUND;

/**
 * A Campanha: the {@link Session}s played so far, numbered 1..n, and the {@code CharacterSheet}s
 * taking part, referenced by id.
 *
 * <p><b>The progression lock.</b> While a Sessão is {@link SessionStatus#ONGOING}, a participant
 * may not progress. That covers new Talentos, Graduações, Títulos and Habilidades de Título,
 * Especializações, Habilidades de Competência and de Atributo, and a raised Atributo base.
 * {@link #isProgressionLocked()} answers that question and {@link #requireProgressionAllowed()}
 * enforces it. <b>This class doesn't call either one itself</b>, and neither do the progression
 * services: the caller holding the current Campanha checks before it calls them. See
 * this package's {@code package-info}.
 *
 * <p>At most one Sessão is open (CREATED or ONGOING) at a time, so at most one is ONGOING.
 */
@Getter
public class Campaign {

    @NonNull
    private final UUID id;
    @NonNull
    @Setter
    private String name;
    private final List<Session> sessions = new ArrayList<>();
    private final Set<UUID> participantSheetIds = new LinkedHashSet<>();

    private Campaign(final UUID id, final String name) {
        this.id = id;
        this.name = name;
    }

    public static Campaign create(@NonNull final String name) {
        return new Campaign(UUID.randomUUID(), name);
    }

    /**
     * Rebuilds a stored Campanha. Sessions are ordered by number and must run 1..n with no gaps,
     * and at most one may be open — otherwise every later numbering and lock answer would be wrong.
     *
     * @throws IllegalArgumentException if the stored sessions break either rule
     */
    public static Campaign of(@NonNull final UUID id, @NonNull final String name,
                              @NonNull final Collection<Session> sessions,
                              @NonNull final Collection<UUID> participantSheetIds) {
        Campaign campaign = new Campaign(id, name);
        List<Session> ordered = sessions.stream().sorted(Comparator.comparingInt(Session::getNumber)).toList();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getNumber() != i + 1) {
                throw new IllegalArgumentException("Session numbers must run 1..n without gaps");
            }
        }
        if (ordered.stream().filter(Session::isOpen).count() > 1) {
            throw new IllegalArgumentException("At most one session may be CREATED or ONGOING");
        }
        campaign.sessions.addAll(ordered);
        campaign.participantSheetIds.addAll(participantSheetIds);
        return campaign;
    }

    public List<Session> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public Set<UUID> getParticipantSheetIds() {
        return Collections.unmodifiableSet(participantSheetIds);
    }

    /**
     * Adds the next Sessão, numbered one past the last, in {@link SessionStatus#CREATED}.
     *
     * @throws IllegalOperationException {@code SESSION_ALREADY_OPEN} while another Sessão is not yet ENDED
     */
    public Session createSession() {
        if (sessions.stream().anyMatch(Session::isOpen)) {
            throw new IllegalOperationException(SESSION_ALREADY_OPEN);
        }
        Session session = Session.create(sessions.size() + 1);
        sessions.add(session);
        return session;
    }

    /**
     * @throws IllegalOperationException {@code SESSION_NOT_FOUND}; {@code SESSION_ALREADY_ONGOING} if
     *                                   another Sessão is being played; {@code INVALID_SESSION_TRANSITION}
     *                                   if this one isn't CREATED
     */
    public Session startSession(final int number) {
        Session session = getSession(number);
        if (getOngoingSession().filter(ongoing -> ongoing != session).isPresent()) {
            throw new IllegalOperationException(SESSION_ALREADY_ONGOING);
        }
        session.start();
        return session;
    }

    /**
     * @throws IllegalOperationException {@code SESSION_NOT_FOUND}; {@code INVALID_SESSION_TRANSITION}
     *                                   if this one isn't ONGOING
     */
    public Session endSession(final int number) {
        Session session = getSession(number);
        session.end();
        return session;
    }

    /** @throws IllegalOperationException {@code SESSION_NOT_FOUND} */
    public Session getSession(final int number) {
        return findSession(number).orElseThrow(() -> new IllegalOperationException(SESSION_NOT_FOUND));
    }

    public Optional<Session> findSession(final int number) {
        return sessions.stream().filter(session -> session.getNumber() == number).findFirst();
    }

    public Optional<Session> getOngoingSession() {
        return sessions.stream().filter(Session::isOngoing).findFirst();
    }

    /** Whether participants are currently barred from progressing — a Sessão is ONGOING. */
    public boolean isProgressionLocked() {
        return getOngoingSession().isPresent();
    }

    /**
     * Call before any progression service for one of this Campanha's participants.
     *
     * @throws IllegalOperationException {@code PROGRESSION_LOCKED_DURING_SESSION} while a Sessão is ONGOING
     */
    public void requireProgressionAllowed() {
        if (isProgressionLocked()) {
            throw new IllegalOperationException(PROGRESSION_LOCKED_DURING_SESSION);
        }
    }

    /** Membership may change at any time, mid-Sessão included. Returns whether it was newly added. */
    public boolean addParticipant(@NonNull final UUID characterSheetId) {
        return participantSheetIds.add(characterSheetId);
    }

    public boolean removeParticipant(@NonNull final UUID characterSheetId) {
        return participantSheetIds.remove(characterSheetId);
    }

    public boolean isParticipant(@NonNull final UUID characterSheetId) {
        return participantSheetIds.contains(characterSheetId);
    }
}
