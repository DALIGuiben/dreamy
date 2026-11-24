package com.dreamy.mobile.util

import com.dreamy.mobile.domain.model.ApprovalStatus
import com.dreamy.mobile.domain.model.EventStatus
import com.dreamy.mobile.domain.model.EventType

val EventStatus.label: String
    get() = when (this) {
        EventStatus.DRAFT -> "Brouillon"
        EventStatus.AWAITING_APPROVAL -> "Accord demandé"
        EventStatus.CHANGES_REQUESTED -> "Modifications demandées"
        EventStatus.CONFIRMED -> "Confirmé"
        EventStatus.COMPLETED -> "Terminé"
        EventStatus.CANCELLED -> "Annulé"
    }

val ApprovalStatus.label: String
    get() = when (this) {
        ApprovalStatus.PENDING -> "En attente"
        ApprovalStatus.APPROVED -> "Approuvé"
        ApprovalStatus.DECLINED -> "Refusé"
        ApprovalStatus.CHANGES_REQUESTED -> "Modification demandée"
    }

val EventType.label: String
    get() = if (this == EventType.BIRTHDAY) "Anniversaire" else "Événement SOS"
