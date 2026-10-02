package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryDefinition(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isIncome: Boolean = false
)

object CategoryRegistry {
    val expenseCategories = listOf(
        CategoryDefinition("Groceries", Icons.Default.LocalGroceryStore, Color(0xFF10B981)),
        CategoryDefinition("Dining Out", Icons.Default.Fastfood, Color(0xFFF97316)),
        CategoryDefinition("Shopping", Icons.Default.ShoppingBag, Color(0xFFEC4899)),
        CategoryDefinition("Transport", Icons.Default.DirectionsCar, Color(0xFF06B6D4)),
        CategoryDefinition("Housing & Rent", Icons.Default.Home, Color(0xFF6366F1)),
        CategoryDefinition("Utilities & Bills", Icons.Default.AccountBalance, Color(0xFF8B5CF6)),
        CategoryDefinition("Entertainment", Icons.Default.Movie, Color(0xFFA855F7)),
        CategoryDefinition("Healthcare", Icons.Default.MedicalServices, Color(0xFFEF4444)),
        CategoryDefinition("Fitness", Icons.Default.FitnessCenter, Color(0xFF14B8A6)),
        CategoryDefinition("Education", Icons.Default.School, Color(0xFF3B82F6)),
        CategoryDefinition("Gifts & Donations", Icons.Default.CardGiftcard, Color(0xFFF43F5E)),
        CategoryDefinition("Other Expense", Icons.Default.LocalAtm, Color(0xFF64748B))
    )

    val incomeCategories = listOf(
        CategoryDefinition("Salary", Icons.Default.Work, Color(0xFF10B981), isIncome = true),
        CategoryDefinition("Freelance", Icons.Default.Payments, Color(0xFF059669), isIncome = true),
        CategoryDefinition("Investments", Icons.Default.TrendingUp, Color(0xFF0D9488), isIncome = true),
        CategoryDefinition("Bonus", Icons.Default.CardGiftcard, Color(0xFF34D399), isIncome = true),
        CategoryDefinition("Other Income", Icons.Default.AccountBalance, Color(0xFF22C55E), isIncome = true)
    )

    fun getCategory(name: String): CategoryDefinition {
        return (expenseCategories + incomeCategories).firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: CategoryDefinition(name, Icons.Default.LocalAtm, Color(0xFF64748B))
    }
}
