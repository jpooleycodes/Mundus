package com.mbrlabs.mundus.editor.ui.widgets

import com.kotcrab.vis.ui.widget.file.FileChooser
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.preferences.MundusPreferencesManager
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent

/**
 * Extends FileChooser to add behavior that sets the current chooser directory to the
 * current projects last opened directory on project change
 *
 * @author JamesTKhan
 * @version July 19, 2022
 */
class PersistingFileChooser(mode: Mode) : FileChooser(mode), ProjectChangedEvent.ProjectChangedListener {

    private var projectManager: ProjectManager = Mundus.inject()

    init {
        Mundus.registerEventListener(this)
    }

    /**
     * Load the last opened directory for this project, and set that as the current directory for filechooser
     */
    private fun setLastOpenedDirectory() {
        if (projectManager.current().projectPref.contains(MundusPreferencesManager.PROJ_LAST_DIR)) {
            val path = projectManager.current().projectPref.getString(MundusPreferencesManager.PROJ_LAST_DIR)
            setDirectory(path)
        }
    }

    override fun fadeOut() {
        super.fadeOut()
        // On fade out of the chooser, store the last opened directory
        projectManager.current().projectPref.set(MundusPreferencesManager.PROJ_LAST_DIR, currentDirectory.pathWithoutExtension())
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        setLastOpenedDirectory()
    }
}