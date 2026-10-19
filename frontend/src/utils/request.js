import axios from 'axios'
import { Message } from 'element-ui'
import { getToken, removeToken } from './auth'

export function translateApiError(message) {
  const errors = {
    'basic-data-in-use': '当前资料已被业务数据引用，不能删除，可停用后禁止新业务继续使用。',
    'basic-data-not-found': '基础资料不存在或已删除',
    'basic-data-disabled': '基础资料已停用，请重新选择',
    'basic-data-duplicate': '编码或资料记录已存在，请检查后重试',
    'product-required': '请选择商品',
    'product-id-required': '请选择商品',
    'category-required': '请选择商品分类',
    'category-id-required': '请选择商品分类',
    'unit-id-required': '请选择基础单位',
    'from-unit-id-required': '请选择源单位',
    'to-unit-id-required': '请选择目标单位',
    'parent-id-required': '请选择上级分类',
    'category-parent-invalid': '上级分类无效',
    'category-cycle': '分类不能选择自身或其子分类作为上级',
    'shelf-life-invalid': '保质期必须是非负整数',
    'product-unit-invalid': '所选单位不适用于该商品',
    'product-unit-unreachable': '该单位未配置到商品基础单位的换算关系',
    'convert-units-invalid': '源单位与目标单位必须不同',
    'convert-cycle': '单位换算不能形成循环',
    'convert-rate-inconsistent': '换算比率与现有换算关系不一致',
    'convert-rate-required': '请填写换算比率',
    'status-invalid': '状态无效',
    'convert-rate-invalid': '换算比率必须大于零',
    'unit-convert-exists': '该商品的单位换算关系已存在',
    'unit-convert-cycle': '单位换算不能形成循环',
    'export-failed': '导出失败，请稍后重试'
  }
  const resources = { product: '商品', category: '分类', unit: '单位', warehouse: '仓库', supplier: '供应商', customer: '客户' }
  Object.keys(resources).forEach(key => {
    errors[`${key}-code-required`] = `请填写${resources[key]}编码`
    errors[`${key}-code-exists`] = `${resources[key]}编码已存在`
    errors[`${key}-name-required`] = `请填写${resources[key]}名称`
  })
  return errors[message] || message || '请求失败'
}

function readBlob(blob) {
  if (blob.text) return blob.text()
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsText(blob)
  })
}

export async function assertDownloadBlob(blob) {
  if (!(blob instanceof Blob)) throw new Error('下载文件格式无效')
  // Some gateways omit the JSON content type, so also inspect the opening bytes.
  const prefix = await readBlob(blob.slice(0, 64))
  if (/json|text\//i.test(blob.type) || /^\s*[{[]/.test(prefix)) {
    let data
    try { data = JSON.parse(await readBlob(blob)) } catch (e) { throw new Error('导出失败，服务器未返回 Excel 文件') }
    throw new Error(translateApiError(data.message || 'export-failed'))
  }
}

export async function normalizeHttpResponse(response) {
  if (response.config && response.config.responseType === 'blob') {
    await assertDownloadBlob(response.data)
    return response
  }
  return normalizeApiResponse(response.data)
}

export function normalizeApiResponse(response) {
  const res = response && response.code !== undefined ? response : response.data
  if (res.code === 0) {
    return res.data
  }
  throw new Error(translateApiError(res.message || 'request-error'))
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

service.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  response => normalizeHttpResponse(response),
  async error => {
    const status = error.response && error.response.status
    let message = error.response && error.response.data && error.response.data.message
    if (error.response && error.response.data instanceof Blob) {
      try { await assertDownloadBlob(error.response.data) } catch (blobError) { message = blobError.message }
    }

    if (status === 401) {
      removeToken()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    error.message = translateApiError(message || error.message || '请求失败')
    Message.error(error.message)
    return Promise.reject(error)
  }
)

export default service
