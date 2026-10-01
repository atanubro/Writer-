package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FactCheckDiscrepancy
import com.example.data.model.FactCheckResult
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.DiscrepancyRed
import com.example.ui.theme.DiscrepancyRedContainer
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBlueContainer
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenContainer

@Composable
fun FactualInconsistenciesWidget(
    factCheckResult: FactCheckResult?,
    isChecking: Boolean,
    onRunFactCheck: () -> Unit,
    onApplyFix: ((sourceValue: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var filterOnlyDiscrepancies by remember { mutableStateOf(false) }

    val discrepancies = factCheckResult?.discrepancies ?: emptyList()
    val mismatchCount = discrepancies.count { !it.isVerified }
    val isAllConsistent = factCheckResult != null && (factCheckResult.isConsistent || mismatchCount == 0)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (mismatchCount > 0) DiscrepancyRed.copy(alpha = 0.5f) else Slate200, RoundedCornerShape(12.dp))
            .testTag("factual_inconsistencies_widget")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                if (mismatchCount > 0) DiscrepancyRedContainer else VerifiedGreenContainer,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = "Fact Check",
                            tint = if (mismatchCount > 0) DiscrepancyRed else VerifiedGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FACTUAL INCONSISTENCIES & VERIFICATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = if (mismatchCount > 0) DiscrepancyRed else NavyBluePrimary
                        )
                        Text(
                            text = if (factCheckResult == null) "Not verified yet" else "Score: ${factCheckResult.scorePercent}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Status Badge
                if (factCheckResult != null) {
                    Surface(
                        color = if (mismatchCount > 0) DiscrepancyRedContainer else VerifiedGreenContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (mismatchCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (mismatchCount > 0) DiscrepancyRed else VerifiedGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (mismatchCount > 0) "$mismatchCount MISMATCHES" else "100% VERIFIED",
                                color = if (mismatchCount > 0) DiscrepancyRed else VerifiedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Content
            if (isChecking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying dates, reference numbers, and amounts vs source...", fontSize = 12.sp)
                }
            } else if (factCheckResult == null) {
                // Empty state before checking
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cross-check the generated reply against source letter to detect mismatched dates, reference numbers, quantities, or financial figures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRunFactCheck,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary),
                        modifier = Modifier.testTag("widget_run_fact_check_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Facts Now", fontSize = 12.sp)
                    }
                }
            } else if (isAllConsistent) {
                // Verified state
                Surface(
                    color = VerifiedGreenContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = VerifiedGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All referenced dates, letter numbers, deadlines, and amounts match the incoming letter accurately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VerifiedGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onRunFactCheck,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-check", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-verify", fontSize = 10.sp)
                    }
                }
            } else {
                // Inconsistencies / Discrepancies List
                Text(
                    text = "Mismatched parameters detected comparing Source vs. Reply:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Navy800
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val displayedList = if (filterOnlyDiscrepancies) discrepancies.filter { !it.isVerified } else discrepancies

                    displayedList.forEach { disc ->
                        DiscrepancyWarningItem(
                            item = disc,
                            onApplyFix = onApplyFix
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filterOnlyDiscrepancies) "Showing mismatches only" else "Showing all ${discrepancies.size} checks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedButton(
                        onClick = onRunFactCheck,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("recheck_facts_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recheck", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-check Facts", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscrepancyWarningItem(
    item: FactCheckDiscrepancy,
    onApplyFix: ((sourceValue: String) -> Unit)?
) {
    val categoryStyle = getCategoryStyle(item.category, item.isVerified)

    Surface(
        color = categoryStyle.backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, categoryStyle.borderColor, RoundedCornerShape(8.dp))
            .testTag("discrepancy_item_${item.category.replace(" ", "_").lowercase()}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Category Badge & Verification Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryStyle.icon,
                        contentDescription = item.category,
                        tint = categoryStyle.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = categoryStyle.accentColor
                    )
                }

                Surface(
                    color = if (item.isVerified) VerifiedGreenContainer else DiscrepancyRedContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (item.isVerified) "MATCH" else "MISMATCH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isVerified) VerifiedGreen else DiscrepancyRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Comparison: Source vs Reply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "SOURCE LETTER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy700
                    )
                    Text(
                        text = item.sourceValue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy800
                    )
                }

                // Reply Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (item.isVerified) Color.White.copy(alpha = 0.8f) else DiscrepancyRedContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (item.isVerified) Color.Transparent else DiscrepancyRed.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(6.dp)
                ) {
                    Text(
                        text = "REPLY DRAFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isVerified) Navy700 else DiscrepancyRed
                    )
                    Text(
                        text = item.replyValue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isVerified) Navy800 else DiscrepancyRed
                    )
                }
            }

            if (item.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.remarks,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Fix Button if Mismatched
            if (!item.isVerified && onApplyFix != null && item.sourceValue.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { onApplyFix(item.sourceValue) },
                        color = categoryStyle.accentColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("apply_fix_${item.category.replace(" ", "_").lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Use Source Value",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CategoryStyle(
    val backgroundColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val icon: ImageVector
)

private fun getCategoryStyle(category: String, isVerified: Boolean): CategoryStyle {
    val cat = category.lowercase()
    return when {
        cat.contains("date") -> CategoryStyle(
            backgroundColor = Color(0xFFFFFBEB), // Light Amber
            borderColor = Color(0xFFFDE68A),
            accentColor = Color(0xFFB45309), // Amber 700
            icon = Icons.Default.DateRange
        )
        cat.contains("reference") || cat.contains("letter no") -> CategoryStyle(
            backgroundColor = Color(0xFFF5F3FF), // Light Violet
            borderColor = Color(0xFFDDD6FE),
            accentColor = Color(0xFF6D28D9), // Violet 700
            icon = Icons.Default.Numbers
        )
        cat.contains("amount") || cat.contains("financial") || cat.contains("price") -> CategoryStyle(
            backgroundColor = Color(0xFFFEF2F2), // Light Red
            borderColor = Color(0xFFFECACA),
            accentColor = Color(0xFFB91C1C), // Red 700
            icon = Icons.Default.AttachMoney
        )
        cat.contains("quantity") || cat.contains("units") -> CategoryStyle(
            backgroundColor = Color(0xFFEFF6FF), // Light Blue
            borderColor = Color(0xFFBFDBFE),
            accentColor = Color(0xFF1D4ED8), // Blue 700
            icon = Icons.Default.FormatListNumbered
        )
        cat.contains("deadline") -> CategoryStyle(
            backgroundColor = Color(0xFFFFF7ED), // Light Orange
            borderColor = Color(0xFFFED7AA),
            accentColor = Color(0xFFC2410C), // Orange 700
            icon = Icons.Default.AccessTime
        )
        else -> CategoryStyle(
            backgroundColor = if (isVerified) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
            borderColor = if (isVerified) Color(0xFFBBF7D0) else Color(0xFFFECACA),
            accentColor = if (isVerified) VerifiedGreen else DiscrepancyRed,
            icon = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning
        )
    }
}
