import { shallowMount } from '@vue/test-utils'
import BasicDataSelect from '@/components/BasicDataSelect'
import { getBasicOptions, getProductUnits } from '@/api/basic'

jest.mock('@/api/basic', () => ({ getBasicOptions: jest.fn(), getProductUnits: jest.fn() }))
const flush = async () => { await Promise.resolve(); await Promise.resolve() }
const mount = propsData => shallowMount(BasicDataSelect, { propsData, stubs: ['el-select', 'el-option'] })

describe('BasicDataSelect', () => {
  beforeEach(() => { getBasicOptions.mockReset(); getProductUnits.mockReset() })
  test('ignores stale searches and emits Vue 2 value/input plus full selection', async () => {
    let oldResolve
    getBasicOptions.mockImplementationOnce(() => new Promise(resolve => { oldResolve = resolve }))
      .mockResolvedValueOnce([{ id: 2, code: 'P2', name: '新商品', baseUnitId: 8 }])
    const wrapper = mount({ resource: 'products' })
    await wrapper.vm.search('新')
    oldResolve([{ id: 1, name: '旧商品' }]); await flush()
    expect(wrapper.vm.options.map(item => item.id)).toEqual([2])
    wrapper.vm.change(2)
    expect(wrapper.emitted().input[0]).toEqual([2])
    expect(wrapper.emitted().change[0][1].baseUnitId).toBe(8)
  })
  test('keeps an explicitly supplied historical name without offering other disabled data', async () => {
    getBasicOptions.mockResolvedValue([])
    const wrapper = mount({ resource: 'warehouses', value: 7, selectedLabel: '老仓库' })
    await flush()
    expect(wrapper.vm.displayOptions).toEqual([expect.objectContaining({ id: 7, name: '老仓库', historical: true })])
  })
  test('loads only product-supported units and clears stale options when product changes', async () => {
    getProductUnits.mockResolvedValue([{ id: 3, name: '箱', convertRate: 10, baseUnitId: 4 }])
    const wrapper = mount({ resource: 'product-units', productId: 5 })
    await flush()
    expect(getProductUnits).toHaveBeenCalledWith(5, expect.any(Object))
    await wrapper.setProps({ productId: undefined }); await flush()
    expect(wrapper.vm.options).toEqual([])
  })
})


test('retains newly selected option label when a later search omits it', async () => {
  getBasicOptions.mockResolvedValueOnce([{ id: 1, name: '新单位' }]).mockResolvedValueOnce([])
  const wrapper = mount({ resource: 'units', value: 7, selectedLabel: '原单位' }); await flush()
  wrapper.vm.change(1)
  await wrapper.setProps({ value: 1 })
  await wrapper.vm.search('不存在')
  expect(wrapper.vm.displayOptions[0].name).toBe('新单位')
  expect(wrapper.emitted().input).toHaveLength(1)
})
