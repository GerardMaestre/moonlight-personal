package com.limelight.shared.data.remote.mapper

import com.limelight.shared.data.remote.dto.AssetDto
import com.limelight.shared.data.remote.dto.AssetsResponseDto
import com.limelight.shared.domain.media.Asset
import com.limelight.shared.domain.media.AssetType
import com.limelight.shared.domain.media.TimelinePage

object AssetMapper {
    fun toDomain(dto: AssetDto): Asset {
        val id = requireNotNull(dto.id) { "Asset id is required" }
        val createdAt = requireNotNull(dto.fileCreatedAt) { "Asset fileCreatedAt is required for asset $id" }
        return Asset(
            id = id,
            fileName = dto.originalFileName?.takeIf { it.isNotBlank() } ?: id,
            mimeType = dto.originalMimeType.orEmpty(),
            createdAtIso = createdAt,
            updatedAtIso = dto.updatedAt,
            type = dto.type.toAssetType(),
            isFavorite = dto.isFavorite ?: false,
            city = dto.exifInfo?.city,
            country = dto.exifInfo?.country,
        )
    }

    fun toTimelinePage(dto: AssetsResponseDto): TimelinePage {
        val items = dto.items.orEmpty().map(::toDomain)
        val pageNumber = dto.nextPage?.toIntOrNull()
        return TimelinePage(
            items = items,
            nextPage = pageNumber,
            nextCursor = dto.nextCursor,
            total = dto.total ?: items.size,
        )
    }

    private fun String?.toAssetType(): AssetType = when (this?.uppercase()) {
        "IMAGE" -> AssetType.IMAGE
        "VIDEO" -> AssetType.VIDEO
        else -> AssetType.OTHER
    }
}
