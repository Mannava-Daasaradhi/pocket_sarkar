package com.pocketsarkar.ai.mediapipe

enum class ChatRole { USER, ASSISTANT }

data class ChatTurn(val role: ChatRole, val content: String)