package com.viewsonic.classswift.core.ui

import androidx.compose.ui.Modifier
import com.viewsonic.designer.node.designNode as designNodeImpl

/**
 * Re-export shim. The design-node primitive moved to the standalone, reusable
 * [com.viewsonic.designer.node] module (shared by the multiplatform app UI and the
 * desktop :designer-bridge). These aliases keep the existing
 * `com.viewsonic.classswift.core.ui.designNode` call sites (and DesignCanvas) working
 * unchanged. Prefer importing from `com.viewsonic.designer.node` in new code.
 */
typealias DesignNodeRegistry = com.viewsonic.designer.node.DesignNodeRegistry
typealias DesignNodeInfo = com.viewsonic.designer.node.DesignNodeInfo

val LocalDesignNodeRegistry get() = com.viewsonic.designer.node.LocalDesignNodeRegistry

fun Modifier.designNode(id: String): Modifier = designNodeImpl(id)
