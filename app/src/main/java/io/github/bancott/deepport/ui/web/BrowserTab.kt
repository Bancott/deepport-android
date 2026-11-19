package io.github.bancott.deepport.ui.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * 用 Custom Tabs 打开「真浏览器」页面。
 *
 * 充值、发票这类页面依赖浏览器级能力：支付 scheme 唤起后按 return_url 回跳、浏览器自己的
 * Cookie 仓、微信/支付宝的环境判断。这些在内置 WebView 里都缺，所以这类链路一律交给
 * 支持 Custom Tabs 的浏览器（Chrome / Edge / 系统浏览器），返回到 App 时页面栈还在。
 *
 * 没有可用浏览器时退回普通 ACTION_VIEW，再不行就交给系统选择器。
 */
fun openInBrowserTab(context: Context, url: String, toolbarColor: Int) {
    val uri = Uri.parse(url)
    val customTabs = runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(toolbarColor)
            .build()
    }.getOrNull()

    if (customTabs != null) {
        try {
            customTabs.launchUrl(context, uri)
            return
        } catch (_: ActivityNotFoundException) {
            // 没有支持 Custom Tabs 的浏览器，走下面的兜底
        } catch (_: Exception) {
            // 个别 ROM 会在 launch 时抛 SecurityException，同样兜底
        }
    }

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
