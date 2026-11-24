package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.CoachTtsBridge
import com.example.network.observeNetworkConnectivity
import java.io.IOException

private const val GAME_HOST = "appassets.androidplatform.net"
private const val GAME_ASSET_PATH = "/assets/_________4fa4778a.html"
private const val GAME_ASSET_FILE = "_________4fa4778a.html"
private const val GAME_URL = "https://$GAME_HOST$GAME_ASSET_PATH"

private data class JsDialogState(
  val message: String,
  val isConfirm: Boolean,
  val result: JsResult,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoachGameScreen() {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val backgroundArgb = MaterialTheme.colorScheme.background.toArgb()

  val isOnline by
    remember(context) { observeNetworkConnectivity(context) }
      .collectAsStateWithLifecycle(initialValue = true)
  var offlineBannerDismissed by remember { mutableStateOf(false) }

  var isInitialLoading by remember { mutableStateOf(true) }
  var loadProgress by remember { mutableIntStateOf(0) }
  var customFullscreenView by remember { mutableStateOf<View?>(null) }
  var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
  var jsDialog by remember { mutableStateOf<JsDialogState?>(null) }
  var showExitDialog by remember { mutableStateOf(false) }

  val ttsBridge = remember(context) { CoachTtsBridge(context) }

  val webView = remember {
    WebView(context).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      setBackgroundColor(backgroundArgb)
      setLayerType(View.LAYER_TYPE_HARDWARE, null)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
      }
      overScrollMode = View.OVER_SCROLL_NEVER
      isVerticalScrollBarEnabled = false
      isHorizontalScrollBarEnabled = false

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        @Suppress("DEPRECATION")
        databaseEnabled = true
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        textZoom = 100
        cacheMode = WebSettings.LOAD_DEFAULT
      }

      addJavascriptInterface(ttsBridge, "AndroidTTS")
    }
  }

  DisposableEffect(lifecycleOwner, webView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          webView.onResume()
          webView.resumeTimers()
        }
        Lifecycle.Event.ON_PAUSE -> {
          webView.onPause()
          webView.pauseTimers()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      ttsBridge.shutdown()
      webView.stopLoading()
      webView.removeJavascriptInterface("AndroidTTS")
      webView.destroy()
    }
  }

  // Toggle system bar visibility during WebView fullscreen match mode
  DisposableEffect(customFullscreenView) {
    val window = (context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    if (customFullscreenView != null) {
      controller?.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      controller?.hide(WindowInsetsCompat.Type.systemBars())
    } else {
      controller?.show(WindowInsetsCompat.Type.systemBars())
    }
    onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
  }

  BackHandler {
    if (customFullscreenView != null) {
      customViewCallback?.onCustomViewHidden()
      customFullscreenView = null
      customViewCallback = null
      return@BackHandler
    }

    webView.evaluateJavascript(
      "(function(){ return window.__androidHandleBack ? window.__androidHandleBack() : false; })()"
    ) { result ->
      if (result != "true") {
        if (webView.canGoBack()) {
          webView.goBack()
        } else {
          showExitDialog = true
        }
      }
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .then(
          if (customFullscreenView == null) {
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
          } else {
            Modifier
          }
        )
  ) {
    AndroidView(
      factory = {
        webView.apply {
          webViewClient =
            object : WebViewClient() {
              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
              ): WebResourceResponse? {
                val url = request?.url ?: return null
                if (url.host == GAME_HOST && url.path == GAME_ASSET_PATH) {
                  return try {
                    val stream = context.assets.open(GAME_ASSET_FILE)
                    WebResourceResponse(
                      "text/html",
                      "UTF-8",
                      200,
                      "OK",
                      mapOf("Cache-Control" to "no-store"),
                      stream,
                    )
                  } catch (_: IOException) {
                    null
                  }
                }
                return null
              }

              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
              ): Boolean {
                val uri = request?.url ?: return false
                if (uri.host == GAME_HOST) return false
                return try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())))
                  true
                } catch (_: Exception) {
                  false
                }
              }

              override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isInitialLoading = true
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                isInitialLoading = false
                view?.evaluateJavascript(INJECTED_ANDROID_BRIDGE_JS, null)
              }
            }

          webChromeClient =
            object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadProgress = newProgress
                if (newProgress >= 95) {
                  isInitialLoading = false
                }
              }

              override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) {
                  callback?.onCustomViewHidden()
                  return
                }
                customFullscreenView?.let { customViewCallback?.onCustomViewHidden() }
                customFullscreenView = view
                customViewCallback = callback
              }

              override fun onHideCustomView() {
                customFullscreenView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
              }

              override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?,
              ): Boolean {
                if (result == null) return false
                jsDialog = JsDialogState(message.orEmpty(), isConfirm = false, result = result)
                return true
              }

              override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?,
              ): Boolean {
                if (result == null) return false
                jsDialog = JsDialogState(message.orEmpty(), isConfirm = true, result = result)
                return true
              }
            }

          if (url == null) {
            loadUrl(GAME_URL)
          }
        }
      },
      modifier = Modifier.fillMaxSize().testTag("game_webview"),
    )

    // Non-blocking offline guidance banner (2D simulation remains playable offline)
    AnimatedVisibility(
      visible = !isOnline && !offlineBannerDismissed && customFullscreenView == null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.offline_banner_message),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { webView.reload() },
                modifier = Modifier.testTag("retry_connection_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = stringResource(R.string.action_reload))
              }
              TextButton(
                onClick = { offlineBannerDismissed = true },
                modifier = Modifier.testTag("dismiss_offline_button"),
              ) {
                Text(text = stringResource(R.string.action_dismiss))
              }
            }
          }
        }
      }
    }

    // Native fullscreen custom view overlay for WebChromeClient.onShowCustomView
    customFullscreenView?.let { fsView ->
      Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
          factory = { containerContext ->
            FrameLayout(containerContext).apply {
              (fsView.parent as? ViewGroup)?.removeView(fsView)
              addView(
                fsView,
                FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.MATCH_PARENT,
                ),
              )
            }
          },
          modifier = Modifier.fillMaxSize().testTag("fullscreen_match_container"),
        )
        IconButton(
          onClick = {
            customViewCallback?.onCustomViewHidden()
            customFullscreenView = null
            customViewCallback = null
          },
          modifier =
            Modifier.align(Alignment.TopEnd)
              .padding(12.dp)
              .size(48.dp)
              .testTag("exit_fullscreen_button"),
        ) {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.exit_fullscreen_cd),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
      }
    }

    // Initial stadium loading overlay
    AnimatedVisibility(
      visible = isInitialLoading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("loading_overlay"),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.padding(24.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(56.dp),
          )
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          LinearProgressIndicator(
            progress = { (loadProgress / 100f).coerceIn(0.08f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.width(200.dp).height(6.dp),
          )
          Text(
            text = stringResource(R.string.loading_game),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }

  jsDialog?.let { dialog ->
    AlertDialog(
      onDismissRequest = {
        dialog.result.cancel()
        jsDialog = null
      },
      title = { Text(text = stringResource(R.string.js_dialog_title)) },
      text = { Text(text = dialog.message) },
      confirmButton = {
        Button(
          onClick = {
            dialog.result.confirm()
            jsDialog = null
          },
          modifier = Modifier.testTag("js_dialog_confirm_button"),
        ) {
          Text(text = stringResource(R.string.js_dialog_ok))
        }
      },
      dismissButton =
        if (dialog.isConfirm) {
          {
            TextButton(
              onClick = {
                dialog.result.cancel()
                jsDialog = null
              },
              modifier = Modifier.testTag("js_dialog_cancel_button"),
            ) {
              Text(text = stringResource(R.string.js_dialog_cancel))
            }
          }
        } else {
          null
        },
      modifier = Modifier.testTag("js_alert_dialog"),
    )
  }

  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      title = { Text(text = stringResource(R.string.exit_dialog_title)) },
      text = { Text(text = stringResource(R.string.exit_dialog_message)) },
      confirmButton = {
        Button(
          onClick = {
            showExitDialog = false
            (context as? Activity)?.finish()
          },
          modifier = Modifier.testTag("exit_confirm_button"),
        ) {
          Text(text = stringResource(R.string.exit_dialog_confirm))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showExitDialog = false },
          modifier = Modifier.testTag("exit_cancel_button"),
        ) {
          Text(text = stringResource(R.string.exit_dialog_cancel))
        }
      },
      modifier = Modifier.testTag("exit_confirm_dialog"),
    )
  }
}

private const val INJECTED_ANDROID_BRIDGE_JS =
  """
  (function(){
    if (window.__androidBridgeReady) return;
    window.__androidBridgeReady = true;
    window.__androidHandleBack = function(){
      try {
        var xf = document.getElementById('xf');
        if (xf) { if (typeof window.xfx === 'function') window.xfx(); else xf.remove(); return true; }
        var fs = document.querySelector('.w.fsx');
        if (fs && typeof window.fsc === 'function') { window.fsc(); return true; }
        var pc = document.getElementById('pcard');
        if (pc && pc.style.display && pc.style.display !== 'none') {
          if (typeof window.pcx === 'function') window.pcx(); else pc.style.display = 'none';
          return true;
        }
      } catch(e) {}
      return false;
    };
    if (window.AndroidTTS) {
      var hasNativeVoices = false;
      try {
        hasNativeVoices = !!(window.speechSynthesis && window.speechSynthesis.getVoices && window.speechSynthesis.getVoices().length > 0);
      } catch(e) {}
      if (!hasNativeVoices) {
        window.SpeechSynthesisUtterance = window.SpeechSynthesisUtterance || function(t){
          this.text = t || '';
          this.lang = 'ar';
          this.rate = 1.0;
          this.pitch = 1.0;
        };
        var shim = {
          speak: function(u){
            try {
              if (u && u.text) window.AndroidTTS.speak(String(u.text), Number(u.rate || 1.0), Number(u.pitch || 1.0));
            } catch(e) {}
          },
          cancel: function(){
            try { window.AndroidTTS.stop(); } catch(e) {}
          },
          pause: function(){},
          resume: function(){},
          getVoices: function(){ return [{ lang: 'ar', name: 'Android Arabic TTS', default: true }]; }
        };
        try {
          Object.defineProperty(window, 'speechSynthesis', { value: shim, configurable: true });
        } catch(e) {
          window.speechSynthesis = shim;
        }
      }
    }
  })();
  """
