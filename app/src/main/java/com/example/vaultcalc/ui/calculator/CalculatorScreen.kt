package com.example.vaultcalc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vaultcalc.ui.theme.*

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
            .background(AppBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Setup Hint
        if (!state.isPinSet) {
            val hintText = if (state.isConfirmingPin) {
                "Confirm your 4+ digit PIN and press ="
            } else {
                "Set a 4+ digit PIN and press ="
            }
            Text(
                text = hintText,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }

        // Display area
        Text(
            text = state.displayValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Light,
            fontSize = if (state.displayValue.length > 8) 60.sp else 90.sp,
            color = WhiteText,
            maxLines = 1
        )

        // Buttons
        val buttons = listOf(
            listOf(if (state.displayValue == "0") "AC" else "C", "+/-", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "DEL", "=")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { symbol ->
                    val weight = if (symbol == "0" && !row.contains("DEL")) 2.1f else 1f
                    CalculatorButton(
                        symbol = symbol,
                        modifier = Modifier
                            .weight(weight)
                            .aspectRatio(if (weight > 1f) 2.1f else 1f),
                        onClick = { viewModel.onAction(CalculatorAction.ButtonPress(symbol)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val topRow = listOf("AC", "C", "+/-", "%")
    val rightCol = listOf("÷", "×", "−", "+", "=")

    val bgColor = when {
        symbol in topRow -> TopRowGray
        symbol in rightCol -> PrimaryOrange
        else -> NumPadGray
    }

    val textColor = when {
        symbol in topRow -> BlackText
        else -> WhiteText
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(brush = Brush.verticalGradient(colors = listOf(bgColor.copy(alpha = 0.7f), bgColor)))
            .clickable { onClick() }
    ) {
        val textSize = if (symbol.length > 2) 24.sp else 36.sp
        Text(
            text = symbol,
            fontSize = textSize,
            color = textColor,
            fontWeight = FontWeight.Normal
        )
    }
}
