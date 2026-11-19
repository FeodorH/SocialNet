package com.example.socialnet.presentation.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.socialnet.R

@Composable
fun OwnProfileActions(
    isUpdating: Boolean,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(onClick = onEditClick, enabled = !isUpdating) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.edit_button))
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onLogoutClick, enabled = !isUpdating) {
            Text(stringResource(R.string.logout_short), color = MaterialTheme.colorScheme.error)
        }
    }
}