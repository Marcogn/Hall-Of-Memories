package com.marcogn.hallofmemories.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {

    /** Hack library. Graph start destination. */
    @Serializable
    data object Home : Destination

    @Serializable
    data object Templates : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data class HackDetail(val hackId: String) : Destination

    /** [hackId] null creates a new hack, non-null edits the existing one. */
    @Serializable
    data class HackForm(val hackId: String? = null) : Destination

    @Serializable
    data class HofDetail(val entryId: String) : Destination

    /** [entryId] null creates a new Hall of Fame entry under [hackId], non-null edits it. */
    @Serializable
    data class HofForm(val hackId: String, val entryId: String? = null) : Destination
}
