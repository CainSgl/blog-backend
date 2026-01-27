package com.cainsgl.senstitve.config

import com.alibaba.fastjson2.JSON
import com.github.houbb.sensitive.word.bs.SensitiveWordBs
import com.github.houbb.sensitive.word.support.check.WordChecks
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions
import com.github.houbb.sensitive.word.support.tag.WordTags
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.springframework.beans.factory.annotation.Value

import org.springframework.stereotype.Component
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}
@Component
class SensitiveWord
{
    @Value("\${forbidden.filePath}")
    private lateinit var forbiddenFilePath: String
    private val current = AtomicReference<SensitiveWordBs>()
    fun getBs() : SensitiveWordBs
    {
        return current.get()
    }
    fun replace(originContent:String?):String?
    {
        if(originContent == null)
        {
            return null
        }
        return getBs().replace(originContent)
    }

    private lateinit var fileAlterationMonitor: FileAlterationMonitor
    @PostConstruct
    private fun init()
    {
        //加载默认的
        log.info { "加载敏感词文件路径为: $forbiddenFilePath" }
        val file= File(forbiddenFilePath)
        current.set( createBs(loadBsArray(file)))
        
        val fileAlterationObserver = FileAlterationObserver(file.parentFile)
        val listener: FileAlterationListener = object: FileAlterationListenerAdaptor(){
            override fun onFileChange(changedFile: File)
            {
                if (changedFile.absolutePath == file.absolutePath) {
                    log.info { "敏感词文件发生改变，重新加载敏感词文件路径为: $forbiddenFilePath" }
                    current.set(createBs(loadBsArray(file)))
                }
            }
        }
        fileAlterationObserver.addListener(listener)
        
        // 创建监控器，每5秒检查一次文件变化
        fileAlterationMonitor = FileAlterationMonitor(5000)
        fileAlterationMonitor.addObserver(fileAlterationObserver)
        fileAlterationMonitor.start()
        log.info { "敏感词文件监控器已启动，检查间隔: 5秒" }
    }
    
    @PreDestroy
    private fun stopObserver() {
        try {
            fileAlterationMonitor.stop()
            log.info { "敏感词文件监控器已停止" }
        } catch (e: Exception) {
            log.error(e) { "停止敏感词文件监控器失败" }
        }
    }
   private fun loadBsArray(file:File): List<String>
    {
        val jsonContent: String = Files.readString(file.toPath(), StandardCharsets.UTF_8)
       return JSON.parseArray(jsonContent, String::class.java)
    }

    private fun createBs(words: List<String>): SensitiveWordBs
    {
        return  SensitiveWordBs.newInstance()
            .ignoreCase(true)
            .ignoreWidth(true)
            .ignoreNumStyle(true)
            .ignoreChineseStyle(true)
            .ignoreEnglishStyle(true)
            .ignoreRepeat(false)
            .enableNumCheck(false)
            .enableEmailCheck(false)
            .enableUrlCheck(false)
            .enableIpv4Check(false)
            .enableWordCheck(true)
            .wordFailFast(true)
            .wordCheckWord(WordChecks.word())
            .numCheckLen(8)
            .wordTag(WordTags.none())
            .charIgnore(SensitiveWordCharIgnores.defaults())
            .wordResultCondition(WordResultConditions.alwaysTrue())
            .wordDeny{ words }
            .init()
    }
}