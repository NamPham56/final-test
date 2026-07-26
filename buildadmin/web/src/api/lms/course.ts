import { blobRequest, lmsRequest } from './client'
import type { PageResponse } from '/@/types/lms/common'
import type { Course, CourseForm, CourseQuery } from '/@/types/lms/course'

export const searchCourses = (params: CourseQuery) =>
    lmsRequest<PageResponse<Course>>({
        url: '/api/courses',
        method: 'get',
        params,
    })

export const getCourse = (id: number) => lmsRequest<Course>({ url: `/api/courses/${id}`, method: 'get' })

const appendFiles = (body: FormData, field: string, files?: File[]) => {
    files?.forEach((file) => body.append(field, file))
}

const multipart = (data: CourseForm, thumbnail?: File, images?: File[], videos?: File[]) => {
    const body = new FormData()
    body.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (thumbnail) body.append('thumbnail', thumbnail)
    appendFiles(body, 'images', images)
    appendFiles(body, 'videos', videos)
    return body
}

export const createCourse = (data: CourseForm, thumbnail?: File, images?: File[], videos?: File[]) =>
    lmsRequest<Course>({ url: '/api/courses', method: 'post', data: multipart(data, thumbnail, images, videos) })

export const updateCourse = (id: number, data: CourseForm, thumbnail?: File, images?: File[], videos?: File[]) =>
    lmsRequest<Course>({ url: `/api/courses/${id}`, method: 'put', data: multipart(data, thumbnail, images, videos) })

export const deleteCourse = (id: number) => lmsRequest<void>({ url: `/api/courses/${id}`, method: 'delete' })

export const exportCourses = (params: Partial<CourseQuery>) => blobRequest('/api/courses/export', params)

export async function listAllCourses(): Promise<Course[]> {
    const courses: Course[] = []
    let page = 0
    const size = 100

    while (true) {
        const response = await searchCourses({ page, size })
        courses.push(...response.data.items)
        if (!response.data.hasNext) return courses
        page += 1
    }
}
