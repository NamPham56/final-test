export type EnrollmentStatus = 'ENROLLED' | 'LEARNING' | 'COMPLETED' | 'CANCELLED'

export interface Enrollment {
    id: number
    studentId: number
    studentCode: string
    studentName: string
    courseId: number
    courseCode: string
    courseName: string
    enrolledAt: string
    enrollmentStatus: EnrollmentStatus
    progressPercent: number
    completedAt?: string
}

export interface EnrollmentCreate {
    studentId: number
    courseIds: number[]
}

export interface EnrollmentUpdate {
    enrollmentStatus: EnrollmentStatus
    progressPercent: number
}

export interface EnrollmentQuery {
    courseId?: number
    keyword?: string
    status?: EnrollmentStatus
    page: number
    size: number
}
