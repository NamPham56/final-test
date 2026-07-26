import { lmsRequest } from './client'
import type { PageResponse } from '/@/types/lms/common'
import type { Enrollment, EnrollmentCreate, EnrollmentQuery, EnrollmentUpdate } from '/@/types/lms/enrollment'

export const searchEnrollments = (params: EnrollmentQuery) =>
    lmsRequest<PageResponse<Enrollment>>({ url: '/api/enrollments', method: 'get', params })

export const getEnrollment = (id: number) => lmsRequest<Enrollment>({ url: `/api/enrollments/${id}`, method: 'get' })

export const enroll = (data: EnrollmentCreate) => lmsRequest<Enrollment[]>({ url: '/api/enrollments', method: 'post', data })

export const updateEnrollment = (id: number, data: EnrollmentUpdate) => lmsRequest<Enrollment>({ url: `/api/enrollments/${id}`, method: 'put', data })

export const deleteEnrollment = (id: number) => lmsRequest<void>({ url: `/api/enrollments/${id}`, method: 'delete' })

export const courseStudents = (courseId: number) => lmsRequest<Enrollment[]>({ url: `/api/enrollments/course/${courseId}/students`, method: 'get' })
