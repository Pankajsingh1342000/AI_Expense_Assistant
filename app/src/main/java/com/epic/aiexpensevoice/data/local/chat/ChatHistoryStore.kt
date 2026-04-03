package com.epic.aiexpensevoice.data.local.chat

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.epic.aiexpensevoice.domain.model.AgentContentBlock
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.ChatMessageUiModel
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.Sender
import com.epic.aiexpensevoice.domain.model.TrendPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.chatDataStore by preferencesDataStore(name = "chat_history")

class ChatHistoryStore(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    private val messagesKey = stringPreferencesKey("messages_json")

    fun observeMessages(): Flow<List<ChatMessageUiModel>> = context.chatDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[messagesKey].orEmpty()
            if (json.isBlank()) emptyList() else {
                runCatching {
                    val type = object : TypeToken<List<ChatMessageSnapshot>>() {}.type
                    gson.fromJson<List<ChatMessageSnapshot>>(json, type).map(ChatMessageSnapshot::toDomain)
                }.getOrDefault(emptyList())
            }
        }

    suspend fun saveMessages(messages: List<ChatMessageUiModel>) {
        context.chatDataStore.edit { preferences ->
            preferences[messagesKey] = gson.toJson(messages.map(ChatMessageUiModel::toSnapshot))
        }
    }

    suspend fun clear() {
        context.chatDataStore.edit { preferences ->
            preferences.remove(messagesKey)
        }
    }
}

private data class ChatMessageSnapshot(
    val id: String,
    val sender: String,
    val text: String,
    val timestampLabel: String,
    val blocks: List<BlockSnapshot>,
)

private data class BlockSnapshot(
    val type: String,
    val title: String? = null,
    val detail: String? = null,
    val value: String? = null,
    val amount: Double? = null,
    val subtitle: String? = null,
    val options: List<String> = emptyList(),
    val expenses: List<ExpenseSnapshot> = emptyList(),
    val budgets: List<BudgetSnapshot> = emptyList(),
    val categories: List<CategorySnapshot> = emptyList(),
    val points: List<TrendPointSnapshot> = emptyList(),
)

private data class ExpenseSnapshot(
    val id: Int?,
    val title: String,
    val amount: Double,
    val category: String,
    val dateLabel: String,
    val description: String?,
)

private data class BudgetSnapshot(
    val category: String,
    val spent: Double,
    val limit: Double,
    val statusLabel: String,
    val progress: Float,
    val tone: String,
)

private data class CategorySnapshot(
    val category: String,
    val amount: Double,
    val share: Float,
    val colorArgb: Int,
)

private data class TrendPointSnapshot(
    val label: String,
    val amount: Double,
)

private fun ChatMessageUiModel.toSnapshot(): ChatMessageSnapshot = ChatMessageSnapshot(
    id = id,
    sender = sender.name,
    text = text,
    timestampLabel = timestampLabel,
    blocks = blocks.map { block ->
        when (block) {
            is AgentContentBlock.PlainText -> BlockSnapshot(type = "plain_text", value = block.value)
            is AgentContentBlock.Success -> BlockSnapshot(type = "success", title = block.title, detail = block.detail)
            is AgentContentBlock.Warning -> BlockSnapshot(type = "warning", title = block.title, detail = block.detail)
            is AgentContentBlock.Insight -> BlockSnapshot(type = "insight", title = block.title, detail = block.detail)
            is AgentContentBlock.Clarification -> BlockSnapshot(type = "clarification", title = block.title, options = block.options)
            is AgentContentBlock.TotalsSummary -> BlockSnapshot(type = "totals", title = block.title, amount = block.amount, subtitle = block.subtitle)
            is AgentContentBlock.ExpenseList -> BlockSnapshot(type = "expenses", title = block.title, expenses = block.expenses.map(ExpenseItem::toSnapshot))
            is AgentContentBlock.BudgetOverview -> BlockSnapshot(type = "budgets", title = block.title, budgets = block.budgets.map(BudgetStatus::toSnapshot))
            is AgentContentBlock.CategoryBreakdown -> BlockSnapshot(type = "categories", title = block.title, categories = block.items.map(CategorySpend::toSnapshot))
            is AgentContentBlock.TrendChart -> BlockSnapshot(type = "trend", title = block.title, points = block.points.map(TrendPoint::toSnapshot))
        }
    },
)

private fun ChatMessageSnapshot.toDomain(): ChatMessageUiModel = ChatMessageUiModel(
    id = id,
    sender = runCatching { Sender.valueOf(sender) }.getOrDefault(Sender.Assistant),
    text = text,
    timestampLabel = timestampLabel,
    blocks = blocks.mapNotNull(BlockSnapshot::toDomain),
)

private fun BlockSnapshot.toDomain(): AgentContentBlock? = when (type) {
    "plain_text" -> value?.let(AgentContentBlock::PlainText)
    "success" -> AgentContentBlock.Success(title.orEmpty(), detail.orEmpty())
    "warning" -> AgentContentBlock.Warning(title.orEmpty(), detail.orEmpty())
    "insight" -> AgentContentBlock.Insight(title.orEmpty(), detail.orEmpty())
    "clarification" -> AgentContentBlock.Clarification(title.orEmpty(), options)
    "totals" -> AgentContentBlock.TotalsSummary(title.orEmpty(), amount ?: 0.0, subtitle.orEmpty())
    "expenses" -> AgentContentBlock.ExpenseList(title.orEmpty(), expenses.map(ExpenseSnapshot::toDomain))
    "budgets" -> AgentContentBlock.BudgetOverview(title.orEmpty(), budgets.map(BudgetSnapshot::toDomain))
    "categories" -> AgentContentBlock.CategoryBreakdown(title.orEmpty(), categories.map(CategorySnapshot::toDomain))
    "trend" -> AgentContentBlock.TrendChart(title.orEmpty(), points.map(TrendPointSnapshot::toDomain))
    else -> null
}

private fun ExpenseItem.toSnapshot(): ExpenseSnapshot = ExpenseSnapshot(id, title, amount, category, dateLabel, description)
private fun ExpenseSnapshot.toDomain(): ExpenseItem = ExpenseItem(id = id, title = title, amount = amount, category = category, dateLabel = dateLabel, description = description)

private fun BudgetStatus.toSnapshot(): BudgetSnapshot = BudgetSnapshot(category, spent, limit, statusLabel, progress, tone.name)
private fun BudgetSnapshot.toDomain(): BudgetStatus = BudgetStatus(category, spent, limit, statusLabel, progress, runCatching { BudgetTone.valueOf(tone) }.getOrDefault(BudgetTone.Healthy))

private fun CategorySpend.toSnapshot(): CategorySnapshot = CategorySnapshot(category, amount, share, color.toArgb())
private fun CategorySnapshot.toDomain(): CategorySpend = CategorySpend(category, amount, share, Color(colorArgb))

private fun TrendPoint.toSnapshot(): TrendPointSnapshot = TrendPointSnapshot(label, amount)
private fun TrendPointSnapshot.toDomain(): TrendPoint = TrendPoint(label, amount)
