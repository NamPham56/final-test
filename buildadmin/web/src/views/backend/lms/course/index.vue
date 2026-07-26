<template>
    <div class="lms-page lms-course">
        <section class="lms-hero">
            <div>
                <span class="lms-kicker">{{ $t('lms.app') }} · {{ $t('lms.training') }}</span>
                <h1>{{ $t('lms.courseListTitle') }}</h1>
                <p>{{ $t('lms.courseListSubtitle') }}</p>
            </div>
            <div class="lms-metric">
                <strong>{{ total }}</strong>
                <span>{{ $t('lms.activeCourses') }}</span>
            </div>
        </section>

        <el-card shadow="never">
            <div class="lms-toolbar">
                <div class="lms-filters">
                    <el-input v-model="query.keyword" clearable :placeholder="$t('lms.searchCourses')" @keyup.enter="search" @clear="search" />
                    <el-date-picker
                        v-model="dates"
                        class="course-date-filter"
                        type="daterange"
                        unlink-panels
                        value-format="YYYY-MM-DD"
                        range-separator="-"
                        :start-placeholder="$t('lms.startDate')"
                        :end-placeholder="$t('lms.endDate')"
                    />
                    <el-button type="primary" @click="search">{{ $t('lms.search') }}</el-button>
                    <el-button @click="resetFilter">{{ $t('lms.reset') }}</el-button>
                </div>
                <div class="course-toolbar-actions">
                    <el-button :loading="exporting" @click="exportExcel">{{ $t('lms.exportExcel') }}</el-button>
                    <el-button type="primary" @click="goCreate">{{ $t('lms.addCourse') }}</el-button>
                </div>
            </div>

            <el-table v-loading="loading" :data="rows" row-key="id">
                <el-table-column :label="$t('lms.course')" min-width="300">
                    <template #default="{ row }">
                        <div class="course-cell">
                            <el-image v-if="row.media?.[0]" :src="mediaContentUrl(row.media[0].mediaId)" fit="cover" preview-teleported />
                            <div v-else class="course-cell__placeholder">{{ row.courseName?.charAt(0) || 'K' }}</div>
                            <div class="course-cell__content">
                                <strong>{{ row.courseName }}</strong>
                                <span>{{ row.courseCode }}</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.courseFee')" min-width="145">
                    <template #default="{ row }">
                        <strong class="course-price">{{ formatPrice(row.price) }}</strong>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.period')" min-width="210">
                    <template #default="{ row }">
                        <div class="course-period">
                            <span>{{ formatDate(row.startDate) }}</span>
                            <small>- {{ formatDate(row.endDate) }}</small>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.status')" width="145">
                    <template #default="{ row }">
                        <el-tag :type="courseState(row).type" effect="light" round>{{ courseState(row).label }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.actions')" width="180" fixed="right" align="right">
                    <template #default="{ row }">
                        <div class="lms-row-actions">
                            <el-tooltip :content="$t('lms.detail')" placement="top">
                                <el-button
                                    class="lms-row-action"
                                    circle
                                    :icon="View"
                                    :aria-label="$t('lms.detail')"
                                    @click="goDetail(row)"
                                />
                            </el-tooltip>
                            <el-tooltip :content="$t('lms.viewLessons')" placement="top">
                                <el-button
                                    class="lms-row-action"
                                    circle
                                    :icon="Reading"
                                    :aria-label="$t('lms.viewLessons')"
                                    @click="goLessons(row)"
                                />
                            </el-tooltip>
                            <el-tooltip :content="$t('lms.edit')" placement="top">
                                <el-button
                                    class="lms-row-action"
                                    circle
                                    :icon="EditPen"
                                    :aria-label="$t('lms.edit')"
                                    @click="goEdit(row)"
                                />
                            </el-tooltip>
                            <el-tooltip :content="$t('lms.delete')" placement="top">
                                <el-button
                                    class="lms-row-action is-danger"
                                    circle
                                    :icon="Delete"
                                    :aria-label="$t('lms.delete')"
                                    @click="remove(row)"
                                />
                            </el-tooltip>
                        </div>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty :description="$t('lms.noCourses')" />
                </template>
            </el-table>

            <div class="lms-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="query.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="total"
                    layout="total, sizes, prev, pager, next"
                    @current-change="load"
                    @size-change="changePageSize"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, EditPen, Reading, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { deleteCourse, exportCourses, searchCourses } from '/@/api/lms/courseApi'
import { downloadBlob } from '/@/api/lms/client'
import { mediaContentUrl } from '/@/api/lms/mediaApi'
import type { Course } from '/@/types/lms/course'
import '/@/styles/lms.scss'

const router = useRouter()
const { t, locale } = useI18n()
const rows = ref<Course[]>([])
const loading = ref(false)
const exporting = ref(false)
const total = ref(0)
const dates = ref<string[]>([])
const query = reactive({ keyword: '', fromDate: '', toDate: '', page: 0, size: 20 })
const currentPage = computed({ get: () => query.page + 1, set: (value) => (query.page = value - 1) })

watch(dates, (value) => {
    query.fromDate = value?.[0] || ''
    query.toDate = value?.[1] || ''
})

async function load() {
    loading.value = true
    try {
        const response = await searchCourses(query)
        rows.value = response.data.items
        total.value = response.data.totalItems
    } finally {
        loading.value = false
    }
}

function search() {
    query.page = 0
    load()
}

function resetFilter() {
    query.keyword = ''
    dates.value = []
    query.fromDate = ''
    query.toDate = ''
    search()
}

function changePageSize() {
    query.page = 0
    load()
}

function goCreate() {
    router.push({ name: 'lmsCourseCreate' })
}

function goDetail(row: Course) {
    router.push({ name: 'lmsCourseDetail', params: { id: row.id } })
}

function goEdit(row: Course) {
    router.push({ name: 'lmsCourseEdit', params: { id: row.id } })
}

function goLessons(row: Course) {
    router.push({ name: 'lmsCourseLessons', params: { id: row.id } })
}

async function remove(row: Course) {
    await ElMessageBox.confirm(t('lms.deleteCourseConfirm', { name: row.courseName }), t('lms.confirmDelete'), {
        type: 'warning',
        confirmButtonText: t('lms.deleteCourse'),
        cancelButtonText: t('lms.cancel'),
    })
    try {
        const response = await deleteCourse(row.id)
        ElMessage.success(response.message)
        if (rows.value.length === 1 && query.page > 0) query.page -= 1
        load()
    } catch {
        // The shared request wrapper displays the localized backend message.
        // Keep the current table unchanged because the course was not deleted.
    }
}

async function exportExcel() {
    exporting.value = true
    try {
        const response = await exportCourses(query)
        downloadBlob(response.data, t('lms.courseExportFilename'))
    } catch {
        // blobRequest already displays the localized backend message.
    } finally {
        exporting.value = false
    }
}

function formatPrice(price: number) {
    return `${Number(price || 0).toLocaleString(locale.value === 'en' ? 'en-US' : 'vi-VN')} ₫`
}

function formatDate(value?: string) {
    if (!value) return t('lms.notConfigured')
    return new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'vi-VN').format(new Date(`${value}T00:00:00`))
}

function courseState(course: Course): { label: string; type: 'success' | 'info' | 'warning' } {
    const today = new Date().toISOString().slice(0, 10)
    if (course.endDate && course.endDate < today) return { label: t('lms.courseStateEnded'), type: 'info' }
    if (course.startDate && course.startDate > today) return { label: t('lms.courseStateUpcoming'), type: 'warning' }
    return { label: t('lms.courseStateRunning'), type: 'success' }
}

onMounted(load)
</script>

<style scoped>
.course-date-filter {
    width: 310px;
}
.course-toolbar-actions {
    display: flex;
    gap: 10px;
}
.course-cell {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 13px;
}
.course-cell :deep(.el-image),
.course-cell__placeholder {
    width: 74px;
    height: 48px;
    flex: 0 0 74px;
    border-radius: 9px;
}
.course-cell__placeholder {
    display: grid;
    place-items: center;
    background: linear-gradient(135deg, #dbeafe, #eff6ff);
    color: #2563eb;
    font-size: 19px;
    font-weight: 800;
}
.course-cell__content,
.course-period {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 4px;
}
.course-cell__content strong {
    overflow: hidden;
    color: #172033;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.course-cell__content span,
.course-period small {
    color: #718096;
    font-size: 12px;
}
.course-price {
    color: #1d4ed8;
    font-weight: 700;
}
.course-period span {
    color: #344054;
    font-weight: 600;
}
@media (max-width: 900px) {
    .course-toolbar-actions {
        width: 100%;
        justify-content: flex-end;
    }
    .course-date-filter {
        width: 100%;
    }
}
@media (max-width: 560px) {
    .course-toolbar-actions,
    .course-toolbar-actions :deep(.el-button) {
        width: 100%;
    }
}
</style>
