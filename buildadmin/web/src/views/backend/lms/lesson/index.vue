<template>
    <div class="lms-page lms-lesson">
        <section class="lms-hero">
            <div>
                <span class="lms-kicker">{{ $t('lms.app') }} · {{ $t('lms.learningContent') }}</span>
                <h1>{{ $t('lms.lessonListTitle') }}</h1>
                <p>{{ $t('lms.lessonListSubtitle') }}</p>
            </div>
            <div class="lms-metric">
                <strong>{{ allRows.length }}</strong>
                <span>{{ $t('lms.lessons') }}</span>
            </div>
        </section>

        <el-card shadow="never">
            <div class="lms-toolbar lesson-toolbar">
                <div class="lms-filters">
                    <el-select
                        v-model="courseId"
                        class="lesson-course-select"
                        filterable
                        :placeholder="$t('lms.selectCourse')"
                        :loading="loadingCourses"
                        @change="selectCourse"
                    >
                        <el-option
                            v-for="course in courses"
                            :key="course.id"
                            :label="`${course.courseCode} · ${course.courseName}`"
                            :value="course.id"
                        />
                    </el-select>
                    <el-input v-model="keyword" clearable :placeholder="$t('lms.searchLessons')" @keyup.enter="resetPage" @clear="resetPage" />
                </div>
                <el-button type="primary" :disabled="!courseId" @click="goCreate">{{ $t('lms.addLesson') }}</el-button>
            </div>

            <div v-if="currentCourse" class="selected-course">
                <span class="selected-course__icon"
                    ><el-icon><Collection /></el-icon
                ></span>
                <div>
                    <small>{{ $t('lms.courseInfo') }}</small><strong>{{ currentCourse.courseCode }} · {{ currentCourse.courseName }}</strong>
                </div>
                <el-button link type="primary" @click="goCourseDetail">{{ $t('lms.detail') }}</el-button>
            </div>

            <el-table v-loading="loading" :data="rows" row-key="id">
                <el-table-column :label="$t('lms.order')" width="95" align="center">
                    <template #default="{ row }"
                        ><span class="lesson-order">{{ row.lessonOrder }}</span></template
                    >
                </el-table-column>
                <el-table-column :label="$t('lms.lessons')" min-width="310">
                    <template #default="{ row }">
                        <div class="lesson-cell">
                            <div class="lesson-cell__cover">
                                <el-image v-if="lessonThumbnail(row)" :src="lessonThumbnail(row)" fit="cover" />
                                <el-icon v-else><Reading /></el-icon>
                            </div>
                            <div>
                                <strong>{{ row.title }}</strong
                                ><span>{{ row.lessonCode || $t('lms.noLessonCode') }}</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.duration')" width="150">
                    <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
                </el-table-column>
                <el-table-column :label="$t('lms.resources')" width="160">
                    <template #default="{ row }">
                        <div class="lesson-assets">
                            <el-tag v-if="videoCount(row)" type="success" effect="plain" round>{{ videoCount(row) }} {{ $t('lms.videos') }}</el-tag>
                            <el-tag v-if="imageCount(row)" effect="plain" round>{{ imageCount(row) }} {{ $t('lms.imagesAndDocuments') }}</el-tag>
                            <span v-if="!row.media?.length">{{ $t('lms.noData') }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :label="$t('lms.status')" width="130">
                    <template #default><el-tag type="success" effect="light" round>{{ $t('lms.active') }}</el-tag></template>
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
                <template #empty>
                    <el-empty :description="courseId ? $t('lms.noLessons') : $t('lms.selectCourse')" />
                </template>
            </el-table>

            <div v-if="filteredRows.length" class="lms-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50]"
                    :total="filteredRows.length"
                    layout="total, sizes, prev, pager, next"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Delete, EditPen, Reading, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { listAllCourses } from '/@/api/lms/courseApi'
import { deleteLesson, listLessons } from '/@/api/lms/lessonApi'
import { mediaContentUrl } from '/@/api/lms/mediaApi'
import type { Course } from '/@/types/lms/course'
import type { Lesson } from '/@/types/lms/lesson'
import '/@/styles/lms.scss'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const courses = ref<Course[]>([])
const courseId = ref<number>()
const allRows = ref<Lesson[]>([])
const loading = ref(false)
const loadingCourses = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const currentCourse = computed(() => courses.value.find((course) => course.id === courseId.value))
const filteredRows = computed(() => {
    const searchLocale = locale.value === 'en' ? 'en-US' : 'vi-VN'
    const value = keyword.value.trim().toLocaleLowerCase(searchLocale)
    if (!value) return allRows.value
    return allRows.value.filter(
        (lesson) => lesson.title.toLocaleLowerCase(searchLocale).includes(value) || lesson.lessonCode?.toLocaleLowerCase(searchLocale).includes(value)
    )
})
const rows = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredRows.value.slice(start, start + pageSize.value)
})

watch(keyword, resetPage)
watch(pageSize, resetPage)

async function initialise() {
    loadingCourses.value = true
    try {
        courses.value = await listAllCourses()
        const routeCourseId = Number(route.params.id || route.query.courseId || 0)
        courseId.value = courses.value.some((course) => course.id === routeCourseId) ? routeCourseId : courses.value[0]?.id
        await load()
    } finally {
        loadingCourses.value = false
    }
}

async function load() {
    if (!courseId.value) {
        allRows.value = []
        return
    }
    loading.value = true
    try {
        allRows.value = (await listLessons(courseId.value)).data.sort((a, b) => a.lessonOrder - b.lessonOrder)
        resetPage()
    } finally {
        loading.value = false
    }
}

async function selectCourse(value: number) {
    if (route.name === 'lmsCourseLessons') {
        await router.replace({ name: 'lmsCourseLessons', params: { id: value } })
    } else {
        await router.replace({ name: 'lmsLessons', query: { courseId: value } })
    }
    await load()
}

function resetPage() {
    currentPage.value = 1
}

function goCreate() {
    router.push({ name: 'lmsLessonCreate', query: { courseId: courseId.value } })
}

function goDetail(row: Lesson) {
    router.push({ name: 'lmsLessonDetail', params: { id: row.id }, query: { courseId: courseId.value } })
}

function goEdit(row: Lesson) {
    router.push({ name: 'lmsLessonEdit', params: { id: row.id }, query: { courseId: courseId.value } })
}

function goCourseDetail() {
    if (courseId.value) router.push({ name: 'lmsCourseDetail', params: { id: courseId.value } })
}

async function remove(row: Lesson) {
    await ElMessageBox.confirm(t('lms.deleteLessonConfirm', { name: row.title }), t('lms.confirmDelete'), {
        type: 'warning',
        confirmButtonText: t('lms.deleteLesson'),
        cancelButtonText: t('lms.cancel'),
    })
    const response = await deleteLesson(row.id)
    ElMessage.success(response.message)
    await load()
}

function lessonThumbnail(lesson: Lesson) {
    const media = lesson.media?.find((item) => item.mimeType?.startsWith('image/'))
    return media ? mediaContentUrl(media.mediaId) : ''
}

function videoCount(lesson: Lesson) {
    return lesson.media?.filter((item) => item.mimeType?.startsWith('video/')).length || 0
}

function imageCount(lesson: Lesson) {
    return lesson.media?.filter((item) => item.mimeType?.startsWith('image/')).length || 0
}

function formatDuration(value?: number) {
    const seconds = Number(value || 0)
    if (!seconds) return t('lms.notConfigured')
    const minutes = Math.floor(seconds / 60)
    const remainder = seconds % 60
    return minutes
        ? `${minutes} ${t('lms.minutesShort')}${remainder ? ` ${remainder} ${t('lms.secondsShort')}` : ''}`
        : `${remainder} ${t('lms.secondsShort')}`
}

onMounted(initialise)
</script>

<style scoped>
.lesson-course-select {
    width: 340px;
}
.selected-course {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 18px;
    padding: 13px 15px;
    border: 1px solid #dbeafe;
    border-radius: 11px;
    background: #f7faff;
}
.selected-course__icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 38px;
    place-items: center;
    border-radius: 10px;
    background: #dbeafe;
    color: #2563eb;
}
.selected-course > div {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 3px;
}
.selected-course small {
    color: #718096;
}
.selected-course strong {
    overflow: hidden;
    color: #1e3a8a;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.lesson-order {
    display: inline-grid;
    width: 32px;
    height: 32px;
    place-items: center;
    border-radius: 9px;
    background: #eff6ff;
    color: #2563eb;
    font-weight: 800;
}
.lesson-cell {
    display: flex;
    align-items: center;
    gap: 13px;
}
.lesson-cell__cover {
    display: grid;
    width: 72px;
    height: 46px;
    flex: 0 0 72px;
    overflow: hidden;
    place-items: center;
    border-radius: 9px;
    background: #eff6ff;
    color: #3b82f6;
    font-size: 22px;
}
.lesson-cell__cover :deep(.el-image) {
    width: 100%;
    height: 100%;
}
.lesson-cell > div:last-child {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 4px;
}
.lesson-cell strong {
    overflow: hidden;
    color: #172033;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.lesson-cell span,
.lesson-assets > span {
    color: #718096;
    font-size: 12px;
}
.lesson-assets {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
}
@media (max-width: 760px) {
    .lesson-course-select {
        width: 100%;
    }
    .selected-course {
        align-items: flex-start;
        flex-wrap: wrap;
    }
}
</style>
