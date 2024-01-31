package com.mbrlabs.mundus.editor.ui.modules.dialogs

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneListener
import com.mbrlabs.mundus.commons.assets.TerrainAsset
import com.mbrlabs.mundus.commons.assets.TextureAsset
import com.mbrlabs.mundus.commons.scene3d.GameObject
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent
import com.mbrlabs.mundus.commons.scene3d.components.TerrainManagerComponent.ProceduralGeneration
import com.mbrlabs.mundus.commons.terrain.LodLevel
import com.mbrlabs.mundus.commons.terrain.Terrain
import com.mbrlabs.mundus.commons.terrain.TerrainLoader
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.AssetAlreadyExistsException
import com.mbrlabs.mundus.editor.assets.EditorAssetManager
import com.mbrlabs.mundus.editor.assets.MetaSaver
import com.mbrlabs.mundus.editor.core.io.IOManager
import com.mbrlabs.mundus.editor.core.io.IOManagerProvider
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.SceneGraphChangedEvent
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.terrain.HeightMapTerrainTab
import com.mbrlabs.mundus.editor.ui.modules.dialogs.terrain.ProceduralTerrainTab
import com.mbrlabs.mundus.editor.utils.LoDUtils
import com.mbrlabs.mundus.editor.utils.ThreadLocalPools
import com.mbrlabs.mundus.editor.utils.createTerrainGO
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs


/**
 * @author JamesTKhan
 * @version October 24, 2022
 */
class AddTerrainChunksDialog : BaseDialog("Add Terrain"), TabbedPaneListener {
    private var loadingDialog: VisDialog? = null
    private var parentGO: GameObject? = null

    val root = VisTable()

    private val tabbedPane = TabbedPane()
    private val tabContainer = VisTable()

    private val proceduralTerrainTab = ProceduralTerrainTab(this)
    private val heightmapTerrainTab = HeightMapTerrainTab(this)

    private var projectManager : ProjectManager
    private val ioManager: IOManager
    private var metaSaver : MetaSaver

    init {
        isResizable = true
        projectManager = Mundus.inject()
        ioManager = Mundus.inject<IOManagerProvider>().ioManager
        metaSaver = Mundus.inject()

        tabbedPane.addListener(this)
        tabbedPane.add(proceduralTerrainTab)
        tabbedPane.add(heightmapTerrainTab)

        root.add(tabbedPane.table).growX().row()
        root.add(tabContainer).expand().fill().row()
        tabbedPane.switchTab(0)

        root.padTop(6f).padRight(6f).padBottom(22f)
        add(root).expand().fill().row()
    }

    private var generatingTerrain = false
    private var generateLoD = false
    private var terraformingThreads: AtomicInteger = AtomicInteger(0)
    private var creationThreads: AtomicInteger = AtomicInteger(0)
    private var assetsToTerraform = ConcurrentHashMap<Vector2, TerrainComponent>()
    private var assetsToCreate = Array<IntArray>()
    private var terrainChunkMatrix: TerrainChunkMatrix? = null

    private var terrainName: String? = null
    private var executor: ExecutorService? = null
    private var terraformExecutor: ExecutorService? = null

    override fun draw(batch: Batch?, parentAlpha: Float) {
        val assetsToCreate = assetsToCreate.size > 0

        if (assetsToCreate) {
            runCreationThreads()
        } else if (assetsToTerraform.size > 0) {
            runTerraformingThreads()
        }

        if (terrainChunkMatrix?.isDone() == true) {
            setupNeighborTerrains()
        }

        if (generatingTerrain) {
            color.a = 0.4f
            val label = loadingDialog!!.contentTable.getChild(0) as Label
            label.setText("Creation threads: ${creationThreads.get()}\n"
                    + "Terraform threads: ${terraformingThreads.get()}\n"
                    + "Terraform Queue: ${assetsToTerraform.size}\n")

            loadingDialog!!.pack()
            if (!assetsToCreate && creationThreads.get() == 0 && assetsToTerraform.isEmpty()) {
                generatingTerrain = false
                color.a = 1.0f
                loadingDialog?.hide()

                Mundus.postEvent(SceneGraphChangedEvent())

                // If we don't shut it down, may keep app running in background
                executor?.shutdown()
                executor = null

                terraformExecutor?.shutdown()
                terraformExecutor = null
            }
        }

        super.draw(batch, parentAlpha)
    }

    fun createTerrainChunk(res: Int, width: Int, multipleTerrain: Boolean, xIteration: Int, yIteration: Int, name: String, splatMapResolution: Int, genLoD: Boolean) {
        terrainName = name
        executor = Executors.newFixedThreadPool(4)
        terraformExecutor = Executors.newSingleThreadExecutor()
        generateLoD = genLoD

        val context = projectManager.current()
        val sceneGraph = context.currScene.sceneGraph
        val goID = projectManager.current().obtainID()

        // Save context here so that the ID above is persisted in .pro file
        ioManager.saveProjectContext(projectManager.current())

        if (multipleTerrain) {
            parentGO = GameObject(context.currScene.sceneGraph, "$terrainName Manager", goID)
            parentGO!!.addComponent(createTerrainManagerComponent(parentGO!!))
        } else {
            parentGO = null
        }

        val layerName = "${terrainName}.layer"
        if (projectManager.current().assetManager.assetExists(layerName)) {
            Dialogs.showErrorDialog(UI, "Terrain Layer with name $terrainName already exists. Pick a different name or\nremove existing asset.")
            return
        }

        // Before we start, make sure the terrain name does not already exist
        val assetExists: Boolean
        if (multipleTerrain) {
            assetExists = checkMultipleTerrainAssetsExist(res, width, splatMapResolution, xIteration, yIteration)
        } else {
            assetExists = checkSingleTerrainAssetExists(res, width, splatMapResolution)
        }

        if (assetExists) return

        terrainChunkMatrix = TerrainChunkMatrix(xIteration, yIteration)

        generatingTerrain = true
        loadingDialog = Dialogs.showOKDialog(UI, "Generating Terrain", "Generating ")
        val button = loadingDialog!!.buttonsTable.getChild(0) as VisTextButton
        button.isDisabled = true

        if (multipleTerrain) {
            sceneGraph.addGameObject(parentGO)
            Mundus.postEvent(SceneGraphChangedEvent())
        }
    }

    private fun checkMultipleTerrainAssetsExist(res: Int, width: Int, splatMapResolution: Int, xIteration: Int, yIteration: Int): Boolean {
        var assetExists = false

        for (i in 0 until xIteration) {
            for (j in 0 until yIteration) {

                // Before we start, make sure the terrain name does not already exist
                val terraFileName = "${terrainName}$i-$j.terra.meta"
                if (projectManager.current().assetManager.assetExists(terraFileName)) {
                    assetExists = true
                    assetsToCreate.clear()
                    Dialogs.showErrorDialog(UI, "Terrain with name $terrainName already exists. Pick a different name or\nremove existing asset.")
                    break
                }

                assetsToCreate.add(intArrayOf(res, width, splatMapResolution, i, j))
            }
            if (assetExists) break
        }

        return assetExists
    }

    private fun checkSingleTerrainAssetExists(res: Int, width: Int, splatMapResolution: Int): Boolean {
        var assetExists = false
        val terraFileName = "${terrainName}.terra.meta"

        if (projectManager.current().assetManager.assetExists(terraFileName)) {
            assetExists = true
            assetsToCreate.clear()
            Dialogs.showErrorDialog(UI, "Terrain with name $terrainName already exists. Pick a different name or\nremove existing asset.")
        } else {
            assetsToCreate.add(intArrayOf(res, width, splatMapResolution, 0, 0))
        }

        return assetExists
    }

    private fun createTerrainManagerComponent(parentGO: GameObject): TerrainManagerComponent {
        var proceduralGeneration: ProceduralGeneration? = null
        if (tabbedPane.activeTab is ProceduralTerrainTab) {
            val proceduralTerrainTab = tabbedPane.activeTab as ProceduralTerrainTab

            proceduralGeneration = ProceduralGeneration()
            proceduralGeneration.minHeight = proceduralTerrainTab.getMinHeightValue()
            proceduralGeneration.maxHeight = proceduralTerrainTab.getMaxHeightValue()

            proceduralTerrainTab.uploadNoiseModifiers(proceduralGeneration.noiseModifiers)
        }


        return TerrainManagerComponent(parentGO, proceduralGeneration)
    }

    private fun runCreationThreads() {
        val context = projectManager.current()
        val sceneGraph = context.currScene.sceneGraph
        // Start a new thread pool for creating the assets

        // Create a new layer asset to assign to all terrain chunks
        val terrainLayerAsset = projectManager.current().assetManager.createTerrainLayerAsset(terrainName!!)
        // set base texture
        val chessboard =
            projectManager.current().assetManager.findAssetByID(EditorAssetManager.STANDARD_ASSET_TEXTURE_CHESSBOARD)
        if (chessboard != null) {
            terrainLayerAsset.splatBase = chessboard as TextureAsset
            terrainLayerAsset.resolveDependencies(projectManager.current().assetManager.assetMap)
            terrainLayerAsset.applyDependencies()
        }
        projectManager.current().assetManager.addAsset(terrainLayerAsset)
        projectManager.current().assetManager.addModifiedAsset(terrainLayerAsset)
        projectManager.current().assetManager.saveAsset(terrainLayerAsset)

        for (arr in assetsToCreate) {
            val goID = projectManager.current().obtainID()

            // Submit thread job
            this.executor?.submit {
                creationThreads.addAndGet(1)
                val res = arr[0]
                val width = arr[1]
                val splatMapResolution = arr[2]
                val i = arr[3]
                val j = arr[4]

                val asset: TerrainAsset
                val loader: TerrainLoader
                try {
                    asset = createTerrainAsset(res, width, splatMapResolution, i, j)
                    asset.meta.terrain.terrainLayerAssetId = terrainLayerAsset.id
                    asset.lodLevels = if (generateLoD) arrayOf<LodLevel>() else null
                    loader = asset.startAsyncLoad()
                } catch (ex: AssetAlreadyExistsException) {
                    Dialogs.showErrorDialog(stage, "An asset with that name already exists.")
                    executor?.shutdownNow()
                    creationThreads.decrementAndGet()
                    //break
                    return@submit
                }

                // post a Runnable to the rendering thread that processes the result
                Gdx.app.postRunnable {
                    creationThreads.decrementAndGet()
                    asset.finishSyncLoad(loader)
                    asset.resolveDependencies(context.assetManager.assetMap)
                    metaSaver.save(asset.meta)

                    projectManager.current().assetManager.addAsset(asset)

                    val goName = if (isMultipleTerrain()) "${terrainName}$i-$j" else "$terrainName"
                    val terrainGO = createTerrainGO(sceneGraph, goID, goName, asset)

                    terrainGO.setLocalPosition((i * width).toFloat(), 0f, (j * width).toFloat())
                    val component = terrainGO.findComponentByType(Component.Type.TERRAIN) as TerrainComponent

                    context.currScene.terrains.add(component)
                    projectManager.current().assetManager.addNewAsset(asset)
                    Mundus.postEvent(AssetImportEvent(asset))

                    if (isMultipleTerrain()) {
                        parentGO!!.addChild(terrainGO)
                    } else {
                        sceneGraph.addGameObject(terrainGO)
                        Mundus.postEvent(SceneGraphChangedEvent())
                    }

                    // Now Queue it up for terraforming
                    assetsToTerraform[Vector2(i.toFloat(), j.toFloat())] = component

                    // Add generated terrain chunk to matrix
                    terrainChunkMatrix!!.addTerrain(i, j, component)
                }
            }
        }

        // After the jobs are submitted, clear the queue and shutdown executor (shuts down AFTER jobs complete)
        assetsToCreate.clear()
    }

    private fun runTerraformingThreads() {
        val grid = assetsToTerraform.entries.first().key
        val component = assetsToTerraform.entries.first().value
        val asset = component.terrainAsset
        assetsToTerraform.remove(grid)

        terraformExecutor?.submit {
            terraformingThreads.addAndGet(1)

            var minHeight = 0f
            var maxHeight = 0f
            if (tabbedPane.activeTab is ProceduralTerrainTab) {
                minHeight = proceduralTerrainTab.getMinHeightValue()
                maxHeight = proceduralTerrainTab.getMaxHeightValue()
                proceduralTerrainTab.terraform(grid.x.toInt(), grid.y.toInt(), component)
            } else if (tabbedPane.activeTab is HeightMapTerrainTab) {
                minHeight = heightmapTerrainTab.getMinHeightValue()
                maxHeight = heightmapTerrainTab.getMaxHeightValue()
                heightmapTerrainTab.terraform(grid.x.toInt(), grid.y.toInt(), component)
            }

            asset.updateTerrainMaterial()
            asset.terrain.update(ThreadLocalPools.vector3ThreadPool.get())

            // Generate simplified results for LoD
            val results = LoDUtils.buildTerrainLod(component, Terrain.LOD_SIMPLIFICATION_FACTORS, abs(maxHeight - minHeight))

            // post a Runnable to the rendering thread that processes the result
            Gdx.app.postRunnable {
                component.updateDimensions()

                // Convert to LodLevels with actual meshes
                asset.lodLevels = LoDUtils.convertToLodLevels(asset.terrain.model, results)

                terraformingThreads.decrementAndGet()
            }
        }
    }

    private fun createTerrainAsset(resolution: Int, width: Int, splatMapResolution: Int, i: Int, j: Int): TerrainAsset {
        val terrainAssetName = if (isMultipleTerrain()) "${terrainName}$i-$j" else terrainName!!

        // create asset
        val asset: TerrainAsset = projectManager.current().assetManager.createTerraAssetAsync(
                terrainAssetName, resolution, width, splatMapResolution)

        return asset
    }

    /**
     * Setups neighbor terrains for each terrain.
     */
    private fun setupNeighborTerrains() {
        val terrainComponents = terrainChunkMatrix!!.terrainComponents

        // Bottom right corner is 0,0. Top neighbor is +y, right neighbor is +x
        for (x in 0 until terrainComponents.size) {
            for (y in 0 until terrainComponents[x].size) {
                if (y+1 < terrainComponents[x].size) {
                    terrainComponents[x][y]!!.topNeighbor = terrainComponents[x][y+1]
                }
                if (x-1 >= 0) {
                    terrainComponents[x][y]!!.rightNeighbor = terrainComponents[x-1][y]
                }
                if (y-1 >= 0) {
                    terrainComponents[x][y]!!.bottomNeighbor = terrainComponents[x][y-1]
                }
                if (x+1 < terrainComponents.size) {
                    terrainComponents[x][y]!!.leftNeighbor = terrainComponents[x+1][y]
                }
            }
        }

        terrainChunkMatrix = null
    }

    private fun isMultipleTerrain(): Boolean = parentGO != null

    inner class TerrainChunkMatrix(x: Int, y: Int) {

        private var remainingTerrainComponents = x * y
        val terrainComponents = Array(x) { Array<TerrainComponent?>(y) {null} }

        fun addTerrain(x: Int, y: Int, terrainComponent: TerrainComponent) {
            --remainingTerrainComponents

            terrainComponents[x][y] = terrainComponent
        }

        fun isDone(): Boolean = remainingTerrainComponents == 0
    }

    override fun switchedTab(tab: Tab?) {
        tabContainer.clearChildren()
        tabContainer.add(tab?.contentTable).expand().fill()
    }

    override fun removedTab(tab: Tab?) {

    }

    override fun removedAllTabs() {

    }

}
