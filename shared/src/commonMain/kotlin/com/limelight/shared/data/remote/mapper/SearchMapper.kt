package com.limelight.shared.data.remote.mapper

import com.limelight.shared.data.remote.dto.SearchResponseDto
import com.limelight.shared.domain.media.SearchResult

object SearchMapper {
    fun toDomain(dto: SearchResponseDto): SearchResult {
        val assets = requireNotNull(dto.assets) { "Search response is missing assets page" }
        return SearchResult(page = AssetMapper.toTimelinePage(assets))
    }
}
