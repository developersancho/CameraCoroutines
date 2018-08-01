package com.beepiz.cameracoroutines.sample

import android.Manifest
import android.annotation.SuppressLint
import android.hardware.camera2.CameraAccessException
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresPermission
import android.support.v7.app.AppCompatActivity
import com.beepiz.cameracoroutines.exceptions.CamException
import com.beepiz.cameracoroutines.sample.autorecorder.recordVideo
import com.beepiz.cameracoroutines.sample.extensions.CamCharacteristics
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import splitties.viewdsl.core.setContentView
import timber.log.Timber

class CamTestActivity : AppCompatActivity() {

    private lateinit var testJob: Job

    private val camThread by lazy { HandlerThread("camera") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = CamTestUi(this)
        setContentView(ui)
        camThread.start()
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        testJob = testCamera()
    }

    override fun onStop() {
        super.onStop()
        testJob.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        camThread.quitSafely()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    private fun testCamera() = launch(UI.immediate) {
        try {
            val camHandler = Handler(camThread.looper)
            val externalFilesDir = getExternalFilesDir(null).absolutePath
            val backVideoPath = "$externalFilesDir/BackRecorded.mp4"
            val frontVideoPath = "$externalFilesDir/FrontRecorded.mp4"
            recordVideo(CamCharacteristics.LensFacing.BACK, backVideoPath, camHandler)
            recordVideo(CamCharacteristics.LensFacing.FRONT, frontVideoPath, camHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e)
        } catch (e: CamException) {
            Timber.e(e)
        } catch (e: Exception) {
            Timber.e(e)
        }
        finish()
    }
}
