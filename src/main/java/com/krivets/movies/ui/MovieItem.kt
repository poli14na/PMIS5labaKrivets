package com.krivets.movies.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun MovieItem(
	modifier: Modifier = Modifier,
	name: String,
	imageUrl: String?
) {
	val context = LocalContext.current

	Row(
		modifier = modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		val imageBuilder = remember {
			ImageRequest.Builder(context).data(imageUrl).build()
		}

		Box(contentAlignment = Alignment.Center) {
			Icon(
				Icons.Default.Star,
				contentDescription = null
			)
			AsyncImage(
				model = imageBuilder,
				contentDescription = name,
				modifier = Modifier
					.size(64.dp)
					.clip(RoundedCornerShape(8.dp)),
				contentScale = ContentScale.Crop
			)
		}


		Spacer(modifier = Modifier.width(8.dp))

		Text(
			text = name,
			style = MaterialTheme.typography.labelLarge
		)
	}
}