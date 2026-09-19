package com.campussync.app.core.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.campussync.app.core.utils.ClassFormatter
import androidx.compose.ui.text.TextStyle

@Composable
fun ClassNameText(
    collegeId: String,
    classId: String,
    modifier: Modifier = Modifier,
    prefix: String = "",
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    var displayName by remember(classId) { mutableStateOf(if (classId.contains("-") && classId.length < 20) classId else "Loading...") }
    
    LaunchedEffect(classId) {
        if ((!classId.contains("-") || classId.length >= 20) && classId.isNotBlank()) {
            displayName = ClassFormatter.getDisplayName(collegeId, classId)
        } else if (classId.isNotBlank()) {
            displayName = classId
        }
    }
    
    Text(
        text = "${prefix}${displayName.ifBlank { "Select a Class" }}",
        modifier = modifier,
        fontWeight = fontWeight,
        color = color,
        fontSize = fontSize,
        style = style
    )
}
