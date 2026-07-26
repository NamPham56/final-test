<template>
    <div class="lms-page lms-form-page course-form-page">
        <div class="lms-page-heading">
            <div>
                <el-breadcrumb separator="/">
                    <el-breadcrumb-item :to="{ name: 'lmsCourses' }">{{ $t('lms.courses') }}</el-breadcrumb-item>
                    <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
                </el-breadcrumb>
                <h1>{{ pageTitle }}</h1>
                <p>{{ pageDescription }}</p>
            </div>
            <el-tag v-if="!isCreate && course" :type="state.type" effect="light" round>{{ state.label }}</el-tag>
        </div>

        <el-skeleton v-if="loading" :rows="9" animated class="course-loading" />
        <el-result v-else-if="notFound" icon="warning" :title="$t('lms.noCourses')" :sub-title="$t('lms.noData')">
            <template #extra><el-button type="primary" @click="backToList">{{ $t('lms.backToList') }}</el-button></template>
        </el-result>

        <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" status-icon @submit.prevent>
            <div class="lms-form-layout">
                <aside class="lms-summary-card course-summary">
                    <div class="course-cover-wrap">
                        <el-image v-if="coverUrl" :src="coverUrl" fit="cover" class="course-cover" preview-teleported />
                        <div v-else class="course-cover course-cover--empty">
                            <el-icon><Picture /></el-icon>
                            <span>{{ $t('lms.noImage') }}</span>
                        </div>
                        <el-upload
                            v-if="!isDetail"
                            class="course-cover-upload"
                            :auto-upload="false"
                            :show-file-list="false"
                            multiple
                            accept="image/*"
                            :on-change="selectThumbnail"
                        >
                            <el-button circle type="primary" :aria-label="$t('lms.imagesAndDocuments')"
                                ><el-icon><Camera /></el-icon
                            ></el-button>
                        </el-upload>
                    </div>
                    <h2>{{ form.courseName || $t('lms.newCourse') }}</h2>
                    <span class="course-code">{{ form.courseCode || $t('lms.noCourseCode') }}</span>
                    <el-tag :type="state.type" effect="light" round>{{ state.label }}</el-tag>

                    <div class="course-quick-info">
                        <div>
                            <span class="course-quick-info__icon"
                                ><el-icon><Money /></el-icon
                            ></span>
                            <p>
                                <small>{{ $t('lms.courseFee') }}</small><strong>{{ formattedPrice }}</strong>
                            </p>
                        </div>
                        <div>
                            <span class="course-quick-info__icon"
                                ><el-icon><Calendar /></el-icon
                            ></span>
                            <p>
                                <small>{{ $t('lms.period') }}</small><strong>{{ periodLabel }}</strong>
                            </p>
                        </div>
                        <div>
                            <span class="course-quick-info__icon"
                                ><el-icon><Files /></el-icon
                            ></span>
                            <p>
                                <small>{{ $t('lms.mediaFiles') }}</small><strong>{{ existingMedia.length }} {{ $t('lms.files') }}</strong>
                            </p>
                        </div>
                    </div>
                </aside>

                <section class="lms-form-card">
                    <el-tabs v-model="activeTab" class="lms-form-tabs">
                        <el-tab-pane :label="$t('lms.generalInfo')" name="general">
                            <div class="form-section-heading">
                                <div>
                                    <h3>{{ $t('lms.courseInfo') }}</h3>
                                    <p>{{ $t('lms.courseListSubtitle') }}</p>
                                </div>
                            </div>
                            <div class="lms-form-grid">
                                <el-form-item :label="$t('lms.courseCode')" prop="courseCode">
                                    <el-input
                                        v-model="form.courseCode"
                                        maxlength="50"
                                        show-word-limit
                                        :disabled="isDetail"
                                        :placeholder="$t('lms.courseCodePlaceholder')"
                                    />
                                </el-form-item>
                                <el-form-item :label="$t('lms.name')" prop="courseName">
                                    <el-input v-model="form.courseName" maxlength="200" :disabled="isDetail" :placeholder="$t('lms.name')" />
                                </el-form-item>
                                <el-form-item :label="$t('lms.courseFee')" prop="price">
                                    <el-input-number v-model="form.price" :min="0" :step="100000" :disabled="isDetail" controls-position="right" />
                                </el-form-item>
                                <el-form-item class="form-grid-full" :label="$t('lms.description')" prop="description">
                                    <el-input
                                        v-model="form.description"
                                        type="textarea"
                                        :rows="6"
                                        maxlength="2000"
                                        show-word-limit
                                        :disabled="isDetail"
                                        :placeholder="$t('lms.courseDescriptionPlaceholder')"
                                    />
                                </el-form-item>
                            </div>
                        </el-tab-pane>

                        <el-tab-pane :label="$t('lms.period')" name="schedule">
                            <div class="form-section-heading">
                                <div>
                                    <h3>{{ $t('lms.courseScheduleTitle') }}</h3>
                                    <p>{{ $t('lms.courseScheduleDescription') }}</p>
                                </div>
                            </div>
                            <div class="lms-form-grid">
                                <el-form-item :label="$t('lms.startDate')" prop="startDate">
                                    <el-date-picker
                                        v-model="form.startDate"
                                        type="date"
                                        value-format="YYYY-MM-DD"
                                        :disabled="isDetail"
                                        :placeholder="$t('lms.startDate')"
                                    />
                                </el-form-item>
                                <el-form-item :label="$t('lms.endDate')" prop="endDate">
                                    <el-date-picker
                                        v-model="form.endDate"
                                        type="date"
                                        value-format="YYYY-MM-DD"
                                        :disabled="isDetail"
                                        :placeholder="$t('lms.endDate')"
                                    />
                                </el-form-item>
                            </div>
                            <div class="schedule-note">
                                <el-icon><InfoFilled /></el-icon>
                                <p>{{ $t('lms.courseScheduleHint') }}</p>
                            </div>
                        </el-tab-pane>

                        <el-tab-pane :label="$t('lms.imagesAndDocuments')" name="media">
                            <div class="form-section-heading">
                                <div>
                                    <h3>{{ $t('lms.courseMediaTitle') }}</h3>
                                    <p>{{ $t('lms.courseMediaDescription') }}</p>
                                </div>
                            </div>
                            <MediaList v-if="existingMedia.length" :model-value="existingMedia" :editable="!isDetail" @remove="removeMedia" />
                            <el-empty v-else :description="$t('lms.noCourseMedia')" :image-size="88" />
                            <MediaChangeArrayValidator v-if="!isDetail" :model="form" @validate-field="validateField" />

                            <div v-if="!isDetail" class="media-upload-panel">
                                <div class="media-upload-box">
                                    <h4>{{ $t('lms.courseThumbnail') }}</h4>
                                    <el-upload drag :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="selectThumbnail">
                                        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                                        <div class="el-upload__text">{{ $t('lms.dragImageChoose') }}</div>
                                        <template #tip><div class="el-upload__tip">{{ $t('lms.singleThumbnailHint') }}</div></template>
                                    </el-upload>
                                    <SelectedMediaPreview
                                        :title="$t('lms.selectedThumbnail')"
                                        :items="thumbnailPreviewItems"
                                        editable
                                        @remove="clearThumbnail"
                                    />
                                </div>

                                <div class="media-upload-box">
                                    <h4>{{ $t('lms.courseImages') }}</h4>
                                    <el-upload drag multiple :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="selectCourseImage">
                                        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                                        <div class="el-upload__text">{{ $t('lms.dragImagesChoose') }}</div>
                                        <template #tip><div class="el-upload__tip">{{ $t('lms.imageUploadHint') }}</div></template>
                                    </el-upload>
                                    <SelectedMediaPreview :title="$t('lms.selectedNewImages')" :items="imagePreviewItems" editable @remove="removeCourseImage" />
                                </div>

                                <div class="media-upload-box">
                                    <h4>{{ $t('lms.courseVideos') }}</h4>
                                    <el-upload drag multiple :auto-upload="false" :show-file-list="false" accept="video/*" :on-change="selectCourseVideo">
                                        <el-icon class="el-icon--upload"><VideoCamera /></el-icon>
                                        <div class="el-upload__text">{{ $t('lms.dragVideosChoose') }}</div>
                                        <template #tip><div class="el-upload__tip">{{ $t('lms.videoUploadHint') }}</div></template>
                                    </el-upload>
                                    <SelectedMediaPreview :title="$t('lms.selectedNewVideos')" :items="videoPreviewItems" editable @remove="removeCourseVideo" />
                                </div>
                            </div>
                        </el-tab-pane>
                    </el-tabs>

                    <footer class="lms-form-footer">
                        <span v-if="dirty && !isDetail" class="unsaved-indicator">● {{ $t('lms.unsavedChanges') }}</span>
                        <span v-else></span>
                        <div>
                            <el-button :disabled="saving" @click="goBack">{{ $t('lms.back') }}</el-button>
                            <el-button v-if="isDetail" type="primary" @click="goEdit">{{ $t('lms.edit') }}</el-button>
                            <el-button v-else type="primary" :loading="saving" :disabled="saving" @click="save">{{ $t('lms.save') }}</el-button>
                        </div>
                    </footer>
                </section>
            </div>
        </el-form>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Calendar, Camera, Files, InfoFilled, Money, Picture, UploadFilled, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { createCourse, getCourse, updateCourse } from '/@/api/lms/courseApi'
import { mediaContentUrl } from '/@/api/lms/mediaApi'
import MediaList from '/@/components/lms/MediaList.vue'
import MediaChangeArrayValidator from '/@/components/lms/MediaChangeArrayValidator.vue'
import SelectedMediaPreview from '/@/components/lms/SelectedMediaPreview.vue'
import type { MediaInfo } from '/@/types/lms/common'
import type { Course, CourseForm } from '/@/types/lms/course'
import '/@/styles/lms.scss'

type EditorMode = 'create' | 'edit' | 'detail'
interface SelectedUploadFile {
    uid: string
    file: File
    url: string
}

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const formRef = ref<FormInstance>()
const activeTab = ref('general')
const loading = ref(false)
const saving = ref(false)
const notFound = ref(false)
const course = ref<Course>()
const existingMedia = ref<MediaInfo[]>([])
const thumbnail = ref<SelectedUploadFile>()
const images = ref<SelectedUploadFile[]>([])
const videos = ref<SelectedUploadFile[]>([])
const thumbnailPreview = ref('')
const snapshot = ref('')
let allowLeave = false

const emptyForm = (): CourseForm => ({
    courseCode: '',
    courseName: '',
    description: '',
    price: 0,
    startDate: undefined,
    endDate: undefined,
    retainedMediaIds: [],
    newMediaIds: [],
    removedMediaIds: [],
})
const form = reactive<CourseForm>(emptyForm())
const mode = computed<EditorMode>(() => (route.meta.editorMode as EditorMode) || 'create')
const isCreate = computed(() => mode.value === 'create')
const isDetail = computed(() => mode.value === 'detail')
const courseId = computed(() => Number(route.params.id || 0))
const pageTitle = computed(() => (isCreate.value ? t('lms.addCourse') : isDetail.value ? t('lms.detail') : t('lms.edit')))
const pageDescription = computed(() =>
    isDetail.value ? t('lms.courseDetailDescription') : t('lms.courseEditorDescription')
)
const coverUrl = computed(() => thumbnailPreview.value || (existingMedia.value[0] ? mediaContentUrl(existingMedia.value[0].mediaId) : ''))
const thumbnailPreviewItems = computed(() => (thumbnail.value ? [toPreviewItem(thumbnail.value)] : []))
const imagePreviewItems = computed(() => images.value.map(toPreviewItem))
const videoPreviewItems = computed(() => videos.value.map(toPreviewItem))
const formattedPrice = computed(() => `${Number(form.price || 0).toLocaleString(locale.value === 'en' ? 'en-US' : 'vi-VN')} ₫`)
const periodLabel = computed(() => {
    if (!form.startDate && !form.endDate) return t('lms.notConfigured')
    return `${shortDate(form.startDate)} – ${shortDate(form.endDate)}`
})
const state = computed<{ label: string; type: 'success' | 'info' | 'warning' }>(() => {
    if (isCreate.value) return { label: t('lms.draft'), type: 'info' }
    const today = new Date().toISOString().slice(0, 10)
    if (form.endDate && form.endDate < today) return { label: t('lms.courseStateEnded'), type: 'info' }
    if (form.startDate && form.startDate > today) return { label: t('lms.courseStateUpcoming'), type: 'warning' }
    return { label: t('lms.courseStateRunning'), type: 'success' }
})
const serialisedState = computed(() =>
    JSON.stringify({
        form,
        media: existingMedia.value.map((item) => item.mediaId),
        thumbnail: thumbnail.value ? fileSignature(thumbnail.value.file) : '',
        images: images.value.map((item) => fileSignature(item.file)),
        videos: videos.value.map((item) => fileSignature(item.file)),
    })
)
const dirty = computed(() => !isDetail.value && Boolean(snapshot.value) && snapshot.value !== serialisedState.value)

const validateEndDate = (_rule: unknown, value: string | undefined, callback: (error?: Error) => void) => {
    if (value && form.startDate && value < form.startDate) callback(new Error(t('lms.endDateBeforeStart')))
    else callback()
}
const validatePrice = (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
    Number(value ?? 0) >= 0 ? callback() : callback(new Error(t('lms.priceNonNegative')))
}
const rules: FormRules<CourseForm> = {
    courseCode: [{ required: true, message: t('lms.courseCodeRequired'), trigger: 'blur' }],
    courseName: [{ required: true, message: t('lms.courseNameRequired'), trigger: 'blur' }],
    price: [
        { required: true, message: t('lms.priceRequired'), trigger: 'change' },
        { validator: validatePrice, trigger: 'change' },
    ],
    endDate: [{ validator: validateEndDate, trigger: 'change' }],
}

function mapCourseToForm(value: Course): CourseForm {
    return {
        courseCode: value.courseCode,
        courseName: value.courseName,
        description: value.description || '',
        price: Number(value.price || 0),
        startDate: value.startDate,
        endDate: value.endDate,
        retainedMediaIds: (value.media || []).map((item) => item.mediaId),
        newMediaIds: [],
        removedMediaIds: [],
    }
}

async function markClean() {
    await nextTick()
    snapshot.value = serialisedState.value
}

async function initialise() {
    allowLeave = false
    notFound.value = false
    activeTab.value = 'general'
    clearSelectedFiles()
    course.value = undefined
    existingMedia.value = []
    Object.assign(form, emptyForm())

    if (isCreate.value) {
        await markClean()
        return
    }

    loading.value = true
    try {
        const response = await getCourse(courseId.value)
        const resolved = response.data
        if (!resolved) {
            notFound.value = true
            return
        }
        course.value = resolved
        existingMedia.value = [...(resolved.media || [])]
        Object.assign(form, mapCourseToForm(resolved))
        await markClean()
    } finally {
        loading.value = false
    }
}

function selectThumbnail(uploadFile: UploadFile) {
    if (!uploadFile.raw) return
    const file = uploadFile.raw
    if (!file.type.startsWith('image/')) {
        ElMessage.warning(t('lms.invalidImageType'))
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning(t('lms.imageTooLarge5Mb'))
        return
    }
    clearThumbnail()
    thumbnail.value = createSelectedFile(file)
    thumbnailPreview.value = thumbnail.value.url
    activeTab.value = 'media'
}

function selectCourseImage(uploadFile: UploadFile) {
    const file = uploadFile.raw
    if (!file) return
    if (!file.type.startsWith('image/')) return void ElMessage.warning(t('lms.invalidImageType'))
    if (file.size > 5 * 1024 * 1024) return void ElMessage.warning(t('lms.imageTooLarge5Mb'))
    images.value.push(createSelectedFile(file))
}

function selectCourseVideo(uploadFile: UploadFile) {
    const file = uploadFile.raw
    if (!file) return
    if (!file.type.startsWith('video/')) return void ElMessage.warning(t('lms.invalidVideoType'))
    if (file.size > 100 * 1024 * 1024) return void ElMessage.warning(t('lms.videoTooLarge100Mb'))
    videos.value.push(createSelectedFile(file))
}

function clearThumbnail() {
    if (thumbnail.value) URL.revokeObjectURL(thumbnail.value.url)
    thumbnail.value = undefined
    thumbnailPreview.value = ''
}

function removeCourseImage(uid: string) {
    removeSelectedFile(images.value, uid)
}

function removeCourseVideo(uid: string) {
    removeSelectedFile(videos.value, uid)
}

function clearSelectedFiles() {
    clearThumbnail()
    images.value.forEach((item) => URL.revokeObjectURL(item.url))
    videos.value.forEach((item) => URL.revokeObjectURL(item.url))
    images.value = []
    videos.value = []
}

function removeMedia(item: MediaInfo) {
    existingMedia.value = existingMedia.value.filter((media) => media.mediaId !== item.mediaId)
    form.retainedMediaIds = form.retainedMediaIds.filter((id) => id !== item.mediaId)
    if (!form.removedMediaIds.includes(item.mediaId)) form.removedMediaIds.push(item.mediaId)
}

function validateField(prop: string) {
    formRef.value?.validateField(prop)
}

async function save() {
    if (!formRef.value) return
    try {
        await formRef.value.validate()
    } catch {
        activeTab.value = 'general'
        ElMessage.warning(t('lms.formInvalid'))
        return
    }

    saving.value = true
    try {
        const response = isCreate.value
            ? await createCourse(form, thumbnail.value?.file, images.value.map((item) => item.file), videos.value.map((item) => item.file))
            : await updateCourse(courseId.value, form, thumbnail.value?.file, images.value.map((item) => item.file), videos.value.map((item) => item.file))
        ElMessage.success(response.message)
        await markClean()
        allowLeave = true
        await router.replace({ name: 'lmsCourseDetail', params: { id: response.data.id } })
    } finally {
        saving.value = false
    }
}

function goEdit() {
    router.push({ name: 'lmsCourseEdit', params: { id: courseId.value } })
}

function goBack() {
    router.push({ name: 'lmsCourses' })
}

function backToList() {
    allowLeave = true
    router.replace({ name: 'lmsCourses' })
}

async function confirmLeave() {
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
}

function shortDate(value?: string) {
    if (!value) return '...'
    return new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'vi-VN').format(new Date(`${value}T00:00:00`))
}

function createSelectedFile(file: File): SelectedUploadFile {
    return {
        uid: `${file.name}-${file.size}-${file.lastModified}-${Math.random()}`,
        file,
        url: URL.createObjectURL(file),
    }
}

function toPreviewItem(item: SelectedUploadFile) {
    return {
        uid: item.uid,
        name: item.file.name,
        size: item.file.size,
        mimeType: item.file.type,
        url: item.url,
    }
}

function removeSelectedFile(items: SelectedUploadFile[], uid: string) {
    const index = items.findIndex((item) => item.uid === uid)
    if (index < 0) return
    URL.revokeObjectURL(items[index].url)
    items.splice(index, 1)
}

function fileSignature(file: File) {
    return `${file.name}:${file.size}:${file.lastModified}`
}

onBeforeRouteLeave(async (_to, _from, next) => {
    if (allowLeave || (await confirmLeave())) next()
    else next(false)
})
watch(() => route.fullPath, initialise, { immediate: true })
onBeforeUnmount(clearSelectedFiles)
</script>

<style scoped>
.course-loading {
    padding: 32px;
    border: 1px solid var(--lms-border);
    border-radius: 16px;
    background: #fff;
}
.course-summary {
    align-self: start;
    padding: 20px;
    text-align: center;
}
.lms-page-heading :deep(.el-breadcrumb) {
    margin-bottom: 9px;
}
.lms-form-tabs :deep(.el-tabs__content) {
    min-height: 430px;
    padding: 0 24px 24px;
}
.course-cover-wrap {
    position: relative;
    margin-bottom: 20px;
}
.course-cover {
    display: block;
    width: 100%;
    aspect-ratio: 16 / 10;
    overflow: hidden;
    border: 5px solid #fff;
    border-radius: 16px;
    box-shadow: 0 10px 30px rgba(30, 64, 175, 0.14);
}
.course-cover--empty {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    gap: 9px;
    background: linear-gradient(145deg, #eff6ff, #f8fafc);
    color: #94a3b8;
}
.course-cover--empty :deep(.el-icon) {
    color: #60a5fa;
    font-size: 38px;
}
.course-cover-upload {
    position: absolute;
    right: 12px;
    bottom: -16px;
}
.course-summary h2 {
    margin: 4px 0 6px;
    color: #172033;
    font-size: 20px;
    line-height: 1.35;
}
.course-code {
    display: block;
    margin-bottom: 12px;
    color: #718096;
    font-size: 13px;
}
.course-quick-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid var(--lms-border);
    text-align: left;
}
.course-quick-info > div {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 10px 5px;
}
.course-quick-info__icon {
    display: grid;
    width: 36px;
    height: 36px;
    flex: 0 0 36px;
    place-items: center;
    border-radius: 10px;
    background: #eff6ff;
    color: #2563eb;
}
.course-quick-info p {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 2px;
    margin: 0;
}
.course-quick-info small {
    color: #718096;
}
.course-quick-info strong {
    overflow: hidden;
    color: #344054;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.form-section-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 22px;
}
.form-section-heading h3 {
    margin: 0 0 5px;
    color: #172033;
    font-size: 17px;
}
.form-section-heading p {
    margin: 0;
    color: #718096;
    font-size: 13px;
}
.form-grid-full {
    grid-column: 1 / -1;
}
.lms-form-grid :deep(.el-input-number),
.lms-form-grid :deep(.el-date-editor) {
    width: 100%;
}
.schedule-note {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-top: 16px;
    padding: 15px;
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    background: #eff6ff;
    color: #1e40af;
}
.schedule-note p {
    margin: 0;
    font-size: 13px;
    line-height: 1.55;
}
.media-upload-panel {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
    margin-top: 22px;
}
.media-upload-box {
    min-width: 0;
    padding: 16px;
    border: 1px solid var(--lms-border);
    border-radius: 12px;
    background: #fbfcfe;
}
.media-upload-box h4 {
    margin: 0 0 13px;
    color: #344054;
}
.media-upload-box :deep(.el-upload),
.media-upload-box :deep(.el-upload-dragger) {
    width: 100%;
}
.unsaved-indicator {
    color: #d97706;
    font-size: 12px;
    font-weight: 600;
}
@media (max-width: 680px) {
    .media-upload-panel {
        grid-template-columns: 1fr;
    }
}
</style>
