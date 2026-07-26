<template>
    <div class="lms-form-page enrollment-editor-page">
        <header class="lms-page-heading">
            <div class="lms-page-heading__main">
                <el-button class="lms-page-heading__back" circle :aria-label="$t('lms.back')" @click="goBack">
                    <el-icon><ArrowLeft /></el-icon>
                </el-button>
                <div>
                    <h1>{{ pageTitle }}</h1>
                    <p>{{ pageDescription }}</p>
                </div>
            </div>
            <el-tag v-if="isDetail" type="info" effect="plain" round>{{ $t('lms.detail') }}</el-tag>
        </header>

        <el-skeleton v-if="loading" animated :rows="9" class="editor-skeleton" />

        <el-empty v-else-if="errorMessage" :description="errorMessage" class="editor-empty">
            <el-button type="primary" @click="goBack">{{ $t('lms.backToList') }}</el-button>
        </el-empty>

        <div v-else class="lms-form-layout">
            <EnrollmentSummaryCard
                :enrollment-id="enrollment?.id"
                :student-name="summaryStudent?.fullName || enrollment?.studentName"
                :student-code="summaryStudent?.studentCode || enrollment?.studentCode"
                :course-name="summaryCourseName"
                :enrolled-at="enrollment?.enrolledAt"
                :status="form.enrollmentStatus"
                :progress="Number(form.progressPercent)"
            />

            <section class="lms-form-card enrollment-form-card">
                <el-form ref="formRef" :model="form" :rules="rules" label-position="top" status-icon>
                    <div class="lms-form-card__body">
                        <el-tabs v-model="activeTab" class="lms-form-tabs">
                            <el-tab-pane :label="$t('lms.generalInfo')" name="general">
                                <div class="lms-section-heading">
                                    <h3>{{ $t('lms.enrollmentInfo') }}</h3>
                                    <p>{{ $t('lms.chooseStudentAndCourse') }}</p>
                                </div>

                                <div class="lms-form-grid">
                                    <el-form-item :label="$t('lms.student')" prop="studentId">
                                        <el-select v-model="form.studentId" filterable :placeholder="$t('lms.student')" :disabled="!isCreate || isDetail">
                                            <el-option
                                                v-for="student in students"
                                                :key="student.id"
                                                :label="`${student.studentCode} · ${student.fullName}`"
                                                :value="student.id"
                                            />
                                        </el-select>
                                    </el-form-item>

                                    <el-form-item :label="$t('lms.course')" prop="courseIds">
                                        <el-select
                                            v-model="form.courseIds"
                                            multiple
                                            filterable
                                            collapse-tags
                                            collapse-tags-tooltip
                                            :placeholder="$t('lms.course')"
                                            :disabled="!isCreate || isDetail"
                                            @change="validateCourseIdsRealtime"
                                        >
                                            <el-option
                                                v-for="course in courses"
                                                :key="course.id"
                                                :label="`${course.courseCode} · ${course.courseName}`"
                                                :value="course.id"
                                            />
                                        </el-select>
                                    </el-form-item>

                                    <div v-if="form.courseIds.length" class="selected-course-array lms-form-grid__wide">
                                        <div class="selected-course-array__head">
                                            <strong>{{ $t('lms.selectedCourses') }}</strong>
                                            <small>{{ $t('lms.realtimeArrayValidation') }}</small>
                                        </div>

                                        <el-form-item
                                            v-for="(courseId, index) in form.courseIds"
                                            :key="`${courseId}-${index}`"
                                            :prop="courseIdFieldProps[index]"
                                            :rules="courseIdRules(index)"
                                        >
                                            <div class="selected-course-array__row">
                                                <span class="selected-course-array__index">#{{ index + 1 }}</span>
                                                <span class="selected-course-array__name">{{ courseNameById(courseId) }}</span>
                                                <el-tooltip v-if="isCreate && !isDetail" :content="$t('lms.delete')" placement="top" :show-after="300">
                                                    <el-button
                                                        class="lms-row-action is-danger"
                                                        circle
                                                        :icon="Delete"
                                                        :aria-label="$t('lms.delete')"
                                                        @click="removeSelectedCourse(index)"
                                                    />
                                                </el-tooltip>
                                            </div>
                                        </el-form-item>
                                    </div>

                                    <el-form-item :label="$t('lms.enrolledAt')">
                                        <el-input :model-value="formatDateTime(enrollment?.enrolledAt)" disabled :placeholder="$t('lms.autoGenerated')" />
                                    </el-form-item>

                                    <el-form-item :label="$t('lms.enrollmentCode')">
                                        <el-input :model-value="enrollment?.id ? `#${enrollment.id}` : ''" disabled :placeholder="$t('lms.autoGenerated')" />
                                    </el-form-item>
                                </div>

                                <el-alert
                                    v-if="isCreate"
                                    :title="$t('lms.multiCourseEnrollmentHint')"
                                    type="info"
                                    :closable="false"
                                    show-icon
                                />
                            </el-tab-pane>

                            <el-tab-pane :label="$t('lms.progress')" name="progress">
                                <div class="lms-section-heading">
                                    <h3>{{ $t('lms.statusProgressTitle') }}</h3>
                                    <p>{{ $t('lms.statusProgressDescription') }}</p>
                                </div>

                                <div class="lms-form-grid">
                                    <el-form-item :label="$t('lms.status')" prop="enrollmentStatus">
                                        <el-select v-model="form.enrollmentStatus" :placeholder="$t('lms.status')" :disabled="isCreate || isDetail">
                                            <el-option v-for="item in statusOptions" :key="item.value" :label="$t(item.label)" :value="item.value" />
                                        </el-select>
                                    </el-form-item>

                                    <el-form-item :label="$t('lms.completedAt')">
                                        <el-input :model-value="formatDateTime(enrollment?.completedAt)" disabled :placeholder="$t('lms.notConfigured')" />
                                    </el-form-item>

                                    <el-form-item :label="$t('lms.progress')" prop="progressPercent" class="lms-form-grid__wide">
                                        <div class="progress-editor">
                                            <el-slider
                                                v-model="form.progressPercent"
                                                :min="0"
                                                :max="100"
                                                :step="1"
                                                :disabled="isCreate || isDetail || isCompleted"
                                                show-stops
                                            />
                                            <el-input-number
                                                v-model="form.progressPercent"
                                                :min="0"
                                                :max="100"
                                                :disabled="isCreate || isDetail || isCompleted"
                                                controls-position="right"
                                            />
                                            <span>%</span>
                                        </div>
                                    </el-form-item>
                                </div>

                                <el-alert
                                    v-if="isCreate"
                                    :title="$t('lms.chooseStudentAndCourse')"
                                    type="info"
                                    :closable="false"
                                    show-icon
                                />
                            </el-tab-pane>
                        </el-tabs>
                    </div>

                    <footer class="lms-form-footer">
                        <span v-if="dirty" class="lms-form-footer__dirty">● {{ $t('lms.unsavedChanges') }}</span>
                        <span v-else></span>
                        <div>
                            <el-button :disabled="saving" @click="goBack">{{ $t('lms.back') }}</el-button>
                            <el-button v-if="isDetail" type="primary" @click="goEdit">{{ $t('lms.edit') }}</el-button>
                            <el-button v-else type="primary" :loading="saving" :disabled="saving" @click="save">
                                {{ isCreate ? $t('lms.createEnrollment') : $t('lms.save') }}
                            </el-button>
                        </div>
                    </footer>
                </el-form>
            </section>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormItemRule, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { searchCourses } from '/@/api/lms/courseApi'
import { enroll, getEnrollment, updateEnrollment } from '/@/api/lms/enrollmentApi'
import { searchStudents } from '/@/api/lms/studentApi'
import EnrollmentSummaryCard from '/@/components/lms/enrollment/EnrollmentSummaryCard.vue'
import type { Course } from '/@/types/lms/course'
import type { Enrollment, EnrollmentStatus } from '/@/types/lms/enrollment'
import type { Student } from '/@/types/lms/student'
import '/@/styles/lms.scss'

interface EnrollmentEditorModel {
    studentId?: number
    courseIds: number[]
    enrollmentStatus: EnrollmentStatus
    progressPercent: number
}

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const formRef = ref<FormInstance>()
const courses = ref<Course[]>([])
const students = ref<Student[]>([])
const enrollment = ref<Enrollment>()
const loading = ref(true)
const saving = ref(false)
const activeTab = ref('general')
const errorMessage = ref('')
const snapshot = ref('')
const form = reactive<EnrollmentEditorModel>({
    studentId: undefined,
    courseIds: [],
    enrollmentStatus: 'ENROLLED',
    progressPercent: 0,
})

const isCreate = computed(() => route.path.endsWith('/create'))
const isDetail = computed(() => route.path.endsWith('/detail'))
const isCompleted = computed(() => form.enrollmentStatus === 'COMPLETED')
const enrollmentId = computed(() => Number(route.params.id))
const formState = computed(() => JSON.stringify(form))
const dirty = computed(() => !loading.value && !isDetail.value && snapshot.value !== formState.value)
const summaryStudent = computed(() => students.value.find((item) => item.id === form.studentId))
const selectedCourses = computed(() => courses.value.filter((item) => form.courseIds.includes(item.id)))
const courseIdFieldProps = computed(() => form.courseIds.map((_courseId, index) => `courseIds.${index}`))
const summaryCourseName = computed(() => {
    if (enrollment.value) return enrollment.value.courseName
    if (!selectedCourses.value.length) return ''
    const [first, ...rest] = selectedCourses.value
    return rest.length ? t('lms.moreCourses', { name: first.courseName, count: rest.length }) : first.courseName
})
const pageTitle = computed(() => (isCreate.value ? t('lms.addEnrollment') : isDetail.value ? t('lms.detail') : t('lms.edit')))
const pageDescription = computed(() => {
    if (isCreate.value) return t('lms.enrollmentCreateDescription')
    if (isDetail.value) return t('lms.enrollmentDetailDescription')
    return t('lms.enrollmentEditDescription')
})

const statusOptions: Array<{ value: EnrollmentStatus; label: string }> = [
    { value: 'ENROLLED', label: 'lms.enrolled' },
    { value: 'LEARNING', label: 'lms.learning' },
    { value: 'COMPLETED', label: 'lms.completed' },
    { value: 'CANCELLED', label: 'lms.cancelled' },
]

const validateCourseIds = (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
    const selectedIds = form.courseIds.map(Number).filter((courseId) => Number.isInteger(courseId) && courseId > 0)
    if (!selectedIds.length) {
        callback(new Error(t('lms.selectCourse')))
        return
    }
    callback()
}

const rules: FormRules<EnrollmentEditorModel> = {
    studentId: [{ required: true, message: t('lms.studentRequired'), trigger: 'change' }],
    courseIds: [{ validator: validateCourseIds, trigger: 'change' }],
    enrollmentStatus: [{ required: true, message: t('lms.statusRequired'), trigger: 'change' }],
    progressPercent: [
        { required: true, message: t('lms.progressRequired'), trigger: 'change' },
        {
            validator: (_rule, value, callback) => {
                const progress = Number(value)
                if (progress < 0 || progress > 100) callback(new Error(t('lms.progressRange')))
                else callback()
            },
            trigger: 'change',
        },
    ],
}

function courseIdRules(index: number): FormItemRule[] {
    return [{ validator: validateCourseId(index), trigger: 'change' }]
}

function validateCourseId(index: number) {
    return (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        const courseId = Number(form.courseIds[index])

        if (!Number.isInteger(courseId) || courseId <= 0) {
            callback(new Error(t('lms.courseInvalid')))
            return
        }

        if (!courses.value.some((course) => course.id === courseId)) {
            callback(new Error(t('lms.courseUnavailable')))
            return
        }

        if (form.courseIds.indexOf(courseId) !== index) {
            callback(new Error(t('lms.duplicateCourseSelection')))
            return
        }

        callback()
    }
}

function validateCourseIdsRealtime() {
    void nextTick(() => {
        formRef.value?.clearValidate('courseIds')
        formRef.value?.validateField('courseIds')
        courseIdFieldProps.value.forEach((prop) => formRef.value?.validateField(prop))
    })
}

function removeSelectedCourse(index: number) {
    form.courseIds.splice(index, 1)
    validateCourseIdsRealtime()
}

function courseNameById(courseId: number) {
    const course = courses.value.find((item) => item.id === courseId)
    return course ? `${course.courseCode} · ${course.courseName}` : `${t('lms.course')} #${courseId}`
}

onMounted(loadPage)

watch(() => form.courseIds.slice(), validateCourseIdsRealtime, { flush: 'post' })
watch(
    () => form.enrollmentStatus,
    (status) => {
        if (status !== 'COMPLETED') return
        form.progressPercent = 100
        void nextTick(() => formRef.value?.validateField('progressPercent'))
    },
)

onBeforeRouteLeave(async () => {
    if (!dirty.value) return true
    try {
        await ElMessageBox.confirm(t('lms.unsavedMessage'), t('lms.unsavedTitle'), {
            type: 'warning',
            confirmButtonText: t('lms.leavePage'),
            cancelButtonText: t('lms.continueEditing'),
        })
        return true
    } catch {
        return false
    }
})

async function loadPage() {
    loading.value = true
    errorMessage.value = ''
    try {
        const [courseResponse, studentResponse] = await Promise.all([searchCourses({ page: 0, size: 200 }), searchStudents({ page: 0, size: 200 })])
        courses.value = courseResponse.data.items
        students.value = studentResponse.data.items

        const queryCourseId = Number(route.query.courseId)
        if (isCreate.value) {
            if (courses.value.some((item) => item.id === queryCourseId)) form.courseIds = [queryCourseId]
        } else {
            if (!enrollmentId.value) {
                errorMessage.value = t('lms.enrollmentContextMissing')
                return
            }
            const response = await getEnrollment(enrollmentId.value)
            const found = response.data
            enrollment.value = found
            form.studentId = found.studentId
            form.courseIds = [found.courseId]
            form.enrollmentStatus = found.enrollmentStatus
            form.progressPercent = found.enrollmentStatus === 'COMPLETED' ? 100 : Number(found.progressPercent)
        }
        await markClean()
    } catch {
        errorMessage.value = t('lms.enrollmentLoadFailed')
    } finally {
        loading.value = false
        await markClean()
    }
}

async function markClean() {
    await nextTick()
    snapshot.value = formState.value
}

function formatDateTime(value?: string) {
    if (!value) return ''
    const date = new Date(value)
    return Number.isNaN(date.getTime())
        ? value
        : new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function listQuery() {
    const selectedCourseId = enrollment.value?.courseId || form.courseIds[0]
    return selectedCourseId ? { courseId: String(selectedCourseId) } : {}
}

function goBack() {
    router.push({ name: 'lmsEnrollments', query: listQuery() })
}

function goEdit() {
    if (!enrollment.value) return
    router.push({ name: 'lmsEnrollmentEdit', params: { id: enrollment.value.id }, query: { courseId: String(enrollment.value.courseId) } })
}

async function save() {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) {
        activeTab.value = 'general'
        return
    }
    saving.value = true
    try {
        const response = isCreate.value
            ? await enroll({ studentId: Number(form.studentId), courseIds: form.courseIds.map(Number) })
            : await updateEnrollment(enrollmentId.value, {
                  enrollmentStatus: form.enrollmentStatus,
                  progressPercent: isCompleted.value ? 100 : Number(form.progressPercent),
              })
        ElMessage.success(response.message || t(isCreate.value ? 'lms.enrollmentCreateSuccess' : 'lms.enrollmentUpdateSuccess'))
        await markClean()
        await router.push({ name: 'lmsEnrollments', query: listQuery() })
    } finally {
        saving.value = false
    }
}
</script>

<style scoped lang="scss">
.editor-skeleton,
.editor-empty {
    padding: 32px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 16px;
}

.enrollment-form-card :deep(.el-alert) {
    margin-top: 10px;
}

.progress-editor {
    display: flex;
    width: 100%;
    gap: 20px;
    align-items: center;
    padding: 20px;
    background: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
}

.progress-editor .el-slider {
    flex: 1;
    min-width: 160px;
}

.progress-editor .el-input-number {
    flex: 0 0 130px;
}

.progress-editor > span {
    font-weight: 700;
    color: var(--el-text-color-secondary);
}

.selected-course-array {
    padding: 14px;
    background: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
}

.selected-course-array__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.selected-course-array__head strong {
    color: var(--el-text-color-primary);
}

.selected-course-array__head small {
    color: var(--el-text-color-secondary);
}

.selected-course-array :deep(.el-form-item) {
    margin-bottom: 10px;
}

.selected-course-array :deep(.el-form-item:last-child) {
    margin-bottom: 0;
}

.selected-course-array__row {
    display: grid;
    width: 100%;
    grid-template-columns: 48px minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
    min-height: 38px;
    padding: 0 10px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
}

.selected-course-array__index {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: 700;
}

.selected-course-array__name {
    overflow: hidden;
    color: var(--el-text-color-regular);
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 720px) {
    .progress-editor {
        flex-wrap: wrap;
    }

    .progress-editor .el-slider {
        flex-basis: 100%;
    }

    .selected-course-array__head,
    .selected-course-array__row {
        align-items: stretch;
        grid-template-columns: 1fr;
    }

    .selected-course-array__head {
        flex-direction: column;
    }
}
</style>
