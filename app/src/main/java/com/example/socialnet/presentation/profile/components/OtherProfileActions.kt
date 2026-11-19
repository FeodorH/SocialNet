package com.example.socialnet.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.socialnet.R

@Composable
fun OtherProfileActions(
    isSubscribed: Boolean,
    isLoading: Boolean,
    onToggleSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        } else if (isSubscribed) {
            OutlinedButton(onClick = onToggleSubscription) {
                Text(stringResource(R.string.unsubscribe))
            }
        } else {
            Button(onClick = onToggleSubscription) {
                Text(stringResource(R.string.subscribe))
            }
        }
    }
}