export interface ApiResponse<T> { success: boolean; code: string; message: string; data: T; timestamp: string }
export interface PageResponse<T> { items: T[]; page: number; size: number; totalItems: number; totalPages: number; hasNext: boolean; hasPrevious: boolean }
export interface MediaInfo { mediaId: number; originalName: string; mimeType: string; fileSize: number; mediaType?: 'AVATAR'|'THUMBNAIL'|'IMAGE'|'VIDEO'|'DOCUMENT' }
export interface MediaChanges { retainedMediaIds: number[]; newMediaIds: number[]; removedMediaIds: number[] }
