package com.epic.aiexpensevoice.presentation.screen.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.repository.ExpenseRepository
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.presentation.state.ExpensesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        expenseRepository.observeExpenses()
            .onEach { expenses ->
                _uiState.value = _uiState.value.copy(
                    expenses = expenses,
                    isLoading = false,
                )
            }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = expenseRepository.refreshExpenses()) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                )
                Resource.Loading -> Unit
            }
        }
    }

    fun updateExpense(expense: ExpenseItem, amount: Double) {
        viewModelScope.launch {
            when (val result = expenseRepository.updateExpense(expense, amount)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    infoMessage = "Updated ${expense.title}",
                    errorMessage = null,
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    errorMessage = result.message,
                    infoMessage = null,
                )
                Resource.Loading -> Unit
            }
        }
    }

    fun deleteExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            when (val result = expenseRepository.deleteExpense(expense)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    infoMessage = "Deleted ${expense.title}",
                    errorMessage = null,
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    errorMessage = result.message,
                    infoMessage = null,
                )
                Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
