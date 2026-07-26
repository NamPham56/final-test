import { blobRequest, lmsRequest } from './client'
import type { PageResponse } from '/@/types/lms/common'
import type { Student, StudentForm, StudentQuery } from '/@/types/lms/student'

export const searchStudents = (params: StudentQuery) => lmsRequest<PageResponse<Student>>({ url: '/api/students', method: 'get', params })

export const getStudent = (id: number) => lmsRequest<Student>({ url: `/api/students/${id}`, method: 'get' })

const multipart = (data: StudentForm, avatar?: File) => {
    const body = new FormData()
    body.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (avatar) body.append('avatar', avatar)
    return body
}

export const createStudent = (data: StudentForm, avatar?: File) =>
    lmsRequest<Student>({ url: '/api/students', method: 'post', data: multipart(data, avatar) })

export const updateStudent = (id: number, data: StudentForm, avatar?: File) =>
    lmsRequest<Student>({ url: `/api/students/${id}`, method: 'put', data: multipart(data, avatar) })

export const deleteStudent = (id: number) => lmsRequest<void>({ url: `/api/students/${id}`, method: 'delete' })

export const exportStudents = (keyword?: string) => blobRequest('/api/students/export', { keyword })
