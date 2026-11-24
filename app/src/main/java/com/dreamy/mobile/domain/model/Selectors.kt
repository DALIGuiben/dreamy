package com.dreamy.mobile.domain.model

import com.dreamy.mobile.util.nextBirthday
import java.time.Instant

/** Participants still waiting for a mother's answer on events that requested approval. */
fun List<Event>.pendingApprovals(): Int = filter { it.status == EventStatus.AWAITING_APPROVAL }
    .sumOf { event -> event.participants.count { it.approvalStatus == ApprovalStatus.PENDING } }

fun List<Event>.upcoming(now: Instant = Instant.now()): List<Event> =
    filter { !it.eventDate.isBefore(now) }.sortedBy { it.eventDate }

fun List<Child>.byNextBirthday(): List<Child> = sortedBy { it.birthDate.nextBirthday() }
