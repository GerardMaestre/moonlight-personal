package com.limelight.shared.data.remote.paging

import com.limelight.shared.data.remote.dto.AssetsResponseDto
import com.limelight.shared.data.remote.mapper.AssetMapper
import com.limelight.shared.domain.media.TimelinePage

class AssetPagingSource(
    private val loader: suspend (page: Int?, cursor: String?, size: Int) -> AssetsResponseDto,
    private val pageSize: Int = 60,
) {
    suspend fun load(page: Int? = 1, cursor: String? = null): TimelinePage {
        val response = loader(page, cursor, pageSize)
        return AssetMapper.toTimelinePage(response)
    }
}
