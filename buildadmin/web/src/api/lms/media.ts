import { getUrl } from '/@/utils/axios'
import { blobRequest, downloadBlob, lmsRequest } from './client'
import type { MediaInfo } from '/@/types/lms/common'
import type { MediaType } from '/@/types/lms/media'

export const uploadMedia = (file: File, mediaType: MediaType) => {
    const data = new FormData()
    data.append('file', file)
    return lmsRequest<MediaInfo>({ url: '/api/media', method: 'post', params: { mediaType }, data })
}

export const mediaContentUrl = (id: number) => `${getUrl()}/api/media/${id}/content`

export const mediaDownloadUrl = (id: number) => `${getUrl()}/api/media/${id}/download`

export const downloadMedia = async (media: MediaInfo) => {
    try {
        const response = await blobRequest(`/api/media/${media.mediaId}/download`)
        downloadBlob(response.data, media.originalName)
    } catch {
        // blobRequest already displays the localized backend message.
    }
}
