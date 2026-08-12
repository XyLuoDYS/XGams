package com.xyluodys.xgams.util

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 文本解析工具 喵 (。・ω・。)
 *
 * 三合一解析：\uXXXX 转义 + & 颜色代码 + 字体分割（私用区字符绑定自定义字体）
 */
object TextParser {

    /** 匹配 \uXXXX 形式的 Unicode 转义（兜底处理，SnakeYAML 双引号内也支持） */
    private val unicodeEscape = Regex("\\\\u([0-9a-fA-F]{4})")

    /** 私用区字符范围（自定义字体图标） */
    private val privateUseRange = '\uE000'..'\uF8FF'

    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    // 转义解析

    /**
     * 将 \uXXXX 字面量转为真正的 Unicode 字符
     * 返回 null 时调用方可提供默认值
     */
    fun unescape(raw: String?): String? {
        if (raw == null) return null
        return raw.replace(unicodeEscape) { m ->
            m.groupValues[1].toInt(16).toChar().toString()
        }
    }

    // 颜色 + 字体 统一解析
    @Suppress("UNUSED_PARAMETER")
    fun parse(
        raw: String?,
        fontNamespace: String? = null,
        useTexture: Boolean = false
    ): Component {
        val unescaped = unescape(raw) ?: return Component.empty()

        // 解析 & 颜色/格式代码（同时兼容 § 形式）
        val colored = legacySerializer.deserialize(unescaped.replace('§', '&'))

        // 配置了自定义字体命名空间时，私用区字符自动绑定自定义字体
        if (fontNamespace != null) {
            return splitFont(colored, Key.key(fontNamespace))
        }
        return colored
    }

    fun splitFont(comp: Component, fontKey: Key): Component {
        if (comp is TextComponent) {
            val text = comp.content()
            if (text.isNotEmpty() && text.any { it in privateUseRange }) {
                val builder = comp.toBuilder()
                builder.content("")

                var i = 0
                while (i < text.length) {
                    val c = text[i]
                    val isPrivate = c in privateUseRange
                    val sb = StringBuilder()
                    sb.append(c)
                    var j = i + 1
                    // 连续的同类型字符合并为一段
                    while (j < text.length && (text[j] in privateUseRange) == isPrivate) {
                        sb.append(text[j])
                        j++
                    }
                    val segment = Component.text(sb.toString()).style(comp.style())
                    builder.append(
                        if (isPrivate) segment.font(fontKey) else segment
                    )
                    i = j
                }

                // 递归处理原子组件，并挂回 builder
                val newChildren = comp.children().map { splitFont(it, fontKey) }
                builder.append(newChildren)
                return builder.build()
            }
        }

        // 当前节点 content 不含私用区字符
        val newChildren = comp.children().map { splitFont(it, fontKey) }
        return comp.children(newChildren)
    }
}
