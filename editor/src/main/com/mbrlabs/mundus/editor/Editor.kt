/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.editor

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mbrlabs.mundus.commons.utils.DebugRenderer
import com.mbrlabs.mundus.commons.utils.ShaderUtils
import com.mbrlabs.mundus.editor.core.plugin.PluginManagerProvider
import com.mbrlabs.mundus.editor.core.project.ProjectAlreadyImportedException
import com.mbrlabs.mundus.editor.core.project.ProjectContext
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.core.registry.ProjectRef
import com.mbrlabs.mundus.editor.core.registry.Registry
import com.mbrlabs.mundus.editor.events.FilesDroppedEvent
import com.mbrlabs.mundus.editor.events.FullScreenEvent
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.events.PluginsLoadedEvent
import com.mbrlabs.mundus.editor.input.FreeCamController
import com.mbrlabs.mundus.editor.input.InputManager
import com.mbrlabs.mundus.editor.input.ShortcutController
import com.mbrlabs.mundus.editor.plugin.AssetManagerImpl
import com.mbrlabs.mundus.editor.plugin.ToasterManagerImpl
import com.mbrlabs.mundus.editor.preferences.MundusPreferencesManager
import com.mbrlabs.mundus.editor.profiling.MundusGLProfiler
import com.mbrlabs.mundus.editor.shader.EditorShaderProvider
import com.mbrlabs.mundus.editor.tools.ToolManager
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.gizmos.GizmoManager
import com.mbrlabs.mundus.editor.utils.Colors
import com.mbrlabs.mundus.editor.utils.Compass
import com.mbrlabs.mundus.editor.utils.GlUtils
import com.mbrlabs.mundus.editor.utils.UsefulMeshs
import com.mbrlabs.mundus.pluginapi.EventExtension
import com.mbrlabs.mundus.pluginapi.manager.PluginEventManager
import com.mbrlabs.mundus.pluginapi.RenderExtension
import com.mbrlabs.mundus.pluginapi.TerrainSceneExtension
import com.mbrlabs.mundus.editorcommons.events.GameObjectModifiedEvent
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent
import com.mbrlabs.mundus.pluginapi.AssetExtension
import com.mbrlabs.mundus.pluginapi.CustomShaderRenderExtension
import com.mbrlabs.mundus.pluginapi.DisposeExtension
import com.mbrlabs.mundus.pluginapi.ToasterExtension
import net.mgsx.gltf.scene3d.scene.SceneRenderableSorter
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.lwjgl.opengl.GL11
import org.pf4j.PluginManager
import java.io.File

/**
 * @author Marcus Brummer
 * @version 07-06-2016
 */
class Editor : Lwjgl3WindowAdapter(), ApplicationListener,
        ProjectChangedEvent.ProjectChangedListener,
        SceneChangedEvent.SceneChangedListener,
        FullScreenEvent.FullScreenEventListener,
        GameObjectModifiedEvent.GameObjectModifiedListener {

    private lateinit var axesInstance: ModelInstance
    private lateinit var compass: Compass

    private lateinit var camController: FreeCamController
    private lateinit var guiCamera: OrthographicCamera
    private lateinit var shortcutController: ShortcutController
    private lateinit var inputManager: InputManager
    private lateinit var projectManager: ProjectManager
    private lateinit var registry: Registry
    private lateinit var toolManager: ToolManager
    private lateinit var gizmoManager: GizmoManager
    private lateinit var glProfiler: MundusGLProfiler
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var debugRenderer: DebugRenderer
    private lateinit var globalPreferencesManager: MundusPreferencesManager
    private lateinit var pluginManager: PluginManager

    override fun create() {
        Mundus.registerEventListener(this)
        camController = Mundus.inject()
        shortcutController = Mundus.inject()
        inputManager = Mundus.inject()
        projectManager = Mundus.inject()
        registry = Mundus.inject()
        toolManager = Mundus.inject()
        gizmoManager = Mundus.inject()
        glProfiler = Mundus.inject()
        shapeRenderer = Mundus.inject()
        debugRenderer = Mundus.inject()
        globalPreferencesManager = Mundus.inject()
        pluginManager = Mundus.inject<PluginManagerProvider>().pluginManager
        setupInput()

        debugRenderer.isEnabled = globalPreferencesManager.getBoolean(MundusPreferencesManager.GLOB_BOOL_DEBUG_RENDERER_ON, false)
        debugRenderer.isAppearOnTop = globalPreferencesManager.getBoolean(MundusPreferencesManager.GLOB_BOOL_DEBUG_RENDERER_DEPTH_OFF, false)
        debugRenderer.isShowFacingArrow = globalPreferencesManager.getBoolean(MundusPreferencesManager.GLOB_BOOL_DEBUG_FACING_ARROW, false)
        // TODO dispose this
        val axesModel = UsefulMeshs.createAxes()
        axesInstance = ModelInstance(axesModel)

        // open last edited project or create default project
        var context: ProjectContext? = projectManager.loadLastProjectAsync()
        if (context == null) {
            context = createDefaultProject()
            projectManager.startAsyncProjectLoad(context!!.path, context)
        }

        guiCamera = OrthographicCamera()
        guiCamera.setToOrtho(
            false,
            UI.viewport.screenWidth.toFloat(),
            UI.viewport.screenHeight.toFloat()
        )

        UI.toggleLoadingScreen(true, context.name)

        initPluginSystem()
    }

    private fun setupInput() {
        // NOTE: order in which processors are added is important: first added,
        // first executed!
        inputManager.addProcessor(shortcutController)
        inputManager.addProcessor(UI)
        // when user does not click on a ui element -> unfocus UI
        inputManager.addProcessor(object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                UI.unfocusAll()
                return false
            }
        })
        inputManager.addProcessor(toolManager)
        inputManager.addProcessor(camController)
        toolManager.setDefaultTool()
    }

    private fun setupSceneWidget() {
        val context = projectManager.current()
        val scene = context.currScene
        val sg = scene.sceneGraph

        val config = ShaderUtils.buildPBRShaderConfig(projectManager.current().assetManager.maxNumBones)
        projectManager.modelBatch?.dispose()
        projectManager.modelBatch = ModelBatch(EditorShaderProvider(config), SceneRenderableSorter())

        val depthConfig = ShaderUtils.buildPBRShaderDepthConfig(projectManager.current().assetManager.maxNumBones)
        projectManager.setDepthBatch((ModelBatch(PBRDepthShaderProvider(depthConfig))))

        UI.sceneWidget.setCam(context.currScene.cam)
        UI.sceneWidget.setRenderer {
            val renderWireframe = projectManager.current().renderWireframe

            Gdx.gl.glLineWidth(globalPreferencesManager.getFloat(MundusPreferencesManager.GLOB_LINE_WIDTH_WIREFRAME, MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE))
            glProfiler.resume()
            sg.update()
            if (renderWireframe) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
            scene.render()
            if (renderWireframe) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
            glProfiler.pause()
            Gdx.gl.glLineWidth(MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE)

            if (debugRenderer.isEnabled) {
                debugRenderer.begin(scene.cam)
                debugRenderer.render(sg.gameObjects)
                debugRenderer.end()
            }

            val renderExtensions = pluginManager.getExtensions(RenderExtension::class.java)
            if (renderExtensions.isNotEmpty()) {
                renderExtensions.forEach {
                    scene.batch.begin(scene.cam)
                    try {
                        scene.batch.render(it.renderableProvider, scene.environment)
                    } catch (ex: Exception) {
                        Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during plugin rendering! $ex"))
                    } finally {
                        scene.batch.end()
                        Gdx.gl.glLineWidth(MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE)
                    }
                }
            }

            pluginManager.getExtensions(CustomShaderRenderExtension::class.java).forEach {
                try {
                    it.render(scene.cam)
                } catch (ex: Exception) {
                    Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during plugin custom rendering! $ex"))
                } finally {
                    Gdx.gl.glLineWidth(MundusPreferencesManager.GLOB_LINE_WIDTH_DEFAULT_VALUE)
                }
            }

            toolManager.render()
            gizmoManager.render()
            compass.render(projectManager.modelBatch, scene.environment)
        }

        gizmoManager.setCamera(context.currScene.cam)
        compass.setWorldCam(context.currScene.cam)
        camController.setCamera(context.currScene.cam)
        UI.sceneWidget.setCam(context.currScene.cam)
        context.currScene.viewport = UI.sceneWidget.viewport
    }

    override fun render() {
        GlUtils.clearScreen(Color.BLACK)
        if (projectManager.isLoading) {
            processLoading()
            return
        }

        UI.act()
        glProfiler.reset()
        camController.update()
        toolManager.act()
        UI.draw()
    }

    private fun processLoading() {
        projectManager.continueLoading()

        // Render a basic loading bar
        val progress = projectManager.loadingProject().assetManager.progress
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.projectionMatrix = guiCamera.combined
        shapeRenderer.color = Colors.GRAY_888
        shapeRenderer.rect(0f, guiCamera.viewportHeight * .1f, Gdx.graphics.width.toFloat(), guiCamera.viewportHeight * .02f)
        shapeRenderer.color = Colors.TEAL
        shapeRenderer.rect(0f, guiCamera.viewportHeight * .1f, progress * Gdx.graphics.width, guiCamera.viewportHeight * .02f)
        shapeRenderer.end()

        if (projectManager.isLoaded) {
            compass = Compass(projectManager.loadingProject().currScene.cam)
            // change project; this will fire a ProjectChangedEvent
            projectManager.changeProject(projectManager.loadingProject())

            pluginManager.getExtensions(TerrainSceneExtension::class.java).forEach {
                try {
                    it.sceneLoaded(projectManager.current().currScene.terrains)
                } catch (ex: Exception) {
                    Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during call plugin's sceneLoaded method! $ex"))
                }
            }

            UI.toggleLoadingScreen(false)
            UI.processVersionDialog()
        }

        UI.act()
        UI.draw()
    }

    override fun onProjectChanged(event: ProjectChangedEvent) {
        setupSceneWidget()
    }

    override fun onSceneChanged(event: SceneChangedEvent) {
        setupSceneWidget()
    }

    override fun onFullScreenEvent(event: FullScreenEvent) {
        if (event.isFullScreen) return
        // looks redundant but the purpose is to reset the FBO's to clear a render glitch on full screen exit
        projectManager.current().currScene.setWaterResolution(projectManager.current().currScene.settings.waterResolution)
    }

    private fun createDefaultProject(): ProjectContext? {
        if (registry.lastOpenedProject == null || !File(registry.lastOpenedProject.path).exists() || registry.projects.size == 0) {
            val name = "Default Project"
            var path = FileUtils.getUserDirectoryPath()
            path = FilenameUtils.concat(path, "MundusProjects")

            // If the default project already exists, import it instead of recreate it.
            // This can happen if the registry was deleted but the project folder was not.
            val defaultProjectPath = FilenameUtils.concat(path, name)
            val file = File(defaultProjectPath)
            if (file.exists()) {
                return try {
                    projectManager.importProject(defaultProjectPath)
                } catch (exception: ProjectAlreadyImportedException) {
                    val projectReference = ProjectRef()
                    projectReference.path = defaultProjectPath
                    projectReference.name = name
                    return projectManager.startAsyncProjectLoad(projectReference)
                }
            }

            return projectManager.createProject(name, path)
        }

        return null
    }

    private fun initPluginSystem() {
        pluginManager.loadPlugins()
        pluginManager.startPlugins()

        pluginManager.plugins.forEach { Mundus.postEvent(LogEvent("Plugin loaded: ${it.pluginId}")) }

        // Setup event handling in plugins
        val pluginEventManager =
            PluginEventManager { listener ->
                Mundus.registerEventListener(listener)
            }
        pluginManager.getExtensions(EventExtension::class.java).forEach {
            try {
                it.manageEvents(pluginEventManager)
            } catch (ex: Exception) {
                Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during manage plugin events! $ex"))
            }
        }

        // Setup asset manager for plugins
        pluginManager.getExtensions(AssetExtension::class.java).forEach {
            try {
                it.assetManager(AssetManagerImpl())
            } catch (ex: Exception) {
                Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during passing asset manager! $ex"))
            }
        }

        // Setup toaster manager for plugins
        pluginManager.getExtensions(ToasterExtension::class.java).forEach {
            try {
                it.toasterManager(ToasterManagerImpl())
            } catch (ex: Exception) {
                Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during passing toaster manager! $ex"))
            }
        }

        Mundus.postEvent(PluginsLoadedEvent())
    }

    override fun closeRequested(): Boolean {
        UI.showDialog(UI.exitDialog)
        return false
    }

    override fun resize(width: Int, height: Int) {
        UI.viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}

    override fun filesDropped(files: Array<out String>?) {
        Mundus.postEvent(FilesDroppedEvent(files))
    }

    override fun onGameObjectModified(event: GameObjectModifiedEvent) {
        if (event.gameObject == null) return
        projectManager.current().currScene.modelCacheManager.requestModelCacheRebuild()
    }

    override fun dispose() {
        debugRenderer.dispose()

        pluginManager.getExtensions(DisposeExtension::class.java).forEach {
            try {
                it.dispose()
            } catch (ex: Exception) {
                Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during dispose plugin! $ex"))
            }
        }

        Mundus.dispose()
    }

}
