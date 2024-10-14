package com.ahuaman.arsceneviewcompose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahuaman.arsceneviewcompose.ui.theme.ARSceneViewComposeTheme
import com.google.ar.core.Config
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARSceneViewComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ARComposable()

                        //Top Icon back
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clickable {
                                finish()
                            }
                        )
                    }

                }
            }
        }
    }
}

//Model
private const val kModelFile = "https://ahuamana.github.io/models-ar/insect/ant.glb"

@Composable
fun ARComposable() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        var isLoading by remember { mutableStateOf(false) }
        var planeRenderer by remember { mutableStateOf(true) }
        val engine = rememberEngine()
        val renderer = rememberRenderer(engine)
        val modelLoader = rememberModelLoader(engine)
        val childNodes = rememberNodes()
        val coroutineScope = rememberCoroutineScope()

        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            renderer = renderer,
            modelLoader = modelLoader,
            planeRenderer = planeRenderer,
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode =
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            onSessionUpdated = { _, frame ->
                if (childNodes.isNotEmpty()) return@ARScene

                frame.getUpdatedPlanes()
                    .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                    ?.let { plane ->
                        isLoading = true
                        childNodes += AnchorNode(
                            engine = engine,
                            anchor = plane.createAnchor(plane.centerPose)
                        ).apply {
                            isEditable = true
                            coroutineScope.launch {
                                modelLoader.loadModelInstance(kModelFile)?.let {
                                    addChildNode(
                                        ModelNode(
                                            modelInstance = it,
                                            // Scale to fit in a 0.5 meters cube
                                            scaleToUnits = 1f,
                                            // Bottom origin instead of center so the
                                            // model base is on floor level
                                            centerOrigin = Position(0f, 0f)
                                        ).apply {
                                            isEditable = true
                                        }
                                    )
                                }
                                planeRenderer = false
                                isLoading = false
                            }
                        }
                    }
            },
            onSessionFailed = {
                Toast.makeText(context, "Session failed", Toast.LENGTH_SHORT).show()
            }
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center),
                color = Color.Magenta
            )
        }
    }

}