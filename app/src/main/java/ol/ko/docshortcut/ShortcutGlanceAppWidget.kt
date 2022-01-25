package ol.ko.docshortcut

import androidx.compose.runtime.Composable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.text.Text

class ShortcutGlanceAppWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShortcutGlanceWidget()
}

class ShortcutGlanceWidget: GlanceAppWidget() {
    @Composable
    override fun Content() {
        Text(text = "Hello world!")
    }

}