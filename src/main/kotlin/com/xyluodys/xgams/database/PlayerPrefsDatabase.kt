package com.xyluodys.xgams.database

import taboolib.common.platform.function.warning
import taboolib.module.database.*
import java.io.File
import java.util.UUID
import javax.sql.DataSource

/**
 * 基于 TabooLib 内置数据库模块（taboolib.module.database）的玩家偏好持久化
 */
object PlayerPrefsDatabase {

    private lateinit var host: HostSQLite
    private lateinit var dataSource: DataSource
    private lateinit var table: Table<HostSQLite, SQLite>

    // 表名与列名
    private const val TABLE = "player_prefs"
    private const val COL_UUID = "uuid"
    private const val COL_BOARD_SIZE = "board_size"
    private const val COL_UPDATED_AT = "updated_at"

    /**
     * 初始化数据库：创建 SQLite 连接池与数据表
     * 应在插件启用阶段（onEnable）调用一次
     */
    fun init(dataFolder: File) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        // TabooLib 内置 SQLite 主机（自动打包 SQLite JDBC 驱动）
        host = HostSQLite(File(dataFolder, "player_prefs.db"))
        // 创建连接池（autoRelease=true，由 TabooLib 在 DISABLE 时自动释放）
        dataSource = host.createDataSource()

        table = Table(TABLE, host) {
            add(COL_UUID) {
                type(ColumnTypeSQLite.TEXT, 36) {
                    options(ColumnOptionSQLite.PRIMARY_KEY)
                }
            }
            add(COL_BOARD_SIZE) {
                type(ColumnTypeSQLite.TEXT, 8) {
                    options(ColumnOptionSQLite.NOTNULL)
                }
            }
            add(COL_UPDATED_AT) {
                type(ColumnTypeSQLite.INTEGER) {
                    options(ColumnOptionSQLite.NOTNULL)
                }
            }
        }
        table.createTable(dataSource)
    }

    /** 读取单个玩家已保存的棋盘尺寸；无记录或数据库未初始化时返回 null */
    fun getBoardSize(uuid: UUID): String? {
        if (!::table.isInitialized) return null
        var result: String? = null
        table.select(dataSource) {
            where { COL_UUID eq uuid.toString() }
        }.firstOrNull {
            result = getString(COL_BOARD_SIZE)
        }
        return result
    }

    /** 保存（插入或更新）单个玩家的棋盘尺寸偏好 */
    fun setBoardSize(uuid: UUID, size: String) {
        if (!::table.isInitialized) return
        val uuidStr = uuid.toString()
        val now = System.currentTimeMillis()
        try {
            val exists = table.find(dataSource) { where { COL_UUID eq uuidStr } }
            if (exists) {
                table.update(dataSource) {
                    set(COL_BOARD_SIZE, size)
                    set(COL_UPDATED_AT, now)
                    where { COL_UUID eq uuidStr }
                }
            } else {
                table.insert(dataSource, COL_UUID, COL_BOARD_SIZE, COL_UPDATED_AT) {
                    value(uuidStr, size, now)
                }
            }
        } catch (e: Throwable) {
            warning("保存玩家 $uuidStr 的棋盘尺寸偏好失败: ${e.message}")
        }
    }

    /** 启动时一次性读取全部已保存的棋盘尺寸（uuid -> 尺寸） */
    fun loadAll(): Map<UUID, String> {
        if (!::table.isInitialized) return emptyMap()
        val result = mutableMapOf<UUID, String>()
        table.select(dataSource) {
            // 不带 where 条件，读取所有行
        }.forEach {
            val uuidStr = getString(COL_UUID)
            val size = getString(COL_BOARD_SIZE)
            if (uuidStr != null && size != null) {
                runCatching { UUID.fromString(uuidStr) }.getOrNull()?.let { result[it] = size }
            }
        }
        return result
    }

    /**
     * 关闭前批量保存当前所有玩家的棋盘尺寸偏好
     */
    fun saveAll(prefs: Map<UUID, String>) {
        prefs.forEach { (uuid, size) -> setBoardSize(uuid, size) }
    }
}
