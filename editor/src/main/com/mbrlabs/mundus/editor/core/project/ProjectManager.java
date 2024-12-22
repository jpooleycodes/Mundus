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

package com.mbrlabs.mundus.editor.core.project;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.Asset;
import com.mbrlabs.mundus.commons.assets.AssetNotFoundException;
import com.mbrlabs.mundus.commons.assets.ModelAsset;
import com.mbrlabs.mundus.commons.assets.SkyboxAsset;
import com.mbrlabs.mundus.commons.assets.TextureAsset;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.commons.dto.SceneDTO;
import com.mbrlabs.mundus.commons.mapper.CustomComponentConverter;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.commons.scene3d.SceneGraph;
import com.mbrlabs.mundus.commons.scene3d.components.Component;
import com.mbrlabs.mundus.commons.scene3d.components.ModelComponent;
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent;
import com.mbrlabs.mundus.commons.scene3d.components.WaterComponent;
import com.mbrlabs.mundus.editor.Main;
import com.mbrlabs.mundus.editor.Mundus;
import com.mbrlabs.mundus.editor.assets.EditorAssetManager;
import com.mbrlabs.mundus.editor.core.EditorScene;
import com.mbrlabs.mundus.editor.core.converter.SceneConverter;
import com.mbrlabs.mundus.editor.core.io.IOManager;
import com.mbrlabs.mundus.editor.core.registry.ProjectRef;
import com.mbrlabs.mundus.editor.core.registry.Registry;
import com.mbrlabs.mundus.editor.core.scene.SceneManager;
import com.mbrlabs.mundus.editor.events.LogEvent;
import com.mbrlabs.mundus.editor.events.LogType;
import com.mbrlabs.mundus.editor.scene3d.components.PickableComponent;
import com.mbrlabs.mundus.editor.shader.Shaders;
import com.mbrlabs.mundus.editor.ui.UI;
import com.mbrlabs.mundus.editor.utils.Log;
import com.mbrlabs.mundus.editor.utils.PluginUtils;
import com.mbrlabs.mundus.editor.utils.SkyboxBuilder;
import com.mbrlabs.mundus.editorcommons.events.ProjectChangedEvent;
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent;
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException;
import org.pf4j.PluginManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Manages Mundus projects and scenes.
 *
 * @author Marcus Brummer
 * @version 25-11-2015
 */
public class ProjectManager implements Disposable {

    private static final String TAG = ProjectManager.class.getSimpleName();

    private static final String DEFAULT_SCENE_NAME = "Main Scene";
    public static final String PROJECT_ASSETS_DIR = "assets/";
    public static final String PROJECT_SCENES_DIR = "scenes/";
    public static final String PROJECT_SCENE_EXTENSION = "mundus";
    public static final String PROJECT_EXTENSION = "pro";

    private ProjectContext currentProject;
    private ProjectContext loadingProject;
    private Registry registry;
    private IOManager ioManager;
    private ModelBatch modelBatch;
    private ModelBatch depthBatch;
    private PluginManager pluginManager;

    public ProjectManager(IOManager ioManager, Registry registry, ModelBatch modelBatch, PluginManager pluginManager) {
        this.registry = registry;
        this.ioManager = ioManager;
        this.modelBatch = modelBatch;
        this.pluginManager = pluginManager;
        currentProject = new ProjectContext(-1);
    }

    /**
     * Returns current project.
     *
     * @return current project
     */
    public ProjectContext current() {
        return currentProject;
    }

    public ProjectContext loadingProject() {
        return loadingProject;
    }

    public boolean isLoading() {
        if (loadingProject == null) {
            return false;
        }

        return !loadingProject.loaded;
    }

    public boolean isLoaded() {
        if (loadingProject == null) {
            return false;
        }

        return loadingProject.loaded;
    }

    /**
     * Saves the active project
     */
    public void saveCurrentProject() {
        saveProject(currentProject);
    }

    public String assetFolder() {
        return currentProject.path + "/" + PROJECT_ASSETS_DIR;
    }

    /**
     * Creates & saves a new project.
     *
     * Creates a new project. However, it does not switch the current project.
     *
     * @param name
     *            project name
     * @param folder
     *            absolute path to project folder
     * @return new project context
     */
    public ProjectContext createProject(String name, String folder) {
        ProjectRef ref = registry.createProjectRef(name, folder);
        String path = ref.getPath();
        new File(path).mkdirs();
        new File(path, PROJECT_ASSETS_DIR).mkdirs();
        new File(path, PROJECT_SCENES_DIR).mkdirs();

        // create currentProject current
        ProjectContext newProjectContext = new ProjectContext(-1);
        newProjectContext.path = path;
        newProjectContext.name = ref.getName();
        newProjectContext.activeSceneName = DEFAULT_SCENE_NAME;
        newProjectContext.assetManager = new EditorAssetManager(
                new FileHandle(path + "/" + ProjectManager.PROJECT_ASSETS_DIR));

        // create default scene & save .mundus
        EditorScene scene = new EditorScene();
        scene.setName(DEFAULT_SCENE_NAME);
        scene.skybox = SkyboxBuilder.createDefaultSkybox(Shaders.INSTANCE.getSkyboxShader());
        scene.skyboxAssetId = getDefaultSkyboxAsset(newProjectContext, true).getID();
        scene.setId(newProjectContext.obtainID());
        SceneManager.saveScene(newProjectContext, scene, pluginManager);
        scene.sceneGraph.scene.batch = modelBatch;

        // save .pro file
        newProjectContext.scenes.add(scene.getName());
        newProjectContext.currScene = scene;
        saveProject(newProjectContext);

        // create standard assets
        newProjectContext.assetManager.createStandardAssets();

        // Generate assets.txt file
        newProjectContext.assetManager.createAssetsTextFile();

        return newProjectContext;
    }

    /**
     * Gets the default skybox, if it can be found.
     *
     * @param projectContext the project context to use
     * @param createIfMissing if true, creates default skybox if it's missing
     * @return skybox asset
     */
    public SkyboxAsset getDefaultSkyboxAsset(ProjectContext projectContext, boolean createIfMissing) {

        // See if the default skybox already exists
        SkyboxAsset defaultSkybox = (SkyboxAsset) projectContext.assetManager.findAssetByFileName("default.sky");

        if (defaultSkybox == null && createIfMissing) {
            FileHandle texture = Gdx.files.internal("textures/skybox/default/skybox_default.png");
            TextureAsset textureAsset = projectContext.assetManager.getOrCreateTextureAsset(texture);

            String id = textureAsset.getID();
            // Create it if it does not exist
            try {
                defaultSkybox = projectContext.assetManager.createSkyBoxAsset("default", id, id, id, id, id, id);
                defaultSkybox.positiveX = textureAsset;
                defaultSkybox.negativeX = textureAsset;
                defaultSkybox.positiveY = textureAsset;
                defaultSkybox.negativeY = textureAsset;
                defaultSkybox.positiveZ = textureAsset;
                defaultSkybox.negativeZ = textureAsset;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AssetAlreadyExistsException e) {
                e.printStackTrace();
            }

            if (defaultSkybox == null)
                throw new GdxRuntimeException("Unable to get or create default skybox.");
        }

        return defaultSkybox;
    }

    /**
     * Imports (opens) a mundus project, that is not in the registry.
     *
     * @param absolutePath
     *            path to project
     * @return project context of imported project
     * @throws ProjectAlreadyImportedException
     *             if project exists already in registry
     * @throws ProjectOpenException
     *             project could not be opened
     */
    public ProjectContext importProject(String absolutePath) throws ProjectAlreadyImportedException, ProjectOpenException {
        // check if already imported
        for (ProjectRef ref : registry.getProjects()) {
            if (ref.getPath().equals(absolutePath)) {
                throw new ProjectAlreadyImportedException("Project " + absolutePath + " is already imported");
            }
        }

        ProjectRef ref = new ProjectRef();
        ref.setPath(absolutePath);

        try {
            ProjectContext context = ioManager.loadProjectContext(ref);
            context.path = absolutePath;
            UI.INSTANCE.toggleLoadingScreen(true, context.name);
            ref.setName(context.name);
            registry.getProjects().add(ref);

            // Set this import project as last opened to prevent NPE only
            // if no project was opened before
            if (registry.getLastProject() == null){
                registry.setLastProject(ref);
            }

            ioManager.saveRegistry(registry);
            startAsyncProjectLoad(absolutePath, context);
            return context;
        } catch (Exception e) {
            throw new ProjectOpenException(e.getMessage());
        }
    }

    private void loadAssets(String path, ProjectContext context) throws MetaFileParseException {
        context.assetManager = new EditorAssetManager(
                new FileHandle(path + "/" + ProjectManager.PROJECT_ASSETS_DIR));

        context.assetManager.queueAssetsForLoading(false);
    }

    /**
     * Completely saves a project & all scenes.
     *
     * @param projectContext
     *            project context
     */
    public void saveProject(ProjectContext projectContext) {
        // save modified assets
        EditorAssetManager assetManager = projectContext.assetManager;
        for (Asset asset : assetManager.getModifiedAssets()) {
            try {
                Log.debug(TAG, "Saving modified asset: {}", asset);
                assetManager.saveAsset(asset);
                System.out.println(asset.getName());
            } catch (IOException e) {
                Log.exception(TAG, e);
            }
        }
        assetManager.getModifiedAssets().clear();

        for (Asset asset : assetManager.getNewAssets()) {
            try {
                Log.debug(TAG, "Saving new asset: {}", asset);
                assetManager.saveAsset(asset);
                System.out.println(asset.getName());
            } catch (IOException e) {
                Log.exception(TAG, e);
            }
        }
        assetManager.getNewAssets().clear();

        // Generate assets.txt file
        assetManager.createAssetsTextFile();

        // save current in .pro file
        ioManager.saveProjectContext(projectContext);
        // save scene in .mundus file
        SceneManager.saveScene(projectContext, projectContext.currScene, pluginManager);

        Log.debug(TAG, "Saving currentProject {}", projectContext.name + " [" + projectContext.path + "]");
        Mundus.INSTANCE.postEvent(new LogEvent("Saving currentProject " + projectContext.name + " [" + projectContext.path + "]"));
    }

    /**
     * Loads the project that was open when the user quit the program.
     *
     * Does not open open the project.
     *
     * @return project context of last project
     */
    public ProjectContext loadLastProjectAsync() {
        ProjectRef lastOpenedProject = registry.getLastOpenedProject();
        if (lastOpenedProject != null) {

            // Check if file exists first
            File file = new File(lastOpenedProject.getPath());
            if (!file.exists()) {
                Log.error(TAG, "Last opened project does not exist: " + lastOpenedProject.getPath());
                return null;
            }

            try {
                return startAsyncProjectLoad(lastOpenedProject);
            } catch (FileNotFoundException fnf) {
                Log.error(TAG, fnf.getMessage());
                fnf.printStackTrace();
            } catch (MetaFileParseException anf) {
                Log.error(TAG, anf.getMessage());
            }
            return null;
        }
        return null;
    }

    /**
     * Starts loading the project context for a project.
     *
     * This does not open to that project, it only starts the async load process.
     * {@link #continueLoading()} must be called each frame while loading to continue the loading process.
     *
     * @param ref
     *            project reference to the project
     * @return initialized but unloaded project context
     * @throws FileNotFoundException
     *             if project can't be found
     */
    public ProjectContext startAsyncProjectLoad(ProjectRef ref) throws FileNotFoundException, MetaFileParseException {
        ProjectContext context = ioManager.loadProjectContext(ref);
        context.path = ref.getPath();
        context.name = ref.getName();

        return startAsyncProjectLoad(ref.getPath(), context);
    }

    public ProjectContext startAsyncProjectLoad(String path, ProjectContext context) throws MetaFileParseException {
        Log.debug(TAG, "Asynchronous project loading started...");
        loadingProject = context;

        // Queues up assets for loading
        loadAssets(path, loadingProject);

        return loadingProject;
    }

    public ProjectContext continueLoading() throws FileNotFoundException, MetaFileParseException, AssetNotFoundException {
        try {
            boolean complete = loadingProject.assetManager.continueLoading();

            if (!complete) {
                return loadingProject;
            }

            return finalizeLoading();
        } catch (GdxRuntimeException exception) {
            UI.INSTANCE.getToaster().error(exception.getCause().getMessage());
            return finalizeLoading();
        }
    }

    private ProjectContext finalizeLoading() throws FileNotFoundException {
        Log.debug(TAG, "Asynchronous project loading complete");

        loadingProject.currScene = loadScene(loadingProject, loadingProject.activeSceneName);
        loadingProject.loaded = true;
        return loadingProject;
    }

    /**
     * Opens a project.
     *
     * Opens a project. If a project is already open it will be disposed.
     *
     * @param context
     *            project context to open
     */
    public void changeProject(ProjectContext context) {
        if (currentProject != null) {
            if (currentProject.assetManager != null) {
                currentProject.assetManager.deleteNewUnsavedAssets();
            }
            currentProject.dispose();
        }

        // Null it out now that loading is complete
        if (context == loadingProject) {
            loadingProject = null;
        }

        currentProject = context;
        currentProject.initPreferences();
        // currentProject.copyFrom(context);
        registry.setLastProject(new ProjectRef());
        registry.getLastOpenedProject().setName(context.name);
        registry.getLastOpenedProject().setPath(context.path);

        ioManager.saveRegistry(registry);

        Gdx.graphics.setTitle(constructWindowTitle());
        Mundus.INSTANCE.postEvent(new ProjectChangedEvent(context.currScene.sceneGraph));
        currentProject.currScene.onLoaded();
    }

    /**
     * Creates a new scene for the given project.
     *
     * @param project
     *            project
     * @param name
     *            scene name
     * @return newly created scene
     */
    public Scene createScene(ProjectContext project, String name) {
        Scene scene = new Scene();
        long id = project.obtainID();
        scene.setId(id);
        scene.setName(name);
        scene.skyboxAssetId = getDefaultSkyboxAsset(project, false).getID();
        if (scene.skyboxAssetId != null)
            scene.skybox = SkyboxBuilder.createDefaultSkybox(Shaders.INSTANCE.getSkyboxShader());
        project.scenes.add(scene.getName());
        SceneManager.saveScene(project, scene, pluginManager);

        return scene;
    }

    /**
     * Loads a scene.
     *
     * This does not open the scene.
     *
     * @param context
     *            project context of the scene
     * @param sceneName
     *            name of the scene
     * @return loaded scene
     * @throws FileNotFoundException
     *             if scene file not found
     */
    public EditorScene loadScene(ProjectContext context, String sceneName) throws FileNotFoundException {
        SceneDTO sceneDTO = SceneManager.loadScene(context, sceneName);

        final Array<CustomComponentConverter> customComponentConverters = PluginUtils.INSTANCE.getCustomComponentConverters(pluginManager);

        EditorScene scene = SceneConverter.convert(sceneDTO, context.assetManager.getAssetMap(), customComponentConverters);

        // load skybox
        if (scene.skyboxAssetId != null && context.assetManager.getAssetMap().containsKey(scene.skyboxAssetId)) {
            SkyboxAsset asset = (SkyboxAsset) context.assetManager.getAssetMap().get(scene.skyboxAssetId);
            scene.setSkybox(asset, Shaders.INSTANCE.getSkyboxShader());
        }

        scene.batch = modelBatch;

        scene.setDepthShader(Shaders.INSTANCE.getDepthShader());

        SceneGraph sceneGraph = scene.sceneGraph;
        for (GameObject go : sceneGraph.getGameObjects()) {
            initGameObject(context, go);
        }

        // create TerrainGroup for active scene
        Array<TerrainComponent> terrainComponents = new Array<>();
        for (GameObject go : sceneGraph.getGameObjects()) {
            go.findComponentsByType(terrainComponents, Component.Type.TERRAIN, true);
        }
        for (TerrainComponent c : terrainComponents) {
            scene.terrains.add(c);
        }

        return scene;
    }

    /**
     * Loads and opens scene
     *
     * @param projectContext
     *            project context of scene
     * @param sceneName
     *            scene name
     */
    public void changeScene(ProjectContext projectContext, String sceneName) {
        try {
            EditorScene newScene = loadScene(projectContext, sceneName);
            projectContext.currScene.dispose();
            projectContext.currScene = newScene;

            Gdx.graphics.setTitle(constructWindowTitle());
            Mundus.INSTANCE.postEvent(new SceneChangedEvent(projectContext.currScene.sceneGraph));
            projectContext.currScene.onLoaded();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.error(TAG, e.getMessage());
        }
    }

    /**
     * Renames scene.
     *
     * @param project The project context
     * @param oldSceneName The old name of scene
     * @param newSceneName The new name of scene
     */
    public void renameScene(final ProjectContext project, final String oldSceneName, final String newSceneName) {
        // Rename scene name in scene list
        for(int i = 0; i < project.scenes.size; ++i) {
            if (project.scenes.get(i).equals(oldSceneName)) {
                project.scenes.removeIndex(i);
                project.scenes.insert(i, newSceneName);
            }
        }

        // If it is the current scene then rename it in project context too
        if (project.currScene.getName().equals(oldSceneName)) {
            project.currScene.setName(newSceneName);
        }

        // Rename scene file on filesystem
        SceneManager.renameScene(project, oldSceneName, newSceneName);
    }

    /**
     * Deletes scene
     *
     * @param project The project context
     * @param sceneName The screen name
     */
    public void deleteScene(final ProjectContext project, final String sceneName) {
        project.scenes.removeValue(sceneName, false);
        SceneManager.deleteScene(project, sceneName);
    }

    private void initGameObject(ProjectContext context, GameObject root) {
        initComponents(context, root);
        if (root.getChildren() != null) {
            for (GameObject c : root.getChildren()) {
                initGameObject(context, c);
            }
        }
    }

    private void initComponents(ProjectContext context, GameObject go) {
        Array<ModelAsset> models = context.assetManager.getModelAssets();
        Array.ArrayIterator<Component> iterator = go.getComponents().iterator();
        while(iterator.hasNext()) {
            Component c = iterator.next();
            if (c == null) {
                // To prevent crashing, log a warning statement and remove the corrupted component
                iterator.remove();
                Log.warn(TAG, "A component for {} was null on load, this may be caused by deleting an asset that is still in a scene.", go);
                Mundus.INSTANCE.postEvent(new LogEvent(LogType.ERROR, "A component for "+ go.name +"  was null on load, this may be caused by deleting an asset that is still in a scene."));
                go.name = go.name.concat(" [COMPONENT ERROR]");
                continue;
            }
            // Model component
            if (c.getType() == Component.Type.MODEL) {
                ModelComponent modelComponent = (ModelComponent) c;
                ModelAsset model = findModelById(models, modelComponent.getModelAsset().getID());
                if (model != null) {
                    modelComponent.setModel(model, false);
                } else {
                    Log.fatal(TAG, "model for modelInstance not found: {}", modelComponent.getModelAsset().getID());
                }
            } else if (c.getType() == Component.Type.WATER) {
                ((WaterComponent) c).getWaterAsset().water.setTransform(go.getTransform());
            }

            // encode id for picking
            if (c instanceof PickableComponent) {
                ((PickableComponent) c).encodeRaypickColorId();
            }
        }
    }

    private ModelAsset findModelById(Array<ModelAsset> models, String id) {
        for (ModelAsset m : models) {
            if (m.getID().equals(id)) {
                return m;
            }
        }

        return null;
    }

    private String constructWindowTitle() {
        return currentProject.name + " - " + currentProject.currScene.getName() + " [" + currentProject.path + "]"
                + " - " + Main.TITLE;
    }

    public ModelBatch getModelBatch() {
        return modelBatch;
    }

    /**
     * Disposes current model batch, assigns the new batch and updates the batch
     * on the current scene.
     *
     * @param modelBatch new ModelBatch instance to use
     */
    public void setModelBatch(ModelBatch modelBatch) {
        if (this.modelBatch != null) {
            this.modelBatch.dispose();
        }

        this.modelBatch = modelBatch;

        if (currentProject != null && currentProject.currScene != null) {
            currentProject.currScene.batch = this.modelBatch;
        }
    }

    /**
     * Disposes current depth model batch, assigns the new batch and updates the depth batch
     * on the current scene.
     *
     * @param depthBatch new ModelBatch instance to use
     */
    public void setDepthBatch(ModelBatch depthBatch) {
        if (this.depthBatch != null) {
            this.depthBatch.dispose();
        }

        this.depthBatch = depthBatch;

        if (currentProject != null && currentProject.currScene != null) {
            currentProject.currScene.depthBatch = this.depthBatch;
        }
    }

    @Override
    public void dispose() {
        currentProject.dispose();
    }
}