package com.mayoigacraft.phyrexia

/**
 * 認識されたテキストブロック（複数行）
 */
data class TextBlock (
    val text: String,
    val textLines: List<TextLine>,
    val percentRect: PercentRect
)
