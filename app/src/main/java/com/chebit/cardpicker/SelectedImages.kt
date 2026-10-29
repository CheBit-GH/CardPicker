package com.chebit.cardpicker

/**
 * The current hand, shared between the grid and the fullscreen viewer.
 * Hands are small (typically 3-10 cards), so holding them in memory is fine.
 */
object SelectedImages {
    var items: List<ImageRef> = emptyList()
}
