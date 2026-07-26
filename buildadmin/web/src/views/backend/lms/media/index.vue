<template>
    <div class="lms-page lms-media-page">
        <section class="lms-hero">
            <div>
                <span class="lms-kicker">{{ $t('lms.resources') }}</span>
                <h1>{{ $t('lms.mediaTitle') }}</h1>
                <p>{{ $t('lms.mediaSubtitle') }}</p>
            </div>
            <div class="lms-metric">
                <strong>{{ uploaded.length }}</strong>
                <span>{{ $t('lms.files') }}</span>
            </div>
        </section>

        <div class="media-workspace">
            <el-card shadow="never" class="media-upload-card">
                <template #header>
                    <div class="media-card-heading">
                        <div class="media-heading-icon">
                            <el-icon><UploadFilled /></el-icon>
                        </div>
                        <div>
                            <h2>{{ $t('lms.newUpload') }}</h2>
                            <p>{{ $t('lms.protectedMedia') }}</p>
                        </div>
                    </div>
                </template>

                <el-form ref="formRef" :model="form" :rules="rules" label-position="top" status-icon>
                    <el-form-item :label="$t('lms.media')" prop="mediaType">
                        <el-select v-model="form.mediaType" :placeholder="$t('lms.media')">
                            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value">
                                <div class="type-option">
                                    <span>{{ item.label }}</span>
                                    <small>{{ item.description }}</small>
                                </div>
                            </el-option>
                        </el-select>
                    </el-form-item>

                    <el-form-item :label="$t('lms.newUpload')" prop="file">
                        <el-upload
                            ref="uploadRef"
                            drag
                            :auto-upload="false"
                            :show-file-list="false"
                            :limit="1"
                            :on-change="selectFile"
                            :on-remove="clearFile"
                            :on-exceed="replaceFile"
                            :accept="accept"
                            class="media-uploader"
                        >
                            <el-icon class="upload-icon"><UploadFilled /></el-icon>
                            <div class="upload-title">{{ $t('lms.uploadDropTitle') }}</div>
                            <div class="upload-description">{{ $t('lms.uploadChooseText') }}</div>
                            <template #tip>
                                <span class="upload-tip">{{ $t('lms.uploadPrivacyHint') }}</span>
                            </template>
                        </el-upload>
                    </el-form-item>

                    <div v-if="form.file" class="selected-file">
                        <div class="selected-preview">
                            <el-image v-if="isImage" :src="previewUrl" fit="cover" />
                            <video v-else-if="isVideo" :src="previewUrl" muted />
                            <el-icon v-else><Document /></el-icon>
                        </div>
                        <div class="selected-meta">
                            <strong>{{ form.file.name }}</strong>
                            <span>{{ form.file.type || $t('lms.unknown') }} · {{ formatSize(form.file.size) }}</span>
                        </div>
                        <div class="lms-row-actions">
                            <el-tooltip :content="$t('lms.delete')" placement="top" :show-after="300">
                                <el-button class="lms-row-action is-danger" circle :aria-label="$t('lms.delete')" @click="resetUpload">
                                    <el-icon><Delete /></el-icon>
                                </el-button>
                            </el-tooltip>
                        </div>
                    </div>

                    <el-button class="upload-submit" type="primary" :loading="uploading" :disabled="uploading || !form.file" @click="submit">
                        <el-icon><Upload /></el-icon>
                        {{ $t('lms.uploadToSystem') }}
                    </el-button>
                </el-form>
            </el-card>

            <aside class="media-guide-card">
                <div class="guide-icon">
                    <el-icon><Lock /></el-icon>
                </div>
                <h2>{{ $t('lms.protectedMedia') }}</h2>
                <p>{{ $t('lms.mediaPrivacyDescription') }}</p>

                <div class="guide-list">
                    <div>
                        <span>01</span>
                        <div><strong>{{ $t('lms.securePreview') }}</strong><small>{{ $t('lms.securePreviewDescription') }}</small></div>
                    </div>
                    <div>
                        <span>02</span>
                        <div><strong>{{ $t('lms.uploadOriginalName') }}</strong><small>{{ $t('lms.downloadEndpointDescription') }}</small></div>
                    </div>
                    <div>
                        <span>03</span>
                        <div><strong>{{ $t('lms.flexibleMediaLink') }}</strong><small>{{ $t('lms.flexibleMediaLinkDescription') }}</small></div>
                    </div>
                </div>

                <el-alert :title="$t('lms.noPhysicalUrl')" type="info" :closable="false" show-icon />
            </aside>
        </div>

        <el-card shadow="never" class="media-library-card">
            <template #header>
                <div class="library-heading">
                    <div>
                        <h2>{{ $t('lms.newUpload') }}</h2>
                        <p>{{ $t('lms.sessionUploadResults') }}</p>
                    </div>
                    <el-tag effect="plain" round>{{ uploaded.length }} {{ $t('lms.files') }}</el-tag>
                </div>
            </template>

            <MediaList v-if="uploaded.length" :model-value="uploaded" />
            <el-empty v-else :description="$t('lms.noUploadedMedia')" :image-size="92" />
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { Delete, Document, Lock, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, genFileId, type FormInstance, type FormRules, type UploadFile, type UploadInstance, type UploadRawFile } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { uploadMedia } from '/@/api/lms/mediaApi'
import MediaList from '/@/components/lms/MediaList.vue'
import type { MediaInfo } from '/@/types/lms/common'
import type { MediaType } from '/@/types/lms/media'
import '/@/styles/lms.scss'

interface MediaUploadForm {
    mediaType: MediaType
    file?: File
}
const { t } = useI18n()

const typeOptions = computed<Array<{ value: MediaType; label: string; description: string }>>(() => [
    { value: 'IMAGE', label: t('lms.mediaTypeImage'), description: t('lms.mediaTypeImageDescription') },
    { value: 'AVATAR', label: t('lms.mediaTypeAvatar'), description: t('lms.mediaTypeAvatarDescription') },
    { value: 'THUMBNAIL', label: t('lms.mediaTypeThumbnail'), description: t('lms.mediaTypeThumbnailDescription') },
    { value: 'VIDEO', label: t('lms.mediaTypeVideo'), description: t('lms.mediaTypeVideoDescription') },
    { value: 'DOCUMENT', label: t('lms.mediaTypeDocument'), description: t('lms.mediaTypeDocumentDescription') },
])

const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const form = reactive<MediaUploadForm>({ mediaType: 'IMAGE', file: undefined })
const uploading = ref(false)
const uploaded = ref<MediaInfo[]>([])
const previewUrl = ref('')

const isImage = computed(() => Boolean(form.file?.type.startsWith('image/')))
const isVideo = computed(() => Boolean(form.file?.type.startsWith('video/')))
const accept = computed(() => {
    if (['AVATAR', 'THUMBNAIL', 'IMAGE'].includes(form.mediaType)) return 'image/*'
    if (form.mediaType === 'VIDEO') return 'video/*'
    return '*/*'
})

const rules: FormRules<MediaUploadForm> = {
    mediaType: [{ required: true, message: t('lms.mediaTypeRequired'), trigger: 'change' }],
    file: [
        {
            validator: (_rule, value, callback) => {
                const file = value as File | undefined
                if (!file) callback(new Error(t('lms.fileRequired')))
                else if (file.size > 100 * 1024 * 1024) callback(new Error(t('lms.fileTooLarge100Mb')))
                else callback()
            },
            trigger: 'change',
        },
    ],
}

function validateFile(file?: File) {
    if (!file) return false
    if (file.size > 100 * 1024 * 1024) {
        ElMessage.warning(t('lms.fileTooLarge100Mb'))
        return false
    }
    if (['AVATAR', 'THUMBNAIL', 'IMAGE'].includes(form.mediaType) && !file.type.startsWith('image/')) {
        ElMessage.warning(t('lms.selectedTypeRequiresImage'))
        return false
    }
    if (form.mediaType === 'VIDEO' && !file.type.startsWith('video/')) {
        ElMessage.warning(t('lms.invalidVideoFile'))
        return false
    }
    return true
}

function setPreview(file?: File) {
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = file ? URL.createObjectURL(file) : ''
}

function selectFile(uploadFile: UploadFile) {
    const raw = uploadFile.raw
    if (!validateFile(raw)) {
        resetUpload()
        return
    }
    form.file = raw
    setPreview(raw)
    formRef.value?.validateField('file').catch(() => undefined)
}

function clearFile() {
    form.file = undefined
    setPreview()
}

function replaceFile(files: File[]) {
    const file = files[0] as UploadRawFile | undefined
    if (!file || !validateFile(file)) return
    uploadRef.value?.clearFiles()
    file.uid = genFileId()
    uploadRef.value?.handleStart(file)
}

function resetUpload() {
    uploadRef.value?.clearFiles()
    clearFile()
    formRef.value?.clearValidate('file')
}

function formatSize(bytes: number) {
    if (!bytes) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB']
    const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
    return `${(bytes / 1024 ** index).toFixed(index ? 1 : 0)} ${units[index]}`
}

async function submit() {
    if (!validateFile(form.file)) return
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid || !form.file) return
    uploading.value = true
    try {
        const response = await uploadMedia(form.file, form.mediaType)
        uploaded.value.unshift(response.data)
        ElMessage.success(response.message || t('lms.fileUploadSuccess'))
        resetUpload()
    } finally {
        uploading.value = false
    }
}

onBeforeUnmount(() => setPreview())
</script>

<style scoped lang="scss">
.media-workspace {
    display: grid;
    grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.75fr);
    gap: 18px;
    align-items: stretch;
}

.media-upload-card,
.media-library-card {
    border-radius: 16px;
}

.media-card-heading,
.library-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.media-card-heading {
    justify-content: flex-start;
    gap: 13px;
}

.media-card-heading h2,
.library-heading h2,
.media-guide-card h2 {
    margin: 0 0 4px;
    font-size: 17px;
    color: var(--el-text-color-primary);
}

.media-card-heading p,
.library-heading p,
.media-guide-card > p {
    margin: 0;
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
}

.media-heading-icon,
.guide-icon {
    display: grid;
    width: 40px;
    height: 40px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border-radius: 11px;
    place-items: center;
}

.media-upload-card :deep(.el-form) {
    max-width: 720px;
}

.media-upload-card :deep(.el-select) {
    width: 100%;
}

.type-option {
    display: flex;
    width: 100%;
    justify-content: space-between;
    gap: 24px;
}

.type-option small {
    color: var(--el-text-color-secondary);
}

.media-uploader,
.media-uploader :deep(.el-upload),
.media-uploader :deep(.el-upload-dragger) {
    width: 100%;
}

.media-uploader :deep(.el-upload-dragger) {
    padding: 30px 18px;
    background: var(--el-fill-color-lighter);
    border: 1px dashed var(--el-color-primary-light-5);
    border-radius: 13px;
}

.upload-icon {
    margin-bottom: 9px;
    font-size: 34px;
    color: var(--el-color-primary);
}

.upload-title {
    margin-bottom: 5px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.upload-description,
.upload-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.upload-description strong {
    color: var(--el-color-primary);
}

.selected-file {
    display: flex;
    gap: 13px;
    align-items: center;
    padding: 12px;
    margin: 4px 0 18px;
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
}

.selected-preview {
    display: grid;
    flex: 0 0 auto;
    width: 54px;
    height: 54px;
    overflow: hidden;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border-radius: 9px;
    place-items: center;
}

.selected-preview .el-image,
.selected-preview video {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.selected-preview > .el-icon {
    font-size: 25px;
}

.selected-meta {
    display: grid;
    flex: 1;
    min-width: 0;
    gap: 5px;
}

.selected-meta strong {
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.selected-meta span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.selected-file .lms-row-actions {
    display: flex;
    flex: 0 0 auto;
}

.selected-file :deep(.lms-row-action) {
    width: 32px;
    height: 32px;
    margin: 0;
}

.upload-submit {
    min-width: 180px;
}

.media-guide-card {
    padding: 25px;
    background: linear-gradient(155deg, #172554, #1e3a8a);
    border: 1px solid rgb(255 255 255 / 10%);
    border-radius: 16px;
    box-shadow: 0 12px 30px rgb(30 58 138 / 16%);
}

.media-guide-card .guide-icon {
    margin-bottom: 18px;
    color: #fff;
    background: rgb(255 255 255 / 13%);
}

.media-guide-card h2,
.media-guide-card > p {
    color: #fff;
}

.media-guide-card > p {
    opacity: 0.76;
}

.guide-list {
    display: grid;
    gap: 16px;
    padding: 22px 0;
}

.guide-list > div {
    display: flex;
    gap: 12px;
}

.guide-list > div > span {
    display: grid;
    flex: 0 0 auto;
    width: 31px;
    height: 31px;
    font-size: 11px;
    font-weight: 700;
    color: #bfdbfe;
    background: rgb(255 255 255 / 10%);
    border-radius: 9px;
    place-items: center;
}

.guide-list > div > div {
    display: grid;
    gap: 4px;
}

.guide-list strong {
    font-size: 13px;
    color: #fff;
}

.guide-list small {
    font-size: 11px;
    line-height: 1.5;
    color: #bfdbfe;
}

.media-guide-card :deep(.el-alert) {
    background: rgb(255 255 255 / 9%);
    border: 1px solid rgb(255 255 255 / 11%);
}

.media-guide-card :deep(.el-alert__title),
.media-guide-card :deep(.el-alert__icon) {
    color: #dbeafe;
}

.media-library-card {
    margin-top: 18px;
}

@media (max-width: 960px) {
    .media-workspace {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 560px) {
    .media-card-heading,
    .library-heading {
        align-items: flex-start;
    }

    .library-heading {
        gap: 12px;
    }
}
</style>
