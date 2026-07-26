import type { RouteRecordRaw } from 'vue-router'

export const adminBaseRoutePath = '/admin'

const adminBaseRoute: RouteRecordRaw = {
    path: adminBaseRoutePath,
    name: 'admin',
    component: () => import('/@/layouts/backend/index.vue'),
    redirect: adminBaseRoutePath + '/students',
    meta: { title: 'pagesTitle.admin' },
    children: [
        {
            path: 'students/create',
            name: 'lmsStudentCreate',
            component: () => import('/@/views/backend/lms/student/editor.vue'),
            meta: { title: 'lms.addStudent' },
        },
        {
            path: 'students/:id/detail',
            name: 'lmsStudentDetail',
            component: () => import('/@/views/backend/lms/student/editor.vue'),
            meta: { title: 'lms.detail' },
        },
        {
            path: 'students/:id/edit',
            name: 'lmsStudentEdit',
            component: () => import('/@/views/backend/lms/student/editor.vue'),
            meta: { title: 'lms.edit' },
        },
        { path: 'students', name: 'lmsStudents', component: () => import('/@/views/backend/lms/student/index.vue'), meta: { title: 'lms.students' } },

        {
            path: 'courses/create',
            name: 'lmsCourseCreate',
            component: () => import('/@/views/backend/lms/course/form.vue'),
            meta: { title: 'lms.addCourse', editorMode: 'create' },
        },
        {
            path: 'courses/:id/detail',
            name: 'lmsCourseDetail',
            component: () => import('/@/views/backend/lms/course/form.vue'),
            meta: { title: 'lms.detail', editorMode: 'detail' },
        },
        {
            path: 'courses/:id/edit',
            name: 'lmsCourseEdit',
            component: () => import('/@/views/backend/lms/course/form.vue'),
            meta: { title: 'lms.edit', editorMode: 'edit' },
        },
        {
            path: 'courses/:id/lessons',
            name: 'lmsCourseLessons',
            component: () => import('/@/views/backend/lms/lesson/index.vue'),
            meta: { title: 'lms.lessons' },
        },
        { path: 'courses', name: 'lmsCourses', component: () => import('/@/views/backend/lms/course/index.vue'), meta: { title: 'lms.courses' } },

        {
            path: 'lessons/create',
            name: 'lmsLessonCreate',
            component: () => import('/@/views/backend/lms/lesson/form.vue'),
            meta: { title: 'lms.addLesson', editorMode: 'create' },
        },
        {
            path: 'lessons/:id/detail',
            name: 'lmsLessonDetail',
            component: () => import('/@/views/backend/lms/lesson/form.vue'),
            meta: { title: 'lms.detail', editorMode: 'detail' },
        },
        {
            path: 'lessons/:id/edit',
            name: 'lmsLessonEdit',
            component: () => import('/@/views/backend/lms/lesson/form.vue'),
            meta: { title: 'lms.edit', editorMode: 'edit' },
        },
        { path: 'lessons', name: 'lmsLessons', component: () => import('/@/views/backend/lms/lesson/index.vue'), meta: { title: 'lms.lessons' } },

        {
            path: 'enrollments/create',
            name: 'lmsEnrollmentCreate',
            component: () => import('/@/views/backend/lms/enrollment/form.vue'),
            meta: { title: 'lms.addEnrollment' },
        },
        {
            path: 'enrollments/:id/detail',
            name: 'lmsEnrollmentDetail',
            component: () => import('/@/views/backend/lms/enrollment/form.vue'),
            meta: { title: 'lms.detail' },
        },
        {
            path: 'enrollments/:id/edit',
            name: 'lmsEnrollmentEdit',
            component: () => import('/@/views/backend/lms/enrollment/form.vue'),
            meta: { title: 'lms.edit' },
        },
        {
            path: 'enrollments',
            name: 'lmsEnrollments',
            component: () => import('/@/views/backend/lms/enrollment/index.vue'),
            meta: { title: 'lms.enrollments' },
        },

        { path: 'media', name: 'lmsMedia', component: () => import('/@/views/backend/lms/media/index.vue'), meta: { title: 'lms.media' } },
        {
            path: 'loading/:to?',
            name: 'adminMainLoading',
            component: () => import('/@/layouts/common/components/loading.vue'),
            meta: { title: 'pagesTitle.loading' },
        },
    ],
}

export default adminBaseRoute
