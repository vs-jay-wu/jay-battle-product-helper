package com.viewsonic.classswift.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * Stable, explicit design-node id used by the Designer Shell's design-mode inspector.
 *
 * Tag components with [designNode] (e.g. `Modifier.designNode("score_card")`) so the
 * inspector — and the AI agent receiving the selection — can refer to the exact element
 * by a name that survives rebuilds (unlike auto-generated indices).
 */
val DesignNodeIdKey = SemanticsPropertyKey<String>("DesignNodeId")

var SemanticsPropertyReceiver.designNodeId: String by DesignNodeIdKey

fun Modifier.designNode(id: String): Modifier = semantics { designNodeId = id }
