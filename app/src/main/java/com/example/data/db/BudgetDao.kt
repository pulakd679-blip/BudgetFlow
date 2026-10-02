package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM category_budgets")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<CategoryBudget>)

    @Update
    suspend fun updateBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM category_budgets")
    suspend fun getCount(): Int
}
