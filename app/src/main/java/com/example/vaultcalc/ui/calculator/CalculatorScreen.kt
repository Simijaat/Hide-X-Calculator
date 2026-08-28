package com.example.vaultcalc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CalculatorScreen(
    onNavigateToVault: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Listen for vault access securely using LaunchedEffect
    LaunchedEffect(state.navigateToVault) {
        if (state.navigateToVault) {
            viewModel.onVaultNavigated()
            onNavigateToVault()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display area
        Text(
            text = state.displayValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Light,
            fontSize = 80.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        // Buttons
        val buttons = listOf(
            listOf("C", "(", ")", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", "DEL")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { symbol ->
                    if (symbol.isNotEmpty()) {
                        CalculatorButton(
                            symbol = symbol,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { viewModel.onAction(CalculatorAction.ButtonPress(symbol)) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isAction = symbol in listOf("C", "(", ")", "/", "*", "-", "+", "=", "DEL")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isAction) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
    ) {
        Text(
            text = symbol,
            fontSize = 32.sp,
            color = if (isAction) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
