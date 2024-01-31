package com.mbrlabs.mundus.editor.events

/**
 * @author JamesTKhan
 * @version November 02, 2022
 */
class UpdateNoiseTextureEvent {
    interface UpdateNoiseTextureListener {
        @Subscribe
        fun onTextureUpdate(event: UpdateNoiseTextureEvent)
    }
}