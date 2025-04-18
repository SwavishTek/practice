package com.robin.interview

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.robin.interview.nodes.DragTransformableNode
import kotlinx.android.synthetic.main.activity_scene_view.*
import java.util.concurrent.CompletionException

class SceneViewActivity : AppCompatActivity() {

    var localModel = "interview.glb"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene_view)
        renderLocalObject()
    }

    private fun renderLocalObject() {
        skuProgressBar.setVisibility(View.VISIBLE)

        val scale = 0.05f  // Start with a smaller scale
        val modelUri = Uri.parse("file:///android_asset/$localModel")

        ModelRenderable.builder()
            .setSource(
                this,
                RenderableSource.builder()
                    .setSource(this, modelUri, RenderableSource.SourceType.GLB)
                    .setScale(scale)
                    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                    .build()
            )
            .setRegistryId(localModel)
            .build()
            .thenAccept { modelRenderable: ModelRenderable ->
                skuProgressBar.setVisibility(View.GONE)
                Log.d("Model Debug", "Model scale: $scale")

                // Apply orange color to the model before adding it to the scene
                addOrangeMaterialToModel(modelRenderable)
            }
            .exceptionally { throwable: Throwable? ->
                var message: String? = if (throwable is CompletionException) {
                    skuProgressBar.setVisibility(View.GONE)
                    "Internet is not working"
                } else {
                    skuProgressBar.setVisibility(View.GONE)
                    "Can't load Model"
                }

                val mainHandler = Handler(Looper.getMainLooper())
                val finalMessage: String = message ?: "Error loading model"
                val myRunnable = Runnable {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(finalMessage)
                        .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                            renderLocalObject()
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }

    private fun addOrangeMaterialToModel(modelRenderable: ModelRenderable) {
        MaterialFactory.makeOpaqueWithColor(this, Color(1.0f, 0.647f, 0.0f))  // Orange color
            .thenAccept { materialInstance ->
                modelRenderable.material = materialInstance
                addNodeToScene(modelRenderable)
            }
            .exceptionally { throwable ->
                Log.e("ModelRendering", "Error applying orange material", throwable)
                return@exceptionally null
            }
    }

    private fun addNodeToScene(model: ModelRenderable) {
        if (sceneView != null) {
            val transformationSystem = makeTransformationSystem()

            val dragTransformableNode = DragTransformableNode(6f, transformationSystem)

            dragTransformableNode.renderable = model
            dragTransformableNode.localPosition = Vector3(0f, 0f, -1f)


            val scaleFactor = calculateScaleFactor(model)
            dragTransformableNode.localScale = Vector3(scaleFactor, scaleFactor, scaleFactor)

            sceneView.getScene().addChild(dragTransformableNode)
            dragTransformableNode.select()

            addBasicLighting()

            sceneView.getScene()
                .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                    transformationSystem.onTouch(hitTestResult, motionEvent)
                }

            resetCameraView()
        }
    }

    private fun makeTransformationSystem(): TransformationSystem {
        val footprintSelectionVisualizer = FootprintSelectionVisualizer()
        return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
    }

    private fun addBasicLighting() {
        val pointLight = Light.builder(Light.Type.POINT)
            .setIntensity(10.0f)
            .build()

        val pointLightNode = Node().apply {
            light = pointLight
            localPosition = Vector3(0f, 2f, -1f)
        }

        sceneView.getScene().addChild(pointLightNode)
    }

    private fun calculateScaleFactor(model: ModelRenderable): Float {
        val modelSize = 1.0f
        return when {
            modelSize > 2.0f -> 0.05f
            modelSize > 1.0f -> 0.1f
            else -> 0.2f
        }
    }

    private fun resetCameraView() {
        val camera = sceneView.scene.camera
        camera.worldPosition = Vector3(0f, 0f, 1.5f)  // Adjust camera position based on model size
        val cameraRotation = Quaternion.lookRotation(Vector3(0f, 0f, -1f), Vector3(0f, 1f, 0f))
        camera.localRotation = cameraRotation
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onResume() {
        super.onResume()
        try {
            sceneView.resume()
        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            sceneView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        var EXTRA_MODEL_TYPE = "modelType"
    }
}
