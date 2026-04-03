package com.epic.aiexpensevoice.data.remote.network

import com.epic.aiexpensevoice.data.remote.dto.AgentQueryDto
import com.epic.aiexpensevoice.data.remote.dto.CategoryTotalDto
import com.epic.aiexpensevoice.data.remote.dto.CreateBudgetRequestDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetStatusDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetWarningsResponseDto
import com.epic.aiexpensevoice.data.remote.dto.CategoryBreakdownDto
import com.epic.aiexpensevoice.data.remote.dto.CreateExpenseRequestDto
import com.epic.aiexpensevoice.data.remote.dto.DailySpendingDto
import com.epic.aiexpensevoice.data.remote.dto.DeleteExpenseResponseDto
import com.epic.aiexpensevoice.data.remote.dto.ExpenseDto
import com.epic.aiexpensevoice.data.remote.dto.ExpenseTotalDto
import com.epic.aiexpensevoice.data.remote.dto.InsightsDto
import com.epic.aiexpensevoice.data.remote.dto.MonthlySummaryDto
import com.epic.aiexpensevoice.data.remote.dto.AuthResponseDto
import com.epic.aiexpensevoice.data.remote.dto.RegisterRequestDto
import com.epic.aiexpensevoice.data.remote.dto.SpendingTrendDto
import com.epic.aiexpensevoice.data.remote.dto.TopCategoryDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateBudgetRequestDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateExpenseRequestDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateExpenseResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<Unit>

    @FormUrlEncoded
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String,
    ): Response<AuthResponseDto>

    @POST("/api/v1/expenses/agent")
    suspend fun sendAgentQuery(
        @Body request: AgentQueryDto,
    ): Response<ResponseBody>

    @GET("/api/v1/expenses")
    suspend fun getExpenses(
        @Query("category") category: String? = null,
        @Query("min_amount") minAmount: Double? = null,
        @Query("max_amount") maxAmount: Double? = null,
    ): Response<List<ExpenseDto>>

    @POST("/api/v1/expenses")
    suspend fun createExpense(
        @Body request: CreateExpenseRequestDto,
    ): Response<ExpenseDto>

    @PUT("/api/v1/expenses/{expenseId}")
    suspend fun updateExpense(
        @Path("expenseId") expenseId: Int,
        @Body request: UpdateExpenseRequestDto,
    ): Response<UpdateExpenseResponseDto>

    @DELETE("/api/v1/expenses/{expenseId}")
    suspend fun deleteExpense(
        @Path("expenseId") expenseId: Int,
    ): Response<DeleteExpenseResponseDto>

    @GET("/api/v1/expenses/summary/total")
    suspend fun getExpenseTotal(): Response<ExpenseTotalDto>

    @GET("/api/v1/budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>

    @POST("/api/v1/budgets")
    suspend fun createBudget(
        @Body request: CreateBudgetRequestDto,
    ): Response<BudgetDto>

    @GET("/api/v1/budgets/status/warnings")
    suspend fun getBudgetWarnings(): Response<BudgetWarningsResponseDto>

    @GET("/api/v1/budgets/{category}")
    suspend fun getBudgetStatus(
        @Path("category") category: String,
    ): Response<BudgetStatusDto>

    @PUT("/api/v1/budgets/{category}")
    suspend fun updateBudget(
        @Path("category") category: String,
        @Body request: UpdateBudgetRequestDto,
    ): Response<BudgetDto>

    @DELETE("/api/v1/budgets/{category}")
    suspend fun deleteBudget(
        @Path("category") category: String,
    ): Response<Unit>

    @GET("/api/v1/analytics/category-breakdown")
    suspend fun getCategoryBreakdown(): Response<List<CategoryBreakdownDto>>

    @GET("/api/v1/analytics/category-total/{category}")
    suspend fun getCategoryTotal(
        @Path("category") category: String,
    ): Response<CategoryTotalDto>

    @GET("/api/v1/analytics/monthly-summary")
    suspend fun getMonthlySummary(): Response<MonthlySummaryDto>

    @GET("/api/v1/analytics/daily-spending")
    suspend fun getDailySpending(): Response<DailySpendingDto>

    @GET("/api/v1/analytics/top-category")
    suspend fun getTopCategory(): Response<TopCategoryDto>

    @GET("/api/v1/analytics/spending-trend")
    suspend fun getSpendingTrend(
        @Query("days") days: Int = 7,
    ): Response<List<SpendingTrendDto>>

    @GET("/api/v1/analytics/insights")
    suspend fun getInsights(): Response<InsightsDto>
}
