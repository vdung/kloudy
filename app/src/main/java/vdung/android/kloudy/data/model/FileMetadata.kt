package vdung.android.kloudy.data.model

import android.net.Uri
import androidx.paging.DataSource
import androidx.room.*
import java.util.*

@Entity
@Fts4
data class FileMetadata(
        @PrimaryKey @ColumnInfo(name = "rowid") val fileId: Int,
        val directory: String,
        val name: String
)

data class DirectoryQueryResult(
        val directory: String,
        val count: Int,
        @Embedded
        val fileEntry: FileEntry
) {
    @Ignore
    val name: String? = Uri.parse(directory).lastPathSegment
}

@Dao
interface FileMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(metadataList: List<FileMetadata>)

    @Query("DELETE FROM fileMetadata")
    fun deleteAll()

    @Query("""
        WITH files AS (
            SELECT * FROM fileEntry
            INNER JOIN fileMetadata ON fileEntry.fileId == fileMetadata.`rowid`
        )
        SELECT * FROM (
            SELECT directory as directory, count(*) as count, max(fileId) as lastId FROM files
            GROUP BY directory
            ORDER BY directory ASC
        ) as f
        INNER JOIN fileEntry on f.lastId == fileEntry.fileId
    """)
    fun getAllDirectories(): DataSource.Factory<Int, DirectoryQueryResult>

    @Query("""
        SELECT * FROM fileEntry
        INNER JOIN fileMetadata ON fileEntry.fileId == fileMetadata.`rowid`
        WHERE directory == :directory
    """)
    fun getFilesInDirectory(directory: String): DataSource.Factory<Int, FileEntry>

}