package com.mbrlabs.mundus.editor.ui.widgets

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.events.UpdateNoiseTextureEvent
import com.mbrlabs.mundus.editor.terrain.FastNoiseGenerator
import com.mbrlabs.mundus.editor.terrain.Terraformer

/**
 * @author JamesTKhan
 * @version October 24, 2022
 */
class NoiseGeneratorWidget() : BaseWidget(), UpdateNoiseTextureEvent.UpdateNoiseTextureListener  {
    var generator: FastNoiseGenerator
    var renderWidget: RenderWidget

    private var listener: ChangeListener
    private var noiseTextureWidth = 180

    private var sb = SpriteBatch()
    private var noiseTexture: Texture? = null

    init {
        Mundus.registerEventListener(this)

        val cam = OrthographicCamera(300f, 300f)
        cam.setToOrtho(true, 300f, 300f)
        renderWidget = RenderWidget(cam)

        generator = Terraformer.fastNoise(null)

        listener = object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                updateNoiseTexture()
            }
        }

        setupUI()
        updateNoiseTexture()

        renderWidget.setRenderer { camera ->
            if (noiseTexture != null) {
                sb.begin()
                sb.projectionMatrix = camera.combined
                sb.draw(noiseTexture, 0f, 0f, 300f, 300f)
                sb.end()
            }
        }
    }

    private fun setupUI() {
        defaults().padTop(5f).padBottom(5f).left()
        add(renderWidget).colspan(2).width(300f).height(300f).expand().fill().row()
    }

    private fun updateNoiseTexture() {
        if (noiseTexture != null) noiseTexture!!.dispose()
        noiseTexture = Texture(generator.generateNoise(noiseTextureWidth, noiseTextureWidth))
    }

    fun setNoiseTextureWidth(width: Int) {
        noiseTextureWidth = width
        updateNoiseTexture()
    }

    override fun onTextureUpdate(event: UpdateNoiseTextureEvent) {
        updateNoiseTexture()
    }
}