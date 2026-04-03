package com.epic.aiexpensevoice.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.presentation.components.GradientScreen

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AuthFormShell("Welcome back", "Pick up right where you left off.") {
        OutlinedTextField(value = state.email, onValueChange = viewModel::updateEmail, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.login(onLoginSuccess) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
            Text(if (state.isLoading) "Signing in..." else "Sign in")
        }
        TextButton(onClick = onNavigateRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Create a new account")
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
    AuthFormShell("Create your account", "Start tracking spending with a calmer, more conversational flow.") {
        OutlinedTextField(value = state.email, onValueChange = viewModel::updateEmail, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::updateConfirmPassword,
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.register(onRegisterSuccess) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
            Text(if (state.isLoading) "Creating account..." else "Create account")
        }
        TextButton(onClick = onNavigateLogin, modifier = Modifier.fillMaxWidth()) {
            Text("I already have an account")
        }
    }
}

@Composable
private fun AuthFormShell(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    GradientScreen {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
    }
}
