import { lmsRequest } from './client'
import type { Lesson, LessonForm } from '/@/types/lms/lesson'

export const listLessons = (courseId: number) =>
    lmsRequest<Lesson[]>({
        url: '/api/lessons',
        method: 'get',
        params: { courseId },
    })

export const getLesson = (id: number) => lmsRequest<Lesson>({ url: `/api/lessons/${id}`, method: 'get' })

const appendFiles = (body: FormData, field: string, files?: File[]) => {
    files?.forEach((file) => body.append(field, file))
}

const multipart = (data: LessonForm, thumbnail?: File, images?: File[], videos?: File[]) => {
    const body = new FormData()
    body.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (thumbnail) body.append('thumbnail', thumbnail)
    appendFiles(body, 'images', images)
    appendFiles(body, 'videos', videos)
    return body
}

export const createLesson = (data: LessonForm, thumbnail?: File, images?: File[], videos?: File[]) =>
    lmsRequest<Lesson>({ url: '/api/lessons', method: 'post', data: multipart(data, thumbnail, images, videos) })

export const updateLesson = (id: number, data: LessonForm, thumbnail?: File, images?: File[], videos?: File[]) =>
    lmsRequest<Lesson>({ url: `/api/lessons/${id}`, method: 'put', data: multipart(data, thumbnail, images, videos) })

export const deleteLesson = (id: number) => lmsRequest<void>({ url: `/api/lessons/${id}`, method: 'delete' })
