package com.app.pegsolitaire.data.licenses

import android.content.Context
import com.app.pegsolitaire.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * [LicenseTextSource] over the APK's assets (tech design §3.6): the whole text, decoded as UTF-8,
 * read on the injected [io] dispatcher so the caller may be on the main thread.
 *
 * Only `IOException` is caught — a missing asset is a `FileNotFoundException`, one of them — and it
 * becomes `null`, which the screens show as their error state. Anything else is a defect and
 * propagates. The largest text it reads is Cronet's notice, 785,183 B; one `readText()` of it is the
 * whole cost, and the re-flow that follows runs on the ViewModel's Default dispatcher, not here.
 */
class AssetLicenseTextSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
    ) : LicenseTextSource {
        override suspend fun read(asset: LicenseAsset): String? =
            withContext(io) {
                try {
                    context.assets
                        .open(asset.assetPath)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                } catch (ignored: IOException) {
                    null
                }
            }
    }
