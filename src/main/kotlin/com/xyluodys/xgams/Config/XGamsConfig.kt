package com.xyluodys.xgams.Config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.common.platform.Awake
import taboolib.common.LifeCycle

// 配置变量不定义了，草你妈
object ConfigManager {
    @Config("config.yml")
    lateinit var config: Configuration

    @Awake(LifeCycle.ENABLE)

    fun reload() {
        config.reload()
    }
}
