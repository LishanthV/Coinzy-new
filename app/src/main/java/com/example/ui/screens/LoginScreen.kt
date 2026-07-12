package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Boolean,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    // Background floating circle animations
    val infiniteTransition = rememberInfiniteTransition(label = "auth_infinite")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin_float"
    )
    val spinAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_spin"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Premium Custom Canvas Illustration: Bouncing Gold Coin and Floating Purple Wallet
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = floatAnim.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Background soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(BentoLavender.copy(alpha = 0.5f), Color.Transparent),
                            center = center,
                            radius = w * 0.5f
                        ),
                        radius = w * 0.45f
                    )

                    // Draw abstract Coinzy Wallet Shield
                    val walletPath = Path().apply {
                        moveTo(w * 0.3f, h * 0.4f)
                        quadraticBezierTo(w * 0.5f, h * 0.3f, w * 0.7f, h * 0.4f)
                        lineTo(w * 0.75f, h * 0.65f)
                        quadraticBezierTo(w * 0.5f, h * 0.85f, w * 0.25f, h * 0.65f)
                        close()
                    }
                    drawPath(
                        path = walletPath,
                        brush = Brush.linearGradient(
                            colors = listOf(BentoAccent, BentoDeepViolet)
                        )
                    )

                    // Draw golden coin floating overhead
                    drawCircle(
                        color = Color(0xFFFFC107), // Gold
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.3f),
                        radius = w * 0.12f
                    )
                    drawCircle(
                        color = Color(0xFFFFE082), // Light Gold inner
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.3f),
                        radius = w * 0.08f
                    )
                    // Inner "C" symbol
                    drawArc(
                        color = Color(0xFFFF8F00), // Dark Gold C
                        startAngle = 45f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.1f, w * 0.1f),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Secondary elements (stars/sparks)
                    drawCircle(
                        color = BentoPink,
                        center = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.35f),
                        radius = w * 0.03f
                    )
                    drawCircle(
                        color = BentoBlue,
                        center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.5f),
                        radius = w * 0.04f
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Name & Header
            Text(
                text = "Coinzy",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = BentoDeepViolet,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Smart Bento-style Personal Finance",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Credentials Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = BentoAccent.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Access Wallet",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = BentoDeepViolet
                    )
                    Text(
                        text = "Enter your security password to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                        textAlign = TextAlign.Center
                    )

                    // Error Message Banner with entry animation
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                border = BorderStroke(1.dp, Color(0xFFFFE4E6))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        color = Color(0xFF9F1239),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    // Password / Passcode Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = BentoAccent
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (password.trim().isEmpty()) {
                                    errorMessage = "Please enter your password"
                                } else {
                                    val success = onLoginSuccess(password)
                                    if (!success) {
                                        errorMessage = "Invalid password. Try again!"
                                    }
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = BentoBg.copy(alpha = 0.3f),
                            unfocusedContainerColor = BentoBg.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Login Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (password.trim().isEmpty()) {
                                errorMessage = "Please enter your password"
                            } else {
                                val success = onLoginSuccess(password)
                                if (!success) {
                                    errorMessage = "Invalid password. Try again!"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Unlock Wallet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick bypass for testing / easy demonstration
                    TextButton(
                        onClick = {
                            password = "admin"
                            errorMessage = null
                        },
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        Text(
                            text = "Bypass Password? Type: admin",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = BentoAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation redirect to register
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New to Coinzy?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create an account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = BentoAccent
                    ),
                    modifier = Modifier
                        .clickable(onClick = onNavigateToRegister)
                        .testTag("goto_register_link")
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
