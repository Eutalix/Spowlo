package com.bobbyesp.spowlo.utils

import androidx.room.Room
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.database.AppDatabase
import com.bobbyesp.spowlo.database.Backup
import com.bobbyesp.spowlo.database.CommandShortcut
import com.bobbyesp.spowlo.database.CommandTemplate
import com.bobbyesp.spowlo.database.CookieProfile
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DatabaseUtil {
    private val format = Json { prettyPrint = true }
    private const val DATABASE_NAME = "app_database"
    private val db = Room.databaseBuilder(
        context, AppDatabase::class.java, DATABASE_NAME
    ).build()

    private val dao = db.songsInfoDao()
    fun insertInfo(vararg infoList: DownloadedSongInfo) {
        applicationScope.launch(Dispatchers.IO) {
            // Replace the previous row for the same file so a re-download refreshes its log and date.
            infoList.forEach { dao.deleteInfoByPathAndInsert(it) }
        }
    }

    // Lyrics sidecars were once recorded as songs; keep them out of every consumer.
    fun getMediaInfo() = dao.getAllMedia()
        .map { songs -> songs.filterNot { it.songPath.isLyricsSidecar() } }

    fun clearHistory() {
        applicationScope.launch(Dispatchers.IO) {
            dao.deleteAllMediaFromDb()
        }
    }

    /** Waits for the wipe to finish, for callers that write straight back in. */
    suspend fun clearHistoryNow() = withContext(Dispatchers.IO) {
        dao.deleteAllMediaFromDb()
    }

    suspend fun getMediaPaths(): List<String> = dao.getAllPaths()

    suspend fun getAllMediaOnce(): List<DownloadedSongInfo> = dao.getAllMediaList()

    suspend fun insertImportedInfo(info: DownloadedSongInfo): Boolean =
        dao.insertInfoDistinctByPath(info)

    suspend fun insertImportedInfoList(infoList: List<DownloadedSongInfo>): Int =
        dao.insertInfoListDistinctByPath(infoList)

    suspend fun getInfoByPath(path: String): DownloadedSongInfo? = dao.getInfoByPath(path)

    suspend fun updateSongInfo(info: DownloadedSongInfo) = dao.updateInfo(info)

    fun getTemplateFlow() = dao.getTemplateFlow()

    fun getCookiesFlow() = dao.getCookieProfileFlow()

    fun getShortcuts() = dao.getCommandShortcuts()

    suspend fun deleteShortcut(shortcut: CommandShortcut) = dao.deleteShortcut(shortcut)
    suspend fun insertShortcut(shortcut: CommandShortcut) = dao.insertShortcut(shortcut)

    suspend fun getCookieById(id: Int) = dao.getCookieById(id)
    suspend fun deleteCookieProfile(profile: CookieProfile) = dao.deleteCookieProfile(profile)

    suspend fun insertCookieProfile(profile: CookieProfile) = dao.insertCookieProfile(profile)

    suspend fun updateCookieProfile(profile: CookieProfile) = dao.updateCookieProfile(profile)
    private suspend fun getTemplateList() = dao.getTemplateList()
    private suspend fun getShortcutList() = dao.getShortcutList()
    suspend fun deleteInfoListByIdList(idList: List<Int>, deleteFile: Boolean = false) =
        dao.deleteInfoListByIdList(idList, deleteFile)

    suspend fun getInfoById(id: Int): DownloadedSongInfo = dao.getInfoById(id)
    suspend fun getInfoByIdOrNull(id: Int): DownloadedSongInfo? = dao.getInfoByIdOrNull(id)
    suspend fun deleteInfoById(id: Int) = dao.deleteInfoById(id)

    suspend fun insertTemplate(commandTemplate: CommandTemplate) =
        dao.insertTemplate(commandTemplate)

    suspend fun updateTemplate(commandTemplate: CommandTemplate) {
        dao.updateTemplate(commandTemplate)
    }

    suspend fun deleteTemplateById(id: Int) = dao.deleteTemplateById(id)
    suspend fun exportTemplatesToJson(): String {
        return format.encodeToString(
            Backup(
                templates = getTemplateList(), shortcuts = getShortcutList()
            )
        )
    }

    suspend fun importTemplatesFromJson(json: String): Int {
        val templateList = getTemplateList()
        val shortcutList = getShortcutList()
        var cnt = 0
        try {
            format.decodeFromString<Backup>(json).run {
                templates.filterNot {
                    templateList.contains(it)
                }.run {
                    dao.importTemplates(this)
                    cnt += size
                }
                dao.insertAllShortcuts(shortcuts.filterNot {
                    shortcutList.contains(it)
                }.apply { cnt += size })
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cnt
    }

    private const val TAG = "DatabaseUtil"

}