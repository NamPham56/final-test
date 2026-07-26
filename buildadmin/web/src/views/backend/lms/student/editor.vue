<template>
    <div class="lms-page lms-form-page student-editor-page">
        <section class="lms-page-heading student-editor-heading">
            <div class="student-editor-heading__main">
                <el-button class="student-editor-heading__back" :icon="ArrowLeft" circle :aria-label="$t('lms.back')" @click="goBack" />
                <div>
                    <el-breadcrumb separator="/">
                        <el-breadcrumb-item :to="{ name: 'lmsStudents' }">{{ $t('lms.students') }}</el-breadcrumb-item>
                        <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
                    </el-breadcrumb>
                    <h1>{{ pageTitle }}</h1>
                    <p>{{ pageDescription }}</p>
                </div>
            </div>
            <el-button v-if="isDetail && studentId" type="primary" :icon="EditPen" @click="goEdit">{{ $t('lms.editProfile') }}</el-button>
        </section>

        <el-skeleton :loading="loading" animated>
            <template #template>
                <div class="student-editor-skeleton">
                    <el-skeleton-item variant="rect" />
                    <el-skeleton-item variant="rect" />
                </div>
            </template>

            <template #default>
                <el-card v-if="loadError" class="student-editor-empty" shadow="never">
                    <el-empty :description="loadError" :image-size="110">
                        <el-button @click="goBack">{{ $t('lms.backToList') }}</el-button>
                        <el-button type="primary" @click="initialize">{{ $t('lms.reload') }}</el-button>
                    </el-empty>
                </el-card>

                <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" status-icon @submit.prevent="save">
                    <div class="lms-form-layout student-editor-layout">
                        <ProfileSummaryCard
                            :model="form"
                            :current-avatar="currentAvatar"
                            :preview="preview"
                            :readonly="isDetail"
                            @select-avatar="selectAvatar"
                        />

                        <main class="lms-form-card student-editor-main">
                            <StudentBasicInfoForm
                                :model="form"
                                :media="existingMedia"
                                :readonly="isDetail"
                                @remove-media="removeMedia"
                                @validate-field="validateField"
                            />

                            <footer class="lms-form-footer student-editor-footer">
                                <div class="student-editor-footer__state">
                                    <span v-if="dirty"><i></i>{{ $t('lms.unsavedChanges') }}</span>
                                    <span v-else-if="!isDetail"
                                        ><el-icon><CircleCheck /></el-icon>{{ $t('lms.dataSynchronized') }}</span
                                    >
                                </div>
                                <div class="student-editor-footer__actions">
                                    <el-button :disabled="saving" @click="goBack">{{ $t('lms.back') }}</el-button>
                                    <el-button v-if="!isDetail" type="primary" native-type="submit" :loading="saving" :disabled="saving">
                                        {{ isCreate ? $t('lms.createStudent') : $t('lms.save') }}
                                    </el-button>
                                </div>
                            </footer>
                        </main>
                    </div>
                </el-form>
            </template>
        </el-skeleton>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, EditPen } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { createStudent, getStudent, updateStudent } from '/@/api/lms/studentApi'
import { mediaContentUrl } from '/@/api/lms/mediaApi'
import ProfileSummaryCard from '/@/components/lms/student/ProfileSummaryCard.vue'
import StudentBasicInfoForm from '/@/components/lms/student/StudentBasicInfoForm.vue'
import type { MediaInfo } from '/@/types/lms/common'
import type { Student, StudentForm } from '/@/types/lms/student'
import '/@/styles/lms.scss'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const initialized = ref(false)
const loadError = ref('')
const avatar = ref<File>()
const preview = ref('')
const existingMedia = ref<MediaInfo[]>([])
const snapshot = ref('')
const bypassGuard = ref(false)

const emptyForm = (): StudentForm => ({
    studentCode: '',
    fullName: '',
    email: '',
    phone: '',
    dateOfBirth: undefined,
    gender: undefined,
    address: '',
    retainedMediaIds: [],
    newMediaIds: [],
    removedMediaIds: [],
})
const form = reactive<StudentForm>(emptyForm())

const isCreate = computed(() => route.name === 'lmsStudentCreate')
const isDetail = computed(() => route.name === 'lmsStudentDetail')
const studentId = computed(() => {
    const value = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
    const id = Number(value)
    return Number.isFinite(id) && id > 0 ? id : undefined
})
const pageTitle = computed(() => (isCreate.value ? t('lms.addStudent') : isDetail.value ? t('lms.detail') : t('lms.edit')))
const pageDescription = computed(() =>
    isCreate.value
        ? t('lms.studentCreateDescription')
        : isDetail.value
          ? t('lms.studentDetailDescription')
          : t('lms.studentEditDescription')
)
const currentAvatar = computed(() => {
    const media = existingMedia.value.find((item) => item.mimeType?.startsWith('image/'))
    return media ? mediaContentUrl(media.mediaId) : undefined
})
const formState = computed(() =>
    JSON.stringify({
        studentCode: form.studentCode,
        fullName: form.fullName,
        email: form.email,
        phone: form.phone || '',
        dateOfBirth: form.dateOfBirth || '',
        gender: form.gender || '',
        address: form.address || '',
        retainedMediaIds: form.retainedMediaIds,
        newMediaIds: form.newMediaIds,
        removedMediaIds: form.removedMediaIds,
        media: existingMedia.value.map((item) => item.mediaId),
        avatar: avatar.value ? `${avatar.value.name}:${avatar.value.size}:${avatar.value.lastModified}` : '',
    })
)
const dirty = computed(() => initialized.value && !isDetail.value && snapshot.value !== formState.value)

const rules: FormRules<StudentForm> = {
    studentCode: [
        { required: true, message: t('lms.studentCodeRequired'), trigger: 'blur' },
        { min: 2, max: 50, message: t('lms.studentCodeLength', { min: 2, max: 50 }), trigger: 'blur' },
    ],
    fullName: [
        { required: true, message: t('lms.fullNameRequired'), trigger: 'blur' },
        { min: 2, max: 150, message: t('lms.fullNameLength', { min: 2, max: 150 }), trigger: 'blur' },
    ],
    email: [
        { required: true, message: t('lms.emailRequired'), trigger: 'blur' },
        { type: 'email', message: t('lms.emailInvalid'), trigger: ['blur', 'change'] },
    ],
    phone: [{ pattern: /^\s*(?:(?:\+?[1-9][0-9]{7,14}|0[0-9]{9,10}))?\s*$/, message: t('lms.phoneInvalid'), trigger: 'change' }],
}

function resetForm() {
    Object.assign(form, emptyForm())
    existingMedia.value = []
    avatar.value = undefined
    revokePreview()
    initialized.value = false
    snapshot.value = ''
    formRef.value?.clearValidate()
}

function applyStudent(student: Student) {
    Object.assign(form, emptyForm(), {
        studentCode: student.studentCode,
        fullName: student.fullName,
        email: student.email,
        phone: student.phone || '',
        dateOfBirth: student.dateOfBirth,
        gender: student.gender,
        address: student.address || '',
        retainedMediaIds: (student.media || []).map((item) => item.mediaId),
        newMediaIds: [],
        removedMediaIds: [],
    })
    existingMedia.value = [...(student.media || [])]
}

function readCachedStudent(): Student | undefined {
    const raw = window.history.state?.student
    if (typeof raw !== 'string') return undefined
    try {
        const student = JSON.parse(raw) as Student
        return student.id === studentId.value ? student : undefined
    } catch {
        return undefined
    }
}

async function markClean() {
    await nextTick()
    snapshot.value = formState.value
    initialized.value = true
}

async function initialize() {
    resetForm()
    loadError.value = ''
    bypassGuard.value = false

    if (isCreate.value) {
        await markClean()
        return
    }
    if (!studentId.value) {
        loadError.value = t('lms.studentIdInvalid')
        return
    }

    loading.value = true
    try {
        const response = await getStudent(studentId.value)
        applyStudent(response.data)
        await markClean()
    } catch (error) {
        loadError.value = error instanceof Error ? error.message : t('lms.studentLoadFailed')
    } finally {
        loading.value = false
    }
}

function selectAvatar(file: UploadFile) {
    if (!file.raw) return
    revokePreview()
    avatar.value = file.raw
    preview.value = URL.createObjectURL(file.raw)
}

function removeMedia(item: MediaInfo) {
    existingMedia.value = existingMedia.value.filter((media) => media.mediaId !== item.mediaId)
    form.retainedMediaIds = form.retainedMediaIds.filter((id) => id !== item.mediaId)
    form.newMediaIds = form.newMediaIds.filter((id) => id !== item.mediaId)
    if (!form.removedMediaIds.includes(item.mediaId)) form.removedMediaIds.push(item.mediaId)
}

function validateField(prop: string) {
    formRef.value?.validateField(prop)
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

async function goBack() {
    if (!(await confirmLeave())) return
    bypassGuard.value = true
    await router.push({ name: 'lmsStudents' })
}

function goEdit() {
    if (!studentId.value) return
    const cached: Student = {
        id: studentId.value,
        ...emptyForm(),
        ...form,
        media: existingMedia.value,
        createdAt: '',
        updatedAt: '',
    }
    bypassGuard.value = true
    void router.push({ name: 'lmsStudentEdit', params: { id: studentId.value }, state: { student: JSON.stringify(cached) } })
}

async function save() {
    if (isDetail.value || saving.value) return
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) {
        ElMessage.warning(t('lms.formInvalid'))
        return
    }

    saving.value = true
    try {
        const response = studentId.value ? await updateStudent(studentId.value, form, avatar.value) : await createStudent(form, avatar.value)
        ElMessage.success(response.message || t(isCreate.value ? 'lms.studentCreateSuccess' : 'lms.studentUpdateSuccess'))
        await markClean()
        bypassGuard.value = true
        await router.replace({
            name: 'lmsStudentDetail',
            params: { id: response.data.id },
            state: { student: JSON.stringify(response.data) },
        })
    } finally {
        saving.value = false
    }
}

function revokePreview() {
    if (!preview.value) return
    URL.revokeObjectURL(preview.value)
    preview.value = ''
}

function beforeUnload(event: BeforeUnloadEvent) {
    if (!dirty.value) return
    event.preventDefault()
    event.returnValue = ''
}

onBeforeRouteLeave(async () => bypassGuard.value || (await confirmLeave()))
watch(() => route.fullPath, initialize, { immediate: true })
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', beforeUnload)
    revokePreview()
})
</script>

<style scoped>
.student-editor-heading__main {
    display: flex;
    gap: 14px;
    align-items: flex-start;
}
.student-editor-heading__back {
    margin-top: 23px;
    flex: none;
}
.student-editor-heading :deep(.el-breadcrumb) {
    margin-bottom: 10px;
}
.student-editor-layout,
.student-editor-skeleton {
    display: grid;
    grid-template-columns: 300px minmax(0, 1fr);
    gap: 22px;
    align-items: start;
}
.student-editor-main {
    overflow: hidden;
    min-width: 0;
    padding: 8px 24px 0;
    border: 1px solid #e4eaf2;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 10px 34px rgba(31, 45, 61, 0.07);
}
.student-editor-skeleton :deep(.el-skeleton__item) {
    height: 580px;
    border-radius: 18px;
}
.student-editor-empty {
    min-height: 440px;
    border-radius: 18px;
}
.student-editor-footer {
    position: sticky;
    z-index: 3;
    bottom: 0;
    display: flex;
    min-height: 72px;
    margin: 12px -24px 0;
    padding: 14px 24px;
    align-items: center;
    justify-content: space-between;
    border-top: 1px solid #e9edf3;
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(9px);
}
.student-editor-footer__state span {
    display: inline-flex;
    gap: 7px;
    align-items: center;
    color: #667085;
    font-size: 12px;
    font-weight: 600;
}
.student-editor-footer__state i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f59e0b;
    box-shadow: 0 0 0 4px #fef3c7;
}
.student-editor-footer__state .el-icon {
    color: #16a34a;
}
.student-editor-footer__actions {
    display: flex;
    gap: 10px;
}
.student-editor-footer__actions :deep(.el-button + .el-button) {
    margin-left: 0;
}
@media (max-width: 960px) {
    .student-editor-layout,
    .student-editor-skeleton {
        grid-template-columns: 1fr;
    }
    .student-editor-skeleton :deep(.el-skeleton__item:first-child) {
        height: 330px;
    }
}
@media (max-width: 600px) {
    .student-editor-heading {
        align-items: stretch;
        flex-direction: column;
    }
    .student-editor-heading__back {
        margin-top: 15px;
    }
    .student-editor-main {
        padding: 6px 14px 0;
    }
    .student-editor-footer {
        align-items: stretch;
        flex-direction: column;
        gap: 12px;
        margin-right: -14px;
        margin-left: -14px;
        padding: 14px;
    }
    .student-editor-footer__actions,
    .student-editor-footer__actions :deep(.el-button) {
        width: 100%;
    }
}
</style>
