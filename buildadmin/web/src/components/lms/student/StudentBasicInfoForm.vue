<template>
    <el-tabs v-model="activeTab" class="lms-form-tabs student-form-tabs">
        <el-tab-pane name="general">
            <template #label>
                <span class="student-tab-label"
                    ><el-icon><User /></el-icon>{{ $t('lms.generalInfo') }}</span
                >
            </template>
            <section class="student-form-section">
                <header>
                    <div class="student-form-section__icon">
                        <el-icon><Postcard /></el-icon>
                    </div>
                    <div>
                        <h3>{{ $t('lms.identityInfo') }}</h3>
                        <p>{{ $t('lms.identityDescription') }}</p>
                    </div>
                </header>
                <div class="lms-form-grid student-form-grid">
                    <el-form-item :label="$t('lms.studentCode')" prop="studentCode">
                        <el-input v-model="model.studentCode" :disabled="readonly" :placeholder="$t('lms.studentCodePlaceholder')" maxlength="50" show-word-limit />
                    </el-form-item>
                    <el-form-item :label="$t('lms.fullName')" prop="fullName">
                        <el-input v-model="model.fullName" :disabled="readonly" :placeholder="$t('lms.fullName')" maxlength="150" />
                    </el-form-item>
                    <el-form-item :label="$t('lms.birthday')" prop="dateOfBirth">
                        <el-date-picker
                            v-model="model.dateOfBirth"
                            :disabled="readonly"
                            type="date"
                            value-format="YYYY-MM-DD"
                            format="DD/MM/YYYY"
                            :placeholder="$t('lms.birthday')"
                        />
                    </el-form-item>
                    <el-form-item :label="$t('lms.gender')" prop="gender">
                        <el-select v-model="model.gender" :disabled="readonly" clearable :placeholder="$t('lms.gender')">
                            <el-option :label="$t('lms.male')" value="MALE" />
                            <el-option :label="$t('lms.female')" value="FEMALE" />
                            <el-option :label="$t('lms.other')" value="OTHER" />
                        </el-select>
                    </el-form-item>
                </div>
            </section>
        </el-tab-pane>

        <el-tab-pane name="contact">
            <template #label>
                <span class="student-tab-label"
                    ><el-icon><Phone /></el-icon>{{ $t('lms.contactInfo') }}</span
                >
            </template>
            <section class="student-form-section">
                <header>
                    <div class="student-form-section__icon">
                        <el-icon><ChatDotRound /></el-icon>
                    </div>
                    <div>
                        <h3>{{ $t('lms.contactInfo') }}</h3>
                        <p>{{ $t('lms.contactDescription') }}</p>
                    </div>
                </header>
                <div class="lms-form-grid student-form-grid">
                    <el-form-item :label="$t('lms.email')" prop="email">
                        <el-input v-model="model.email" :disabled="readonly" placeholder="name@example.com" maxlength="150" />
                    </el-form-item>
                    <el-form-item :label="$t('lms.phone')" prop="phone">
                        <el-input
                            v-model="model.phone"
                            :disabled="readonly"
                            :placeholder="$t('lms.phone')"
                            maxlength="20"
                            @input="emit('validateField', 'phone')"
                        />
                    </el-form-item>
                    <el-form-item class="student-form-grid__wide" :label="$t('lms.address')" prop="address">
                        <el-input
                            v-model="model.address"
                            :disabled="readonly"
                            type="textarea"
                            :rows="5"
                            :placeholder="$t('lms.address')"
                            maxlength="500"
                            show-word-limit
                        />
                    </el-form-item>
                </div>
            </section>
        </el-tab-pane>

        <el-tab-pane name="media">
            <template #label>
                <span class="student-tab-label"
                    ><el-icon><Picture /></el-icon>{{ $t('lms.imagesAndDocuments') }}</span
                >
            </template>
            <section class="student-form-section">
                <header>
                    <div class="student-form-section__icon">
                        <el-icon><Picture /></el-icon>
                    </div>
                    <div>
                        <h3>{{ $t('lms.imagesAndDocuments') }}</h3>
                        <p v-if="readonly">{{ $t('lms.mediaDescriptionReadonly') }}</p>
                        <p v-else>{{ $t('lms.mediaDescriptionEdit') }}</p>
                    </div>
                </header>
                <MediaList v-if="media.length" :model-value="media" :editable="!readonly" @remove="emit('removeMedia', $event)" />
                <el-empty v-else :description="$t('lms.noMedia')" :image-size="88" />
                <MediaChangeArrayValidator v-if="!readonly" :model="model" @validate-field="emit('validateField', $event)" />
            </section>
        </el-tab-pane>
    </el-tabs>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ChatDotRound, Phone, Picture, Postcard, User } from '@element-plus/icons-vue'
import MediaList from '/@/components/lms/MediaList.vue'
import MediaChangeArrayValidator from '/@/components/lms/MediaChangeArrayValidator.vue'
import type { MediaInfo } from '/@/types/lms/common'
import type { StudentForm } from '/@/types/lms/student'

defineProps<{ model: StudentForm; media: MediaInfo[]; readonly?: boolean }>()
const emit = defineEmits<{ removeMedia: [MediaInfo]; validateField: [string] }>()
const activeTab = ref('general')
</script>

<style scoped>
.student-form-tabs {
    min-height: 500px;
}
.student-tab-label {
    display: inline-flex;
    gap: 7px;
    align-items: center;
}
.student-form-section {
    padding: 6px 4px 8px;
}
.student-form-section > header {
    display: flex;
    gap: 13px;
    align-items: center;
    margin-bottom: 25px;
    padding-bottom: 18px;
    border-bottom: 1px solid #edf1f5;
}
.student-form-section__icon {
    display: grid;
    width: 40px;
    height: 40px;
    flex: none;
    place-items: center;
    border-radius: 11px;
    background: #eff6ff;
    color: #2563eb;
    font-size: 19px;
}
.student-form-section h3 {
    margin: 0 0 4px;
    color: #1e293b;
    font-size: 16px;
}
.student-form-section p {
    margin: 0;
    color: #8490a3;
    font-size: 12px;
}
.student-form-grid__wide {
    grid-column: 1/-1;
}
.student-form-grid :deep(.el-form-item) {
    display: block;
    margin-bottom: 22px;
}
.student-form-grid :deep(.el-form-item__label) {
    display: block;
    width: auto !important;
    height: auto;
    margin-bottom: 8px;
    color: #475569;
    font-weight: 650;
    line-height: 1.4;
    text-align: left;
}
.student-form-grid :deep(.el-form-item__content) {
    margin-left: 0 !important;
}
.student-form-grid :deep(.el-input),
.student-form-grid :deep(.el-select),
.student-form-grid :deep(.el-date-editor) {
    width: 100%;
}
.student-form-grid :deep(.el-input__wrapper),
.student-form-grid :deep(.el-select__wrapper) {
    min-height: 42px;
}
.student-form-grid :deep(.el-input.is-disabled .el-input__wrapper),
.student-form-grid :deep(.el-select__wrapper.is-disabled),
.student-form-grid :deep(.el-textarea.is-disabled .el-textarea__inner) {
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e8edf3 inset;
}
@media (max-width: 700px) {
    .student-form-grid__wide {
        grid-column: auto;
    }
    .student-form-tabs :deep(.el-tabs__nav-wrap) {
        overflow-x: auto;
    }
    .student-form-tabs :deep(.el-tabs__item) {
        padding: 0 12px;
        font-size: 12px;
    }
}
</style>
