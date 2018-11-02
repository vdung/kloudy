package vdung.android.kloudy.data.model

import android.net.Uri
import android.os.Parcelable
import androidx.paging.DataSource
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity(tableName = "fileEntry")
@Parcelize
data class FileEntry(
        @PrimaryKey val fileId: Int,
        val url: String,
        val contentType: String,
        val name: String?,
        val lastModified: Date,
        val isSyncing: Boolean = false
) : Parcelable

data class GroupedByMonth(
        val month: String,
        val count: Int
)

@Dao
interface FileEntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(fileEntries: List<FileEntry>): List<Long>

    @Update
    fun update(fileEntries: List<FileEntry>)

    @Delete
    fun delete(fileEntry: FileEntry)

    @Query("UPDATE fileEntry SET isSyncing = 1")
    fun markForSync()

    @Query("DELETE FROM fileEntry WHERE isSyncing = 1")
    fun deleteInvalidEntries()

    @Query("SELECT month, count(*) as count FROM (SELECT strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') AS month FROM fileEntry) GROUP BY month ORDER BY month DESC")
    fun groupFilesByMonth(): List<GroupedByMonth>

    @Query("SELECT * FROM fileEntry ORDER BY lastModified DESC")
    fun getFilesSortedByLastModifiedDescending(): DataSource.Factory<Int, FileEntry>

    @Transaction
    fun upsertAndDeleteInvalidEntries(metadataDao: FileMetadataDao, fileEntries: List<FileEntry>) {
        markForSync()

        val insertResults = insert(fileEntries)
        val entriesToUpdate = insertResults.mapIndexedNotNull { index, result ->
            if (result < 0) fileEntries[index] else null
        }

        update(entriesToUpdate)
        deleteInvalidEntries()

        metadataDao.deleteAll()
        metadataDao.insertAll(fileEntries.map {
            val uri = Uri.parse(it.url)
            val name = uri.lastPathSegment!!
            return@map FileMetadata(it.fileId, uri.path!!.removeSuffix(name), name)
        })
    }
}