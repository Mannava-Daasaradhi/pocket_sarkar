package com.pocketsarkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenefitsScreen(
    userLanguage: String,
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        Category("Agriculture", "Farmers, Subsidies, Seeds", Icons.Default.Agriculture, Color(0xFF4CAF50)),
        Category("Housing", "Home Loans, Urban, Rural", Icons.Default.Home, Color(0xFF2196F3)),
        Category("Health", "Insurance, Hospitals, Medicines", Icons.Default.Favorite, Color(0xFFE91E63)),
        Category("Education", "Scholarships, Books, Uniforms", Icons.Default.School, Color(0xFFFF9800))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Available Schemes", fontWeight = FontWeight.Bold, color = PSNavy) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PSCream)
            )
        },
        containerColor = PSCream
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Select a sector to view top schemes and check your eligibility with AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PSTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(categories) { cat ->
                CategoryCard(cat) { onCategoryClick(cat.name) }
            }
        }
    }
}

private data class Category(val name: String, val sub: String, val icon: ImageVector, val color: Color)

@Composable
private fun CategoryCard(cat: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(cat.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PSNavy)
                Text(cat.sub, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = PSNavy)
        }
    }
}
