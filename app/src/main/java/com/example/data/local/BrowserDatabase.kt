package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.CookieDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.dao.ShortcutDao
import com.example.data.local.dao.SitePermissionDao
import com.example.data.local.dao.TabDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.CookieEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.ShortcutEntity
import com.example.data.local.entity.SitePermissionEntity
import com.example.data.local.entity.TabEntity
import com.example.data.local.entity.UserAccountEntity

/**
 * Base de datos Room principal del Navegador Web.
 * Contiene las tablas para gestión de pestañas, marcadores, historial, descargas,
 * cookies de navegación, permisos por sitio web, cuentas vinculadas y accesos directos configurables.
 */
@Database(
    entities = [
        TabEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        CookieEntity::class,
        UserAccountEntity::class,
        SitePermissionEntity::class,
        ShortcutEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {

    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun cookieDao(): CookieDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun sitePermissionDao(): SitePermissionDao
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
