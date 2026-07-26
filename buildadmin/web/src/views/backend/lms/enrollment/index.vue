<template>
    <div class="lms-page lms-enrollment-page">
        <section class="lms-hero">
            <div>
                <span class="lms-kicker">{{ $t('lms.training') }}</span>
                <h1>{{ $t('lms.enrollmentListTitle') }}</h1>
                <p>{{ $t('lms.enrollmentListSubtitle') }}</p>
            </div>
            <div class="lms-metric">
                <strong>{{ totalItems }}</strong>
                <span>{{ $t('lms.enrollments') }}</span>
            </div>
        </section>

        <el-card shadow="never" class="enrollment-list-card">
            <div class="lms-toolbar enrollment-toolbar">
                <div class="lms-filters enrollment-filters">
                    <el-select
                        v-model="courseId"
                        filterable
                        clearable
                        :placeholder="$t('lms.allCourses')"
                        class="course-filter"
                        :loading="optionsLoading"
                        @change="changeCourse"
                    >
                        <el-option v-for="course in courses" :key="course.id" :label="courseLabel(course)" :value="course.id" />
                    </el-select>
                    <el-input v-model="keyword" clearable :placeholder="$t('lms.searchStudents')" @keyup.enter="applyFilters" @clear="applyFilters" />
                    <el-select v-model="status" clearable :placeholder="$t('lms.status')" @change="applyFilters">
                        <el-option v-for="item in statusOptions" :key="item.value" :label="$t(item.label)" :value="item.value" />
                    </el-select>
                    <el-button @click="clearFilters">{{ $t('lms.reset') }}</el-button>
                </div>
                <el-button type="primary" @click="goCreate">{{ $t('lms.addEnrollment') }}</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" :empty-text="$t('lms.noData')">
                <el-table-column :label="$t('lms.student')" min-width="230">
                    <template #default="{ row }">
                        <div class="enrollment-student-cell">
                            <span class="student-initial">{{ getInitials(row.studentName) }}</span>
                            <div>
                                <strong>{{ row.studentName }}</strong>
                                <small>{{ row.studentCode }}</small>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.course')" min-width="240">
                    <template #default="{ row }">
                        <div class="course-cell">
                            <strong>{{ row.courseName }}</strong>
                            <small>{{ row.courseCode }}</small>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.enrolledAt')" width="145">
                    <template #default="{ row }">{{ formatDate(row.enrolledAt) }}</template>
                </el-table-column>
                <el-table-column :label="$t('lms.status')" width="145">
                    <template #default="{ row }">
                        <el-tag :type="statusMeta(row.enrollmentStatus).type" effect="light" round>
                            {{ statusMeta(row.enrollmentStatus).label }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.progress')" min-width="190">
                    <template #default="{ row }">
                        <div class="progress-cell">
                            <el-progress :percentage="Number(row.progressPercent)" :show-text="false" :stroke-width="8" />
                            <strong>{{ Number(row.progressPercent) }}%</strong>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.actions')" width="144" fixed="right" align="right">
                    <template #default="{ row }">
                        <div class="lms-row-actions">
                            <el-tooltip :content="$t('lms.detail')" placement="top">
                                <el-button class="lms-row-action" circle :aria-label="$t('lms.detail')" @click="goDetail(row)">
                                    <el-icon><View /></el-icon>
                                </el-button>
                            </el-tooltip>
                            <el-tooltip :content="$t('lms.edit')" placement="top">
                                <el-button class="lms-row-action" circle :aria-label="$t('lms.edit')" @click="goEdit(row)">
                                    <el-icon><EditPen /></el-icon>
                                </el-button>
                            </el-tooltip>
                            <el-tooltip :content="$t('lms.delete')" placement="top">
                                <el-button class="lms-row-action is-danger" circle :aria-label="$t('lms.delete')" @click="remove(row)">
                                    <el-icon><Delete /></el-icon>
                                </el-button>
                            </el-tooltip>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <div class="lms-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50]"
                    :total="totalItems"
                    layout="total, sizes, prev, pager, next"
                    @current-change="load"
                    @size-change="changePageSize"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete, EditPen, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { searchCourses } from '/@/api/lms/courseApi'
import { deleteEnrollment, searchEnrollments } from '/@/api/lms/enrollmentApi'
import type { Course } from '/@/types/lms/course'
import type { Enrollment, EnrollmentStatus } from '/@/types/lms/enrollment'
import '/@/styles/lms.scss'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const courses = ref<Course[]>([])
const rows = ref<Enrollment[]>([])
const courseId = ref<number>()
const keyword = ref('')
const status = ref<EnrollmentStatus>()
const loading = ref(false)
const optionsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)

const statusOptions: Array<{ value: EnrollmentStatus; label: string }> = [
    { value: 'ENROLLED', label: 'lms.enrolled' },
    { value: 'LEARNING', label: 'lms.learning' },
    { value: 'COMPLETED', label: 'lms.completed' },
    { value: 'CANCELLED', label: 'lms.cancelled' },
]

onMounted(async () => {
    optionsLoading.value = true
    try {
        const response = await searchCourses({ page: 0, size: 200 })
        courses.value = response.data.items
        const queryCourseId = Number(route.query.courseId)
        courseId.value = courses.value.some((course) => course.id === queryCourseId) ? queryCourseId : undefined
        await load()
    } finally {
        optionsLoading.value = false
    }
})

function courseLabel(course: Course) {
    return `${course.courseCode} · ${course.courseName}`
}

function getInitials(name: string) {
    return name
        .trim()
        .split(/\s+/)
        .slice(-2)
        .map((part) => part[0])
        .join('')
        .toUpperCase()
}

function formatDate(value: string) {
    if (!value) return '—'
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'vi-VN').format(date)
}

function statusMeta(value: EnrollmentStatus): { label: string; type: 'success' | 'warning' | 'info' | 'danger' } {
    const option = statusOptions.find((item) => item.value === value)
    const types: Record<EnrollmentStatus, 'success' | 'warning' | 'info' | 'danger'> = {
        ENROLLED: 'info',
        LEARNING: 'warning',
        COMPLETED: 'success',
        CANCELLED: 'danger',
    }
    return { label: option ? t(option.label) : value, type: types[value] }
}

async function applyFilters() {
    currentPage.value = 1
    await load()
}

async function clearFilters() {
    keyword.value = ''
    status.value = undefined
    await applyFilters()
}

async function changeCourse() {
    currentPage.value = 1
    await router.replace({ query: courseId.value ? { courseId: String(courseId.value) } : {} })
    await load()
}

async function changePageSize() {
    currentPage.value = 1
    await load()
}

async function load() {
    loading.value = true
    try {
        const response = await searchEnrollments({
            courseId: courseId.value,
            keyword: keyword.value.trim() || undefined,
            status: status.value,
            page: currentPage.value - 1,
            size: pageSize.value,
        })
        rows.value = response.data.items
        totalItems.value = response.data.totalItems
    } finally {
        loading.value = false
    }
}

function goCreate() {
    router.push({ name: 'lmsEnrollmentCreate', query: courseId.value ? { courseId: String(courseId.value) } : {} })
}

function goDetail(row: Enrollment) {
    router.push({ name: 'lmsEnrollmentDetail', params: { id: row.id }, query: { courseId: String(row.courseId) } })
}

function goEdit(row: Enrollment) {
    router.push({ name: 'lmsEnrollmentEdit', params: { id: row.id }, query: { courseId: String(row.courseId) } })
}

async function remove(row: Enrollment) {
    try {
        await ElMessageBox.confirm(t('lms.deleteEnrollmentConfirm', { name: row.studentName }), t('lms.confirmDelete'), {
            type: 'warning',
            confirmButtonText: t('lms.deleteEnrollment'),
            cancelButtonText: t('lms.cancel'),
        })
    } catch {
        return
    }
    const response = await deleteEnrollment(row.id)
    ElMessage.success(response.message)
    if (rows.value.length === 1 && currentPage.value > 1) currentPage.value -= 1
    await load()
}
</script>

<style scoped lang="scss">
.enrollment-list-card {
    overflow: visible;
}

.enrollment-filters {
    flex: 1;
}

.enrollment-filters .course-filter {
    min-width: 285px;
}

.enrollment-filters .el-input {
    width: 260px;
}

.enrollment-filters > .el-select:not(.course-filter) {
    width: 180px;
}

.enrollment-student-cell,
.progress-cell {
    display: flex;
    gap: 12px;
    align-items: center;
}

.student-initial {
    display: grid;
    flex: 0 0 auto;
    width: 38px;
    height: 38px;
    font-size: 12px;
    font-weight: 700;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border: 1px solid var(--el-color-primary-light-7);
    border-radius: 11px;
    place-items: center;
}

.enrollment-student-cell > div,
.course-cell {
    display: grid;
    min-width: 0;
    gap: 4px;
}

.enrollment-student-cell strong,
.course-cell strong {
    overflow: hidden;
    font-size: 14px;
    color: var(--el-text-color-primary);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.enrollment-student-cell small,
.course-cell small {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.progress-cell .el-progress {
    flex: 1;
}

.progress-cell > strong {
    min-width: 38px;
    font-size: 13px;
    text-align: right;
}

@media (max-width: 1100px) {
    .enrollment-toolbar {
        align-items: stretch;
        flex-direction: column;
    }
}

@media (max-width: 720px) {
    .enrollment-filters,
    .enrollment-filters .course-filter,
    .enrollment-filters .el-input,
    .enrollment-filters > .el-select:not(.course-filter) {
        width: 100%;
        min-width: 0;
    }
}
</style>
