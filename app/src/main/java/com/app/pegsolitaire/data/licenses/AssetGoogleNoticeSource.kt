package com.app.pegsolitaire.data.licenses

import android.content.Context
import com.app.pegsolitaire.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * [GoogleNoticeSource] over the generated `licenses/google/index.tsv` in the APK's assets (tech
 * design §13.1 LN-4): read and parsed on the injected [io] dispatcher, about 108 lines.
 *
 * No cache: the file is small and fixed for the life of the APK, so the text screen reading it a
 * second time is cheaper than a singleton holding state. Only `IOException` is caught (a missing
 * index is a `FileNotFoundException`) and becomes `null`, the screens' error state, as does a
 * malformed index; anything else propagates.
 */
class AssetGoogleNoticeSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
    ) : GoogleNoticeSource {
        override suspend fun index(): List<GoogleNotice>? =
            withContext(io) {
                val raw =
                    try {
                        context.assets
                            .open(GoogleNoticeAssets.INDEX_PATH)
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                    } catch (ignored: IOException) {
                        null
                    }
                raw?.let(::parseGoogleNoticeIndex)
            }
    }
