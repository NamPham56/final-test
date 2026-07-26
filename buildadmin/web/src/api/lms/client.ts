import createAxios from '/@/utils/axios'
import { ElNotification } from 'element-plus'
import type { AxiosResponse } from 'axios'
import { i18n } from '/@/lang/index'
import { useConfig } from '/@/stores/config'
import type { ApiResponse } from '/@/types/lms/common'

const languageHeader = () => {
    const lang = useConfig().lang.defaultLang
    return lang === 'en' ? 'en' : 'vi'
}

const withLanguage = (config: any) => ({
    ...config,
    headers: {
        ...(config.headers || {}),
        'Accept-Language': languageHeader(),
    },
})

export const lmsRequest = <T>(config: object) => createAxios(withLanguage(config), { showCodeMessage: true }) as unknown as Promise<ApiResponse<T>>

async function localizedBlobError(error: unknown) {
    const response = (error as { response?: { data?: unknown } })?.response
    const data = response?.data
    let message = ''

    if (data instanceof Blob) {
        try {
            const payload = JSON.parse(await data.text()) as { message?: string }
            message = payload.message?.trim() || ''
            if (response) response.data = payload
        } catch {
            // A binary/non-JSON error body has no backend ApiResponse message.
        }
    } else if (data && typeof data === 'object' && 'message' in data) {
        message = String((data as { message?: unknown }).message || '').trim()
    }

    ElNotification({
        type: 'error',
        message: message || i18n.global.t('axios.Abnormal problem, please contact the website administrator!'),
    })
}

export const blobRequest = async (url: string, params?: object): Promise<AxiosResponse<Blob>> => {
    try {
        return (await createAxios(withLanguage({ url, method: 'get', params, responseType: 'blob' }), {
            reductDataFormat: false,
            showErrorMessage: false,
        })) as unknown as AxiosResponse<Blob>
    } catch (error) {
        await localizedBlobError(error)
        throw error
    }
}

export const downloadBlob = (blob: Blob, name: string) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
}
