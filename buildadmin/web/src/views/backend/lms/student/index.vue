<template>
    <div class="lms-page lms-student-list">
        <section class="lms-page-heading">
            <div>
                <span class="lms-kicker">{{ $t('lms.app') }}</span>
                <h1>{{ $t('lms.studentListTitle') }}</h1>
                <p>{{ $t('lms.studentListSubtitle') }}</p>
            </div>
            <div class="student-heading__actions">
                <el-button :icon="Download" :loading="exporting" @click="exportExcel">{{ $t('lms.exportExcel') }}</el-button>
                <el-button type="primary" :icon="Plus" @click="goCreate">{{ $t('lms.addStudent') }}</el-button>
            </div>
        </section>

        <el-card class="student-list-card" shadow="never">
            <div class="student-filter">
                <div class="student-filter__copy">
                    <h2>{{ $t('lms.students') }}</h2>
                    <p>{{ total.toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') }} {{ $t('lms.active') }}</p>
                </div>
                <el-form class="student-filter__form" inline @submit.prevent="search">
                    <el-form-item>
                        <el-input
                            v-model="query.keyword"
                            :prefix-icon="Search"
                            clearable
                            :placeholder="$t('lms.searchStudents')"
                            @clear="search"
                            @keyup.enter="search"
                        />
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" :icon="Search" @click="search">{{ $t('lms.search') }}</el-button>
                        <el-button :icon="Refresh" @click="resetFilter">{{ $t('lms.reset') }}</el-button>
                    </el-form-item>
                </el-form>
            </div>

            <el-table v-loading="loading" :data="rows" row-key="id" class="student-table">
                <template #empty>
                    <el-empty :description="$t('lms.noStudents')" :image-size="92" />
                </template>
                <el-table-column :label="$t('lms.student')" min-width="260">
                    <template #default="{ row }">
                        <div class="student-cell">
                            <el-avatar :size="42" :src="row.media?.[0] ? mediaContentUrl(row.media[0].mediaId) : undefined">
                                {{ initials(row.fullName) }}
                            </el-avatar>
                            <div>
                                <strong>{{ row.fullName }}</strong>
                                <span>{{ row.studentCode }}</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="Email" min-width="220" show-overflow-tooltip />
                <el-table-column prop="phone" :label="$t('lms.phone')" min-width="150">
                    <template #default="{ row }">{{ row.phone || '—' }}</template>
                </el-table-column>
                <el-table-column :label="$t('lms.status')" width="150">
                    <template #default><el-tag type="success" effect="light" round>{{ $t('lms.active') }}</el-tag></template>
                </el-table-column>
                <el-table-column :label="$t('lms.actions')" width="144" fixed="right" align="right">
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
            </el-table>

            <div class="lms-pagination">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="query.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="total"
                    background
                    layout="total, sizes, prev, pager, next"
                    @size-change="changePageSize"
                    @current-change="load"
                />
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, Download, EditPen, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { deleteStudent, exportStudents, searchStudents } from '/@/api/lms/studentApi'
import { downloadBlob } from '/@/api/lms/client'
import { mediaContentUrl } from '/@/api/lms/mediaApi'
import type { Student } from '/@/types/lms/student'
import '/@/styles/lms.scss'

const router = useRouter()
const { t, locale } = useI18n()
const rows = ref<Student[]>([])
const loading = ref(false)
const exporting = ref(false)
const total = ref(0)
const query = reactive({ keyword: '', page: 0, size: 20 })
const currentPage = computed({ get: () => query.page + 1, set: (value) => (query.page = value - 1) })

async function load() {
    loading.value = true
    try {
        const response = await searchStudents(query)
        rows.value = response.data.items
        total.value = response.data.totalItems
    } finally {
        loading.value = false
    }
}

function search() {
    query.page = 0
    void load()
}

function resetFilter() {
    query.keyword = ''
    search()
}

function changePageSize() {
    query.page = 0
    void load()
}

function routeState(row: Student) {
    return { student: JSON.stringify(row) }
}

function goCreate() {
    void router.push({ name: 'lmsStudentCreate' })
}

function goDetail(row: Student) {
    void router.push({ name: 'lmsStudentDetail', params: { id: row.id }, state: routeState(row) })
}

function goEdit(row: Student) {
    void router.push({ name: 'lmsStudentEdit', params: { id: row.id }, state: routeState(row) })
}

async function remove(row: Student) {
    try {
        await ElMessageBox.confirm(t('lms.deleteStudentConfirm', { name: row.fullName }), t('lms.confirmDelete'), {
            type: 'warning',
            confirmButtonText: t('lms.deleteStudent'),
            cancelButtonText: t('lms.cancel'),
        })
        const response = await deleteStudent(row.id)
        ElMessage.success(response.message)
        if (rows.value.length === 1 && query.page > 0) query.page -= 1
        await load()
    } catch (error) {
        if (error !== 'cancel' && error !== 'close') throw error
    }
}

async function exportExcel() {
    exporting.value = true
    try {
        const response = await exportStudents(query.keyword)
        downloadBlob(response.data, t('lms.studentExportFilename'))
        ElMessage.success(t('lms.exportExcel'))
    } catch {
        // blobRequest already displays the localized backend message.
    } finally {
        exporting.value = false
    }
}

function initials(name?: string) {
    return (
        name
            ?.trim()
            .split(/\s+/)
            .slice(-2)
            .map((part) => part[0])
            .join('')
            .toUpperCase() || t('lms.studentInitialFallback')
    )
}

onMounted(load)
</script>

<style scoped>
.student-heading__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}
.student-list-card {
    border-radius: 16px;
}
.student-filter {
    display: flex;
    gap: 24px;
    align-items: flex-end;
    justify-content: space-between;
    margin-bottom: 20px;
}
.student-filter__copy h2 {
    margin: 0 0 4px;
    color: #172033;
    font-size: 18px;
}
.student-filter__copy p {
    margin: 0;
    color: #8490a3;
    font-size: 13px;
}
.student-filter__form {
    display: flex;
    justify-content: flex-end;
    margin-left: auto;
}
.student-filter__form :deep(.el-form-item) {
    margin-right: 10px;
    margin-bottom: 0;
}
.student-filter__form :deep(.el-input) {
    width: min(380px, 34vw);
}
.student-cell {
    display: flex;
    gap: 12px;
    align-items: center;
}
.student-cell :deep(.el-avatar) {
    flex: none;
    border: 2px solid #fff;
    background: #dbeafe;
    color: #1d4ed8;
    font-weight: 700;
    box-shadow: 0 2px 8px rgba(31, 45, 61, 0.14);
}
.student-cell > div {
    display: flex;
    min-width: 0;
    flex-direction: column;
}
.student-cell strong {
    overflow: hidden;
    color: #273449;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.student-cell span {
    margin-top: 3px;
    color: #8a94a6;
    font-size: 12px;
}
@media (max-width: 900px) {
    .student-filter {
        align-items: stretch;
        flex-direction: column;
    }
    .student-filter__form {
        justify-content: flex-start;
        margin-left: 0;
    }
    .student-filter__form :deep(.el-input) {
        width: min(440px, 55vw);
    }
}
@media (max-width: 600px) {
    .student-heading__actions,
    .student-heading__actions :deep(.el-button) {
        width: 100%;
    }
    .student-heading__actions :deep(.el-button + .el-button) {
        margin-left: 0;
    }
    .student-filter__form {
        display: grid;
        width: 100%;
    }
    .student-filter__form :deep(.el-form-item) {
        margin: 0 0 10px;
    }
    .student-filter__form :deep(.el-input),
    .student-filter__form :deep(.el-button) {
        width: 100%;
    }
}
</style>
