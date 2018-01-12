package com.beepiz.cameracoroutines.sample

import android.Manifest
import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresPermission
import android.support.v7.app.AppCompatActivity
import com.beepiz.cameracoroutines.CamCaptureSession
import com.beepiz.cameracoroutines.CamDevice
import com.beepiz.cameracoroutines.extensions.cameraManager
import com.beepiz.cameracoroutines.sample.viewdsl.lazy
import com.beepiz.cameracoroutines.sample.viewdsl.setContentView
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.launch
import timber.log.Timber

class CamTestActivity : AppCompatActivity() {

    private val ui by lazy(::CamTestUi)

    private lateinit var testJob: Job

    private val camThread by kotlin.lazy { HandlerThread("camera") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ui)
        camThread.start()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        testJob = testCamera()
    }

    override fun onPause() {
        super.onPause()
        testJob.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        camThread.quitSafely()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    private fun testCamera() = launch(UI) {
        val camHandler = Handler(camThread.looper)
        try {
            val camManager = cameraManager
            val backCamId = camManager.cameraIdList.firstOrNull {
                val characteristics = camManager.getCameraCharacteristics(it)
                characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK
            } ?: throw NoSuchElementException("No back camera found")
            /*val camCharacteristics = camManager.getCameraCharacteristics(backCamId)
            val configMap = camCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            val sensorOrientation = camCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]*/
            val cam = CamDevice(backCamId, camHandler)
            cam.open()
            ui.surfaceHolderState.createdChannel.consume {
                for (created in this) if (created) break
            }
            val surfaces = listOf(ui.previewSurface)
            val session = cam.createCaptureSession(surfaces)
            session.stateChannel.consume {
                loop@for (state in this) {
                    when(state) {
                        is CamCaptureSession.State.Configured-> break@loop
                        is CamCaptureSession.State.Closed -> throw CancellationException("Session closed!")
                    }
                }
            }
            val captureRequest = session.createCaptureRequest(CamDevice.Template.PREVIEW) {
                surfaces.forEach(it::addTarget)
                it[CaptureRequest.CONTROL_MODE] = CameraMetadata.CONTROL_MODE_AUTO
            }
            session.setRepeatingRequest(captureRequest)
        } catch (e : Exception) {
            Timber.e(e)
        }
    }
}