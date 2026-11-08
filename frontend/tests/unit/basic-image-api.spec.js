import request from '@/utils/request'
import { getProductImage } from '@/api/basic'

jest.mock('@/utils/request', () => jest.fn())

test('includes the image version in the URL parameters to bypass stale browser caches', async () => {
  request.mockResolvedValue({ data: new Blob(['image']) })
  await getProductImage(7, true, 'replacement')
  expect(request).toHaveBeenCalledWith(expect.objectContaining({
    url: '/v1/basic/products/7/thumbnail', params: { v: 'replacement' }, responseType: 'blob'
  }))
})
