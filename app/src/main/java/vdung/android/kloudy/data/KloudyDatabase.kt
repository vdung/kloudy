package vdung.android.kloudy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.model.FileEntryDao
import vdung.android.kloudy.data.model.User
import vdung.android.kloudy.data.model.UserDao
import java.util.*


@Database(entities = [User::class, FileEntry::class], version = 1)
@TypeConverters(Converters::class)
abstract class KloudyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun fileDao(): FileEntryDao
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}