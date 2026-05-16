package com.limelight.shared.data.remote.mapper

import com.limelight.shared.data.remote.dto.AlbumDto
import com.limelight.shared.domain.media.Album

object AlbumMapper {
    fun toDomain(dto: AlbumDto): Album {
        val id = requireNotNull(dto.id) { "Album id is required" }
        val name = requireNotNull(dto.albumName) { "Album name is required for album $id" }
        val createdAt = requireNotNull(dto.createdAt) { "Album createdAt is required for album $id" }
        return Album(
            id = id,
            name = name,
            assetCount = dto.assetCount ?: 0,
            createdAtIso = createdAt,
        )
    }
}
