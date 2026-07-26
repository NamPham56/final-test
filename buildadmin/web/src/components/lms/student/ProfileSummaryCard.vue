<template>
    <aside class="student-profile-card">
        <div class="student-profile-card__cover">
            <span>{{ readonly ? $t('lms.profile') : $t('lms.summary') }}</span>
        </div>

        <div class="student-profile-card__identity">
            <div class="student-profile-card__avatar-wrap">
                <el-avatar :size="116" :src="avatarUrl">{{ initials }}</el-avatar>
                <el-upload v-if="!readonly" :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onSelect">
                    <el-tooltip :content="$t('lms.changeAvatar')" placement="top">
                        <el-button class="student-profile-card__upload" type="primary" circle :aria-label="$t('lms.changeAvatar')">
                            <el-icon><Camera /></el-icon>
                        </el-button>
                    </el-tooltip>
                </el-upload>
            </div>
            <h2>{{ model.fullName || $t('lms.newStudent') }}</h2>
            <p>{{ model.studentCode || $t('lms.noStudentCode') }}</p>
            <el-tag type="success" effect="light" round> <span class="student-profile-card__status-dot"></span>{{ $t('lms.active') }} </el-tag>
            <small v-if="!readonly" class="student-profile-card__avatar-hint">{{ $t('lms.avatarHint') }}</small>
        </div>

        <div class="student-profile-card__facts">
            <div class="student-profile-card__fact">
                <span class="student-profile-card__fact-icon"
                    ><el-icon><Message /></el-icon
                ></span>
                <span
                    ><small>{{ $t('lms.email') }}</small><strong :title="model.email">{{ model.email || $t('lms.notUpdated') }}</strong></span
                >
            </div>
            <div class="student-profile-card__fact">
                <span class="student-profile-card__fact-icon"
                    ><el-icon><Phone /></el-icon
                ></span>
                <span
                    ><small>{{ $t('lms.phone') }}</small><strong>{{ model.phone || $t('lms.notUpdated') }}</strong></span
                >
            </div>
            <div class="student-profile-card__fact">
                <span class="student-profile-card__fact-icon"
                    ><el-icon><Calendar /></el-icon
                ></span>
                <span
                    ><small>{{ $t('lms.birthday') }}</small><strong>{{ formattedBirthday }}</strong></span
                >
            </div>
            <div class="student-profile-card__fact">
                <span class="student-profile-card__fact-icon"
                    ><el-icon><Location /></el-icon
                ></span>
                <span
                    ><small>{{ $t('lms.address') }}</small><strong :title="model.address">{{ model.address || $t('lms.notUpdated') }}</strong></span
                >
            </div>
        </div>
    </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar, Camera, Location, Message, Phone } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { StudentForm } from '/@/types/lms/student'

const props = defineProps<{ model: StudentForm; currentAvatar?: string; preview?: string; readonly?: boolean }>()
const emit = defineEmits<{ selectAvatar: [UploadFile] }>()
const { t, locale } = useI18n()

const avatarUrl = computed(() => props.preview || props.currentAvatar)
const initials = computed(
    () =>
        props.model.fullName
            ?.trim()
            .split(/\s+/)
            .slice(-2)
            .map((part) => part[0])
            .join('')
            .toUpperCase() || t('lms.studentInitialFallback')
)
const formattedBirthday = computed(() => {
    if (!props.model.dateOfBirth) return t('lms.notUpdated')
    const date = new Date(`${props.model.dateOfBirth}T00:00:00`)
    return Number.isNaN(date.getTime())
        ? props.model.dateOfBirth
        : new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'vi-VN').format(date)
})

function onSelect(file: UploadFile) {
    if (!file.raw) return
    if (!file.raw.type.startsWith('image/')) {
        ElMessage.warning(t('lms.avatarInvalidType'))
        return
    }
    if (file.raw.size > 5 * 1024 * 1024) {
        ElMessage.warning(t('lms.avatarTooLarge'))
        return
    }
    emit('selectAvatar', file)
}
</script>

<style scoped>
.student-profile-card {
    position: sticky;
    top: 18px;
    overflow: hidden;
    border: 1px solid #e4eaf2;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 10px 34px rgba(31, 45, 61, 0.08);
}
.student-profile-card__cover {
    display: flex;
    height: 112px;
    padding: 18px 20px;
    align-items: flex-start;
    justify-content: flex-end;
    background: radial-gradient(circle at 14% 20%, rgba(255, 255, 255, 0.2), transparent 28%), linear-gradient(135deg, #172554, #1d4ed8 72%, #3b82f6);
    color: rgba(255, 255, 255, 0.78);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}
.student-profile-card__identity {
    padding: 0 22px 22px;
    text-align: center;
}
.student-profile-card__avatar-wrap {
    position: relative;
    width: 116px;
    margin: -58px auto 15px;
}
.student-profile-card__avatar-wrap :deep(.el-avatar) {
    border: 5px solid #fff;
    background: linear-gradient(135deg, #dbeafe, #eff6ff);
    color: #1d4ed8;
    font-size: 31px;
    font-weight: 750;
    box-shadow: 0 8px 26px rgba(15, 23, 42, 0.22);
}
.student-profile-card__upload {
    position: absolute;
    right: -1px;
    bottom: 3px;
    width: 36px !important;
    min-height: 36px !important;
    border: 3px solid #fff !important;
    box-shadow: 0 5px 14px rgba(37, 99, 235, 0.35) !important;
}
.student-profile-card h2 {
    margin: 0 0 5px;
    color: #172033;
    font-size: 21px;
    line-height: 1.35;
}
.student-profile-card__identity > p {
    margin: 0 0 12px;
    color: #768196;
    font-size: 13px;
    font-weight: 600;
}
.student-profile-card__status-dot {
    display: inline-block;
    width: 7px;
    height: 7px;
    margin-right: 6px;
    border-radius: 50%;
    background: #22c55e;
}
.student-profile-card__avatar-hint {
    display: block;
    margin-top: 12px;
    color: #98a2b3;
    font-size: 11px;
}
.student-profile-card__facts {
    padding: 8px 20px 18px;
    border-top: 1px solid #edf1f6;
}
.student-profile-card__fact {
    display: flex;
    gap: 12px;
    align-items: center;
    padding: 12px 2px;
    border-bottom: 1px solid #f0f3f7;
    text-align: left;
}
.student-profile-card__fact:last-child {
    border-bottom: 0;
}
.student-profile-card__fact-icon {
    display: grid;
    width: 34px;
    height: 34px;
    flex: none;
    place-items: center;
    border-radius: 10px;
    background: #eff6ff;
    color: #2563eb;
    font-size: 16px;
}
.student-profile-card__fact > span:last-child {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
}
.student-profile-card__fact small {
    margin-bottom: 3px;
    color: #8a94a6;
    font-size: 11px;
}
.student-profile-card__fact strong {
    overflow: hidden;
    color: #344054;
    font-size: 13px;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
}
@media (max-width: 960px) {
    .student-profile-card {
        position: relative;
        top: auto;
    }
    .student-profile-card__facts {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0 20px;
    }
}
@media (max-width: 560px) {
    .student-profile-card__facts {
        grid-template-columns: 1fr;
    }
}
</style>
