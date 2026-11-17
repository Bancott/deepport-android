package com.example.deepseekclone.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * 应用内相机（CameraX）：全屏预览 + 快门，另有闪光灯、前后摄切换、点按对焦。
 * 拍完把临时文件交给上层导入附件；权限被拒时给一个明确的重新授权入口。
 */
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onCaptured: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    // 点按对焦：位置 + 透明度（点了才出现，1 秒后淡出）
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAlpha = remember { Animatable(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result -> granted = result }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (granted) {
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    // TextureView 模式：SurfaceView 在 Compose 里层级/截图都更脆，COMPATIBLE 更稳
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }
            val imageCapture = remember {
                ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(previewView) {
                        detectTapGestures { tap ->
                            val point = previewView.meteringPointFactory
                                .createPoint(tap.x, tap.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            runCatching { camera?.cameraControl?.startFocusAndMetering(action) }
                            focusPoint = tap
                        }
                    },
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 对焦圈：跟着点按位置出现，淡入后 0.9 秒淡出
            LaunchedEffect(focusPoint) {
                if (focusPoint != null) {
                    focusAlpha.snapTo(0f)
                    focusAlpha.animateTo(1f)
                    kotlinx.coroutines.delay(900)
                    focusAlpha.animateTo(0f)
                    focusPoint = null
                }
            }
            focusPoint?.let { point ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (point.x - 34.dp.toPx()).roundToInt(),
                                (point.y - 34.dp.toPx()).roundToInt(),
                            )
                        }
                        .size(68.dp)
                        .alpha(focusAlpha.value)
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp)),
                )
            }

            LaunchedEffect(previewView, lensFacing) {
                runCatching {
                    val provider = context.cameraProvider()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture,
                    )
                    imageCapture.targetRotation = previewView.display?.rotation ?: 0
                    // 换镜头后手电筒归零（前置一般没有闪光灯）
                    camera?.cameraControl?.enableTorch(
                        torchOn && camera?.cameraInfo?.hasFlashUnit() == true,
                    )
                }.onFailure {
                    error = "相机启动失败：${it.message.orEmpty()}"
                }
            }

            // 顶栏右侧：闪光灯 / 前后摄
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircleIconButton(
                    contentDescription = if (torchOn) "关闭闪光灯" else "打开闪光灯",
                    onClick = {
                        val next = !torchOn
                        torchOn = next
                        runCatching { camera?.cameraControl?.enableTorch(next) }
                    },
                    containerColor = Color.Black.copy(alpha = 0.35f),
                ) {
                    Icon(
                        imageVector = if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                CircleIconButton(
                    contentDescription = "切换前后摄像头",
                    onClick = {
                        torchOn = false
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    containerColor = Color.Black.copy(alpha = 0.35f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cameraswitch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.18f))
                    .navigationBarsPadding()
                    .padding(vertical = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                ShutterButton(
                    onClick = {
                        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                    onCaptured(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    error = "拍照失败：${exception.message.orEmpty()}"
                                }
                            },
                        )
                    },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "需要相机权限才能拍照",
                    style = DsText.body.copy(fontSize = 16.sp),
                    color = Color.White,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("授予权限", style = DsText.button.copy(fontSize = 15.sp), color = Color.White)
                }
            }
        }

        // 顶栏：关闭 + 标题（关闭按钮单独放左边，右侧留给闪光灯/切换）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                contentDescription = "关闭相机",
                onClick = onClose,
                containerColor = Color.Black.copy(alpha = 0.35f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = "拍照",
            style = DsText.titleBar,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
        )

        error?.let { message ->
            Text(
                text = message,
                style = DsText.caption.copy(fontSize = 13.sp),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "拍照", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(this),
        )
    }
