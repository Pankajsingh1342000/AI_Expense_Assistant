package com.epic.aiexpensevoice.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.presentation.components.GradientPrimaryButton
import com.epic.aiexpensevoice.presentation.components.GradientScreen

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AuthFormShell(
        title = "AI Expense Voice",
        subtitle = "Your AI-powered expense assistant",
        footerText = "New here?",
        footerAction = "Sign Up",
        onFooterClick = onNavigateRegister,
    ) {
        SoftInputField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = "Email Address",
            placeholder = "name@example.com",
            icon = Icons.Outlined.MailOutline,
        )
        SoftInputField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = "Password",
            placeholder = "••••••••",
            icon = Icons.Outlined.Lock,
            isPassword = true,
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        GradientPrimaryButton(
            text = if (state.isLoading) "Signing In..." else "Sign In",
            onClick = { viewModel.login(onLoginSuccess) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        )
        Text(
            text = "OR LOGIN WITH",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            BiometricButton(Icons.Outlined.Fingerprint)
            Spacer(Modifier.padding(6.dp))
            BiometricButton(Icons.Outlined.Mic)
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AuthFormShell(
        title = "AI Expense Voice",
        subtitle = "Set up your AI-powered spending companion",
        footerText = "Already have an account?",
        footerAction = "Sign In",
        onFooterClick = onNavigateLogin,
    ) {
        SoftInputField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = "Email Address",
            placeholder = "name@example.com",
            icon = Icons.Outlined.MailOutline,
        )
        SoftInputField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = "Password",
            placeholder = "Create a password",
            icon = Icons.Outlined.Lock,
            isPassword = true,
        )
        SoftInputField(
            value = state.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = "Confirm Password",
            placeholder = "Repeat password",
            icon = Icons.Outlined.Lock,
            isPassword = true,
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        GradientPrimaryButton(
            text = if (state.isLoading) "Creating Account..." else "Create Account",
            onClick = { viewModel.register(onRegisterSuccess) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        )
    }
}

@Composable
private fun AuthFormShell(
    title: String,
    subtitle: String,
    footerText: String,
    footerAction: String,
    onFooterClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    GradientScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(
                subtitle,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Card(
                shape = RoundedCornerShape(34.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = footerText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onFooterClick) {
                Text(footerAction, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        )
    }
}

@Composable
private fun BiometricButton(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.padding(horizontal = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(18.dp),
        )
    }
}
