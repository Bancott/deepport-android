package io.github.bancott.deepport.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
    val pinned: Boolean,
    val pinnedAt: Long,
    val titleEdited: Boolean,
    /** 父节点 id → 选中的子节点 id，JSON 串 */
    val activeChildJson: String,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    /** 会话内创建顺序（分支顺序靠它保持） */
    val orderIndex: Int,
    val parentId: String?,
    val role: String,
    val text: String,
    val reasoning: String,
    val status: String,
    val finishReason: String?,
    val reasoningSeconds: Int,
    val reasoningExpanded: Boolean,
    val usagePrompt: Int?,
    val usageCompletion: Int?,
    val usageReasoning: Int?,
    /** 附件 / 来源是变长结构，落成 JSON 列 */
    val attachmentsJson: String?,
    val sourcesJson: String?,
    val errorHint: String?,
)

@Dao
interface ChatDao {

    @Query("SELECT * FROM conversations ORDER BY pinned DESC, updatedAt DESC")
    fun loadConversations(): List<ConversationEntity>

    @Query("SELECT * FROM messages ORDER BY conversationId, orderIndex")
    fun loadMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConversations(items: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(items: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    fun deleteMessages(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    fun deleteConversation(id: String)

    @Query("SELECT COUNT(*) FROM conversations")
    fun count(): Int

    /** 整条会话一次事务写：会话元数据 + 全量消息（分支树整体替换） */
    @Transaction
    fun replaceConversation(conversation: ConversationEntity, messages: List<MessageEntity>) {
        insertConversations(listOf(conversation))
        deleteMessages(conversation.id)
        insertMessages(messages)
    }
}

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: ChatDatabase? = null

        fun get(context: Context): ChatDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "ds_chat.db",
            ).build().also { instance = it }
        }
    }
}
