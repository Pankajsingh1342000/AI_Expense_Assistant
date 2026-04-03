package com.epic.aiexpensevoice.data.remote.dto

data class RegisterRequestDto(
    val email: String,
    val password: String,
)

data class AuthResponseDto(
    val access_token: String,
    val token_type: String,
)

data class AgentQueryDto(
    val query: String,
)
