package com.neeva.app.ui.widgets.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.Dimensions

@Composable
fun MenuRow(data: MenuRowData, onClick: (id: Int) -> Unit) {
    val label = data.primaryLabel()
    val iconPainter = data.icon()

    when (data.type) {
        MenuItemType.HEADER -> {
            val secondaryLabel = data.secondaryLabel()
            assert(label != null || secondaryLabel != null)

            Column(modifier = Modifier.padding(Dimensions.PADDING_SMALL)) {
                label?.let {
                    Text(
                        text = label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                secondaryLabel?.let {
                    Text(
                        text = secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium

                    )
                }
            }
        }

        MenuItemType.SEPARATOR -> {
            Spacer(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }

        MenuItemType.ACTION -> {
            DropdownMenuItem(
                text = {
                    Text(
                        modifier = Modifier.padding(horizontal = Dimensions.PADDING_SMALL),
                        text = label!!,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                },
                leadingIcon = iconPainter?.let {
                    {
                        Icon(
                            painter = it,
                            contentDescription = label
                        )
                    }
                },
                onClick = { onClick(data.id!!) }
            )
        }
    }
}
