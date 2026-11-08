import { shallowMount } from '@vue/test-utils'
import ProductImage from '@/components/ProductImage'
import { getProductImage } from '@/api/basic'

jest.mock('@/api/basic', () => ({ getProductImage: jest.fn() }))
const flush = async () => { for (let i = 0; i < 12; i++) await Promise.resolve() }
const originalObserver = global.IntersectionObserver
const mount = () => shallowMount(ProductImage, {
  propsData: { productId: 1, hasImage: true, imageVersion: 'first', preview: true },
  stubs: { 'el-dialog': { template: '<div><slot /></div>' } }
})

beforeEach(() => {
  delete global.IntersectionObserver
  getProductImage.mockReset()
  let sequence = 0
  URL.createObjectURL = jest.fn(() => `blob:image-${++sequence}`)
  URL.revokeObjectURL = jest.fn()
})
afterEach(() => { global.IntersectionObserver = originalObserver })

test('requests a different image version after replacing the product image', async () => {
  getProductImage.mockResolvedValue(new Blob(['image']))
  const wrapper = mount(); await flush()
  expect(getProductImage).toHaveBeenLastCalledWith(1, true, 'first')
  const previous = wrapper.vm.source
  await wrapper.setProps({ imageVersion: 'second' }); await flush()
  expect(getProductImage).toHaveBeenLastCalledWith(1, true, 'second')
  expect(URL.revokeObjectURL).toHaveBeenCalledWith(previous)
  wrapper.destroy()
})

test('ignores a preview response from an earlier closed dialog', async () => {
  let resolveFirst, resolveSecond
  getProductImage.mockResolvedValueOnce(new Blob(['thumbnail']))
    .mockImplementationOnce(() => new Promise(resolve => { resolveFirst = resolve }))
    .mockImplementationOnce(() => new Promise(resolve => { resolveSecond = resolve }))
  const wrapper = mount(); await flush()
  const first = wrapper.vm.openPreview({ stopPropagation() {} }); await flush()
  wrapper.vm.closePreview()
  const second = wrapper.vm.openPreview({ stopPropagation() {} }); await flush()
  resolveSecond(new Blob(['new preview'])); await second
  const latest = wrapper.vm.fullSource
  resolveFirst(new Blob(['old preview'])); await first
  expect(wrapper.vm.fullSource).toBe(latest)
  wrapper.destroy()
})

test('coalesces simultaneous image prop changes and disconnects each observer', async () => {
  const observers = []
  global.IntersectionObserver = jest.fn(callback => {
    const observer = { callback, observe: jest.fn(), disconnect: jest.fn() }
    observers.push(observer)
    return observer
  })
  getProductImage.mockResolvedValue(new Blob(['image']))
  const wrapper = mount()
  await wrapper.setProps({ productId: 2, imageVersion: 'second' }); await flush()
  expect(observers).toHaveLength(2)
  expect(observers[0].disconnect).toHaveBeenCalled()
  expect(() => observers[0].callback([{ isIntersecting: true }])).not.toThrow()
  observers[1].callback([{ isIntersecting: true }]); await flush()
  expect(getProductImage).toHaveBeenCalledTimes(1)
  expect(getProductImage).toHaveBeenCalledWith(2, true, 'second')
  wrapper.destroy()
})
