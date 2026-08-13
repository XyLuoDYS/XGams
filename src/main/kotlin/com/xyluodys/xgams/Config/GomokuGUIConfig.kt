package com.xyluodys.xgams.Config

import com.xyluodys.xgams.util.TextParser
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.common.platform.Awake
import taboolib.common.LifeCycle

object GomokuGUIConfig {

    /** 九个棋盘区域名称（固定顺序） */
    val REGIONS = listOf(
        "left-top", "top", "right-top",
        "left", "center", "right",
        "left-bottom", "bottom", "right-bottom"
    )

    /**
     * 棋盘星位（天元 + 四角星），0-based (row, col)
     * 记谱 d4/l4/h8/d12/l12 → (3,3)/(3,11)/(7,7)/(11,3)/(11,11)
     * 该集合在行列交换下完全不变，因此不依赖 board 行/列方向约定
     * 这些位置在未落子状态下使用各自区域的 starEmpty 样式渲染
     */
    val STAR_POSITIONS = setOf(
        3 to 3,    // d4
        3 to 11,   // l4
        7 to 7,    // h8 天元
        11 to 3,   // d12
        11 to 11   // l12
    )

    @Config("GUI/gomoku.yml")
    lateinit var config: Configuration

    // Layout
    var bigLayout: List<String> = emptyList()
    var smallLayout: List<String> = emptyList()

    // 九个区域的标记字符（big和small共用）
    var cellMarkers: Map<String, String> = emptyMap()

    // 每个尺寸、每个区域的棋子样式
    data class RegionStyle(
        var empty: String = "&70",
        var black: String = "&c1",
        var white: String = "&92",
        var markedBlack: String = "&e1",
        var markedWhite: String = "&e2",
        var winMarkedBlack: String = "&a1",
        var winMarkedWhite: String = "&a2",
        // 星位（天元+四角星）未落子时的显示样式；非空时用它，否则回退 empty
        var starEmpty: String = "&71"
    )
    var bigStyles: Map<String, RegionStyle> = REGIONS.associateWith { RegionStyle() }
    var smallStyles: Map<String, RegionStyle> = REGIONS.associateWith { RegionStyle() }

    // Font
    var fontNamespace: String = "xgams:gomoku"

    // Texture
    var textureEnabled: Boolean = false
    var negativeSpace: String = "\uE010"

    // Dialog
    var dialogTitle: String = "五子棋"
    var dialogWidth: Int = 400
    var bigButtonSpacing: Int = 5
    var bigOtherRowSpacing: Int = 5
    var bigMessageSpacing: Int = 5
    var bigMessageRowSpacing: Int = 5
    var smallButtonSpacing: Int = 5
    var smallOtherRowSpacing: Int = 5
    var smallMessageSpacing: Int = 5
    var smallMessageRowSpacing: Int = 5

    // 五子连珠动画时间配置（ticks，1秒=20ticks）
    var winMarkDelay: Int = 2       // 每颗棋子标记之间的延迟
    var winMessageDelay: Int = 10   // 五子全部标记后到显示游戏结束消息的延迟

    // Messages
    var cellHoverTemplate: String = "第{row}行 第{col}列"
    var toggleHover: String = "点击切换棋盘大小"

    // GameOver messages
    var bigWinYou: String = "&a您成功获胜"
    var bigWinOpponent: String = "&c对方成功获胜"
    var bigDrawMessage: String = "&e平局"
    var smallWinYou: String = "&a您成功获胜"
    var smallWinOpponent: String = "&c对方成功获胜"
    var smallDrawMessage: String = "&e平局"

    // Button
    var toggleToSmallDisplay: String = "&e切换小棋盘"
    var toggleToBigDisplay: String = "&e切换大棋盘"
    var bigActions: List<String> = listOf("change: small")
    var smallActions: List<String> = listOf("change: big")

    // Sound
    var placeSounds: List<String> = emptyList()      // 落子音效（随机播放）
    var winMarkSounds: List<String> = emptyList()    // 连珠获胜动画音效（顺序播放，不足5个循环）
    var placePitchEnabled: Boolean = false           // 落子音效是否启用随机音调
    var placePitches: List<Float> = emptyList()      // 落子音调列表（随机选择，0.5~2.0）

    @Awake(LifeCycle.ENABLE)
    fun load() {
        config.onReload { updateVars() }
        updateVars()
    }

    fun reload() {
        config.reload()
    }

    private fun updateVars() {
        bigLayout = config.getStringList("Layout.big").mapNotNull { TextParser.unescape(it) }
        smallLayout = config.getStringList("Layout.small").mapNotNull { TextParser.unescape(it) }

        // 加载九个区域的标记字符
        cellMarkers = REGIONS.associateWith { region ->
            TextParser.unescape(config.getString("Layout.cell-markers.$region")) ?: region.first().uppercaseChar().toString()
        }

        // 加载每个尺寸每个区域的样式
        bigStyles = REGIONS.associateWith { region ->
            RegionStyle(
                empty = TextParser.unescape(config.getString("Style.big.$region.empty")) ?: "&70",
                black = TextParser.unescape(config.getString("Style.big.$region.black")) ?: "&c1",
                white = TextParser.unescape(config.getString("Style.big.$region.white")) ?: "&92",
                markedBlack = TextParser.unescape(config.getString("Style.big.$region.marked-black")) ?: "&e1",
                markedWhite = TextParser.unescape(config.getString("Style.big.$region.marked-white")) ?: "&e2",
                winMarkedBlack = TextParser.unescape(config.getString("Style.big.$region.win-marked-black")) ?: "&a1",
                winMarkedWhite = TextParser.unescape(config.getString("Style.big.$region.win-marked-white")) ?: "&a2",
                starEmpty = TextParser.unescape(config.getString("Style.big.$region.star-empty")) ?: "&71"
            )
        }
        smallStyles = REGIONS.associateWith { region ->
            RegionStyle(
                empty = TextParser.unescape(config.getString("Style.small.$region.empty")) ?: "&70",
                black = TextParser.unescape(config.getString("Style.small.$region.black")) ?: "&c1",
                white = TextParser.unescape(config.getString("Style.small.$region.white")) ?: "&92",
                markedBlack = TextParser.unescape(config.getString("Style.small.$region.marked-black")) ?: "&e1",
                markedWhite = TextParser.unescape(config.getString("Style.small.$region.marked-white")) ?: "&e2",
                winMarkedBlack = TextParser.unescape(config.getString("Style.small.$region.win-marked-black")) ?: "&a1",
                winMarkedWhite = TextParser.unescape(config.getString("Style.small.$region.win-marked-white")) ?: "&a2",
                starEmpty = TextParser.unescape(config.getString("Style.small.$region.star-empty")) ?: "&71"
            )
        }

        textureEnabled = config.getBoolean("Texture.enabled", false)
        negativeSpace = TextParser.unescape(config.getString("Texture.negative-space")) ?: "\uE010"

        fontNamespace = TextParser.unescape(config.getString("Font.namespace")) ?: "xgams:gomoku"

        dialogTitle = TextParser.unescape(config.getString("Dialog.title")) ?: "五子棋"
        dialogWidth = config.getInt("Dialog.width", 400)
        bigButtonSpacing = config.getInt("Dialog.size-big.button-spacing", 5)
        bigOtherRowSpacing = config.getInt("Dialog.size-big.other-spacing", 5)
        bigMessageSpacing = config.getInt("Dialog.size-big.message-spacing", 5)
        bigMessageRowSpacing = config.getInt("Dialog.size-big.message-row-spacing", 5)
        smallButtonSpacing = config.getInt("Dialog.size-small.button-spacing", 5)
        smallOtherRowSpacing = config.getInt("Dialog.size-small.other-spacing", 5)
        smallMessageSpacing = config.getInt("Dialog.size-small.message-spacing", 5)
        smallMessageRowSpacing = config.getInt("Dialog.size-small.message-row-spacing", 5)

        winMarkDelay = config.getInt("Dialog.win-mark-delay", 2)
        winMessageDelay = config.getInt("Dialog.win-message-delay", 10)

        cellHoverTemplate = TextParser.unescape(config.getString("Messages.cell-hover")) ?: "第{row}行 第{col}列"
        toggleHover = TextParser.unescape(config.getString("Messages.toggle-hover")) ?: "点击切换棋盘大小"

        bigWinYou = TextParser.unescape(config.getString("GameOver.size-big.win-you")) ?: "&a您成功获胜"
        bigWinOpponent = TextParser.unescape(config.getString("GameOver.size-big.win-opponent")) ?: "&c对方成功获胜"
        bigDrawMessage = TextParser.unescape(config.getString("GameOver.size-big.draw")) ?: "&e平局"
        smallWinYou = TextParser.unescape(config.getString("GameOver.size-small.win-you")) ?: "&a您成功获胜"
        smallWinOpponent = TextParser.unescape(config.getString("GameOver.size-small.win-opponent")) ?: "&c对方成功获胜"
        smallDrawMessage = TextParser.unescape(config.getString("GameOver.size-small.draw")) ?: "&e平局"

        toggleToSmallDisplay = TextParser.unescape(config.getString("Button.size-change.size-big.display")) ?: "&e切换小棋盘"
        toggleToBigDisplay = TextParser.unescape(config.getString("Button.size-change.size-small.display")) ?: "&e切换大棋盘"
        bigActions = config.getStringList("Button.size-change.size-big.action").ifEmpty { listOf("change: small") }.mapNotNull { TextParser.unescape(it) }
        smallActions = config.getStringList("Button.size-change.size-small.action").ifEmpty { listOf("change: big") }.mapNotNull { TextParser.unescape(it) }

        // 音效配置
        placeSounds = config.getStringList("Sound.place").mapNotNull { TextParser.unescape(it) }
        winMarkSounds = config.getStringList("Sound.win-mark").mapNotNull { TextParser.unescape(it) }
        placePitchEnabled = config.getBoolean("Sound.place-pitch.enabled", false)
        placePitches = config.getStringList("Sound.place-pitch.pitches").mapNotNull { it.toFloatOrNull() }
    }

    fun getStoneStyle(
        size: String,
        region: String,
        value: Int,
        marked: Boolean = false,
        winMarked: Boolean = false,
        boardRow: Int = -1,
        col: Int = -1
    ): String {
        val styles = if (size == "big") bigStyles else smallStyles
        val style = styles[region] ?: return "?"
        return when {
            // 未落子：星位(d4/l4/h8/d12/l12)使用 starEmpty，否则普通空位
            value == 0 -> {
                if (STAR_POSITIONS.contains(boardRow to col) && style.starEmpty.isNotBlank()) {
                    style.starEmpty
                } else {
                    style.empty
                }
            }
            value == 1 && winMarked -> style.winMarkedBlack
            value == 1 && marked -> style.markedBlack
            value == 1 -> style.black
            value == 2 && winMarked -> style.winMarkedWhite
            value == 2 && marked -> style.markedWhite
            value == 2 -> style.white
            else -> "?"
        }
    }

    fun getLayout(size: String): List<String> {
        return when (size) {
            "big" -> bigLayout
            else -> smallLayout
        }
    }

    fun getToggleButtonText(currentSize: String): String {
        return when (currentSize) {
            "big" -> toggleToSmallDisplay
            else -> toggleToBigDisplay
        }
    }

    fun getButtonSpacing(size: String): Int {
        return when (size) {
            "big" -> bigButtonSpacing
            else -> smallButtonSpacing
        }
    }

    fun getOtherRowSpacing(size: String): Int {
        return when (size) {
            "big" -> bigOtherRowSpacing
            else -> smallOtherRowSpacing
        }
    }

    fun getMessageSpacing(size: String): Int {
        return when (size) {
            "big" -> bigMessageSpacing
            else -> smallMessageSpacing
        }
    }

    fun getMessageRowSpacing(size: String): Int {
        return when (size) {
            "big" -> bigMessageRowSpacing
            else -> smallMessageRowSpacing
        }
    }

    fun getGameOverMessage(winner: Int, size: String): String {
        return when (size) {
            "big" -> when (winner) {
                1 -> bigWinYou
                2 -> bigWinOpponent
                else -> bigDrawMessage
            }
            else -> when (winner) {
                1 -> smallWinYou
                2 -> smallWinOpponent
                else -> smallDrawMessage
            }
        }
    }

    fun getActions(currentSize: String): List<String> {
        return when (currentSize) {
            "big" -> bigActions
            else -> smallActions
        }
    }
}
