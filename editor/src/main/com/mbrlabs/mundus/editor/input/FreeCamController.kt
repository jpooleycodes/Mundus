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

package com.mbrlabs.mundus.editor.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntIntMap
import com.mbrlabs.mundus.commons.scene3d.components.Component
import com.mbrlabs.mundus.commons.scene3d.components.TerrainComponent
import com.mbrlabs.mundus.commons.utils.Pools
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.keymap.KeymapKey
import com.mbrlabs.mundus.editor.core.keymap.KeymapManager
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.tools.picker.GameObjectPicker
import com.mbrlabs.mundus.pluginapi.TerrainHoverExtension
import org.pf4j.PluginManager

/**
 * @author Marcus Brummer
 * @version 24-11-2015
 */
class FreeCamController(private val projectManager: ProjectManager,
                        private val goPicker: GameObjectPicker,
                        private val pluginManager: PluginManager,
                        private val keymapManager: KeymapManager
) : InputAdapter() {

    val SPEED_01 = 10f
    val SPEED_1 = 150f
    val SPEED_10 = 500f

    private var camera: Camera? = null
    private val keys = IntIntMap()
    private val UP = Input.Keys.Q
    private val DOWN = Input.Keys.E
    private val SHIFT_LEFT = Input.Keys.SHIFT_LEFT
    private val SHIFT_RIGHT = Input.Keys.SHIFT_RIGHT
    private var velocity = SPEED_1
    private var zoomAmount = SPEED_01
    private var degreesPerPixel = 0.5f
    private val tmp = Vector3()
    private val tmp2 = Vector3()
    private val tmp3 = Vector3()
    private var pan = true

    fun setCamera(camera: Camera) {
        this.camera = camera
    }

    override fun keyDown(keycode: Int): Boolean {
        keys.put(keycode, keycode)
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        keys.remove(keycode, 0)
        return false
    }

    /**
     * Sets the velocity in units per second for moving forward, backward and
     * strafing left/right.
     *
     * @param velocity
     * *            the velocity in units per second
     */
    fun setVelocity(velocity: Float) {
        this.velocity = velocity
    }

    /**
     * Returns the velocity in units per second for moving forward, backward and
     * strafing left/right.
     *
     * @return the current velocity
     */
    fun getVelocity(): Float {
        return this.velocity
    }

    /**
     * Sets how many degrees to rotate per pixel the mouse moved.
     *
     * @param degreesPerPixel
     */
    fun setDegreesPerPixel(degreesPerPixel: Float) {
        this.degreesPerPixel = degreesPerPixel
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            var deltaX: Float = (-Gdx.input.deltaX).toFloat()
            var deltaY: Float = (-Gdx.input.deltaY).toFloat()

            // If pan is not enabled, rotate the camera
            if (!pan) {
                deltaX *= degreesPerPixel
                deltaY *= degreesPerPixel

                camera!!.direction.rotate(camera!!.up, deltaX)
                tmp.set(camera!!.direction).crs(camera!!.up).nor()

                // Resolves Gimbal Lock : https://github.com/libgdx/libgdx/issues/4023
                val oldPitchAxis = tmp.set(camera!!.direction).crs(camera!!.up).nor()
                val newDirection: Vector3 = tmp2.set(camera!!.direction).rotate(tmp, deltaY)
                val newPitchAxis: Vector3 = tmp3.set(tmp2).crs(camera!!.up)

                if (!newPitchAxis.hasOppositeDirection(oldPitchAxis)) {
                    camera!!.direction.set(newDirection)
                }

            } else {
                tmp.set(camera!!.direction).crs(camera!!.up).nor().scl(deltaX / velocity)
                camera!!.position.add(tmp)

                tmp.set(camera!!.up).nor().scl(-deltaY / velocity)
                camera!!.position.add(tmp)
            }
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        // If using combo key, do not consume event
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) return false

        if (camera == null) return false

        tmp.set(camera!!.direction).nor().scl(-amountY * zoomAmount)
        camera!!.position.add(tmp)
        return true
    }

    @JvmOverloads fun update(deltaTime: Float = Gdx.graphics.deltaTime) {
        if (keys.containsKey(keymapManager.getKey(KeymapKey.MOVE_FORWARD))) {
            tmp.set(camera!!.direction).nor().scl(deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (keys.containsKey(keymapManager.getKey(KeymapKey.MOVE_BACK))) {
            tmp.set(camera!!.direction).nor().scl(-deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (keys.containsKey(keymapManager.getKey(KeymapKey.STRAFE_LEFT))) {
            tmp.set(camera!!.direction).crs(camera!!.up).nor().scl(-deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (keys.containsKey(keymapManager.getKey(KeymapKey.STRAFE_RIGHT))) {
            tmp.set(camera!!.direction).crs(camera!!.up).nor().scl(deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (keys.containsKey(UP)) {
            tmp.set(camera!!.up).nor().scl(deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (keys.containsKey(DOWN)) {
            tmp.set(camera!!.up).nor().scl(-deltaTime * velocity)
            camera!!.position.add(tmp)
        }
        if (pan) {
            if (!keys.containsKey(SHIFT_LEFT) && !keys.containsKey(SHIFT_RIGHT)) {
                pan = false
            }
        }
        if (keys.containsKey(SHIFT_LEFT) || keys.containsKey(SHIFT_RIGHT)) {
            if (!pan) {
                pan = true
            }
        }
        camera!!.update(true)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (camera == null) {
            return false
        }

        val currentProject = projectManager.current()
        val currentScene = currentProject.currScene

        val terrainHoverExtensions = pluginManager.getExtensions(TerrainHoverExtension::class.java)

        if (terrainHoverExtensions.isNotEmpty()) {
            val ray = currentScene.viewport.getPickRay(screenX.toFloat(), screenY.toFloat())

            val go = goPicker.pick(currentScene, screenX, screenY)
            val terrainComponent: TerrainComponent? = go?.findComponentByType(Component.Type.TERRAIN)

            if (terrainComponent != null) {
                val result = terrainComponent.getRayIntersection(Pools.vector3Pool.obtain(), ray)

                terrainHoverExtensions.forEach {
                    try {
                        it.hover(terrainComponent, result)
                    } catch (ex: Exception) {
                        Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during plugin hoover event! $ex"))
                    }
                }

                Pools.vector3Pool.free(result)
            } else {
                terrainHoverExtensions.forEach {
                    try {
                        it.hover(null, null)
                    } catch (ex: Exception) {
                        Mundus.postEvent(LogEvent(LogType.ERROR, "Exception during plugin hoover event! $ex"))
                    }
                }
            }
        }

        return false
    }
}
