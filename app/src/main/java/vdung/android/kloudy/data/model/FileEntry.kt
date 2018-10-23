package vdung.android.kloudy.data.model

import androidx.paging.DataSource
import androidx.room.*
import io.reactivex.Flowable
import java.util.*

@Entity(tableName = "fileEntry")
data class FileEntry(
        @PrimaryKey val fileId: Int,
        val url: String,
        val contentType: String,
        val name: String?,
        val lastModified: Date,
        val isSyncing: Boolean = false
)

data class FileEntryWithDate(
        @Embedded val entry: FileEntry,
        val year: String
)

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

    @Query("SELECT *, strftime('%Y-%m-%d', datetime(lastModified/1000, 'unixepoch')) AS year FROM fileEntry")
    fun allFiles(): Flowable<List<FileEntryWithDate>>

    @Query("SELECT month, count(*) as count FROM (SELECT strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') AS month FROM fileEntry) GROUP BY month ORDER BY month DESC")
    fun groupFilesByMonth(): List<GroupedByMonth>

    @Query("SELECT * FROM fileEntry ORDER BY lastModified DESC")
    fun filesSortedDescendingByLastModified(): DataSource.Factory<Int, FileEntry>

    @Transaction
    fun upsertAndDeleteInvalidEntries(fileEntries: List<FileEntry>) {
        markForSync()

        val insertResults = insert(fileEntries)
        val entriesToUpdate = insertResults.mapIndexedNotNull { index, result ->
            if (result < 0) fileEntries[index] else null
        }

        update(entriesToUpdate)
        deleteInvalidEntries()
    }
}