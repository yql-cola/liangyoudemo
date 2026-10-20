import { downloadFile } from '@/utils/download'
import { normalizeHttpResponse, translateApiError } from '@/utils/request'

describe('Excel downloads', () => {
  beforeEach(() => {
    URL.createObjectURL = jest.fn(() => 'blob:report')
    URL.revokeObjectURL = jest.fn()
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })
  afterEach(() => jest.restoreAllMocks())
  test('preserves binary response and downloads server filename, then releases URL', async () => {
    const response = { config: { responseType: 'blob' }, data: new Blob(['xlsx']), headers: { 'content-disposition': "attachment; filename*=UTF-8''%E5%95%86%E5%93%81.xlsx" } }
    expect(await normalizeHttpResponse(response)).toBe(response)
    await downloadFile(response, 'fallback.xlsx')
    expect(HTMLAnchorElement.prototype.click.mock.instances[0].download).toBe('商品.xlsx')
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:report')
    expect(document.querySelector('a')).toBeNull()
  })
  test('rejects JSON business errors returned as a blob without downloading', async () => {
    const response = { config: { responseType: 'blob' }, data: new Blob([JSON.stringify({ code: 400, message: 'basic-data-in-use' })], { type: 'application/json' }), headers: {} }
    await expect(downloadFile(response)).rejects.toThrow('当前资料已被业务数据引用')
    expect(URL.createObjectURL).not.toHaveBeenCalled()
    await expect(normalizeHttpResponse(response)).rejects.toThrow('当前资料已被业务数据引用')
  })
  test('translates known basic errors and leaves unknown messages intact', () => {
    expect(translateApiError('product-code-exists')).toBe('商品编码已存在')
    expect(translateApiError('validation-error')).toBe('validation-error')
  })
})
