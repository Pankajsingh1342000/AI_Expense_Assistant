package com.epic.aiexpensevoice.data.repository

import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.remote.dto.ExpenseDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateExpenseRequestDto
import com.epic.aiexpensevoice.data.remote.network.ApiService
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import kotlinx.coroutines.flow.Flow
import java.io.IOException

class ExpenseRepository(
    private val apiService: ApiService,
    private val localExpenseStore: LocalExpenseStore,
) {
    fun observeExpenses(): Flow<List<ExpenseItem>> = localExpenseStore.observeExpenses()

    suspend fun refreshExpenses(): Resource<List<ExpenseItem>> = runCatching {
        val response = apiService.getExpenses()
        val body = response.body()
        if (!response.isSuccessful || body == null) error("Expenses request failed with ${response.code()}")
        val expenses = body.map { it.toDomain() }
        localExpenseStore.replaceSyncedExpenses(expenses)
        expenses
    }.fold(
        onSuccess = { Resource.Success(it) },
        onFailure = { error ->
            if (error is IOException) {
                Resource.Success(localExpenseStore.getExpenses())
            } else {
                Resource.Error(error.message ?: "Unable to load expenses.", error)
            }
        },
    )

    suspend fun updateExpense(expense: ExpenseItem, amount: Double): Resource<Unit> = runCatching {
        val expenseId = expense.id
        if (expenseId == null) {
            val updated = localExpenseStore.updateLocalExpenseAmount(null, expense.title, amount)
            if (updated == null) error("We couldn't update this expense right now.")
            return@runCatching
        }
        val response = apiService.updateExpense(
            expenseId = expenseId,
            request = UpdateExpenseRequestDto(amount = amount),
        )
        val body = response.body()
        if (!response.isSuccessful || body == null) error("Expense update failed with ${response.code()}")
        localExpenseStore.upsertSyncedExpense(body.expense.toDomain())
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { error ->
            if (error is IOException) {
                val updated = localExpenseStore.updateLocalExpenseAmount(expense.id, expense.title, amount)
                if (updated != null) Resource.Success(Unit) else Resource.Error("We couldn't update this expense while offline.", error)
            } else {
                Resource.Error(error.message ?: "Unable to update expense.", error)
            }
        },
    )

    suspend fun deleteExpense(expense: ExpenseItem): Resource<Unit> = runCatching {
        val expenseId = expense.id
        if (expenseId == null) {
            val deleted = localExpenseStore.deleteLocalExpense(null, expense.title)
            if (deleted == null) error("We couldn't remove this expense right now.")
            return@runCatching
        }
        val response = apiService.deleteExpense(expenseId)
        if (!response.isSuccessful) error(response.body()?.message ?: "Expense delete failed with ${response.code()}")
        localExpenseStore.removeSyncedExpense(expenseId)
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { error ->
            if (error is IOException) {
                val deleted = localExpenseStore.deleteLocalExpense(expense.id, expense.title)
                if (deleted != null) Resource.Success(Unit) else Resource.Error("We couldn't remove this expense while offline.", error)
            } else {
                Resource.Error(error.message ?: "Unable to delete expense.", error)
            }
        },
    )

    private fun ExpenseDto.toDomain(): ExpenseItem = ExpenseItem(
        id = id,
        title = title,
        amount = amount.toDoubleOrNull() ?: 0.0,
        category = category,
        dateLabel = date,
        description = description,
    )
}
