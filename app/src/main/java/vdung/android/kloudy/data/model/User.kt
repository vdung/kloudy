package vdung.android.kloudy.data.model

import androidx.room.*
import io.reactivex.Flowable

@Entity(tableName = "user")
data class User(
        @PrimaryKey(autoGenerate = true) val id: Int? = null,
        val username: String,
        val password: String,
        val server: String,
        val isValid: Boolean,
        val isCurrent: Boolean
)

@Dao
interface UserDao {
    @Insert
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Delete
    fun delete(user: User)

    @Query("SELECT * FROM user")
    fun getUsers(): Flowable<List<User>>

    @Query("SELECT * FROM user WHERE isCurrent = 1")
    fun getActiveUser(): User?

}