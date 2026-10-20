import { shallowMount } from '@vue/test-utils'
import ChartSwitcher from '@/components/ChartSwitcher'
import ScenarioPage from '@/views/statistics/ScenarioPage'
import { chartOption, statisticsParams, validateStatisticsQuery, statisticsColumns } from '@/views/statistics/scenarios'
import { getStatistics, exportStatistics } from '@/api/statistics/statistics'
import { downloadFile } from '@/utils/download'
import * as echarts from 'echarts'

jest.mock('echarts', () => ({ init: jest.fn(() => ({ setOption: jest.fn(), resize: jest.fn(), dispose: jest.fn() })) }))
jest.mock('@/api/statistics/statistics', () => ({ getStatistics: jest.fn(), exportStatistics: jest.fn() }))
jest.mock('@/utils/download', () => ({ downloadFile: jest.fn() }))
const stubs = ['el-card', 'el-form', 'el-form-item', 'el-select', 'el-option', 'el-button', 'el-date-picker', 'el-radio-group', 'el-radio-button', 'el-table', 'el-table-column', 'el-alert', 'el-empty', 'el-tag']
const mountPage = scenario => shallowMount(ScenarioPage, { propsData: { scenario }, stubs, directives: { loading: {}, permission: {} } })
const flush = async () => { await Promise.resolve(); await Promise.resolve() }
beforeEach(() => { getStatistics.mockReset(); getStatistics.mockResolvedValue({ rows: [], metrics: [] }); exportStatistics.mockReset(); exportStatistics.mockResolvedValue({ data: 'file' }) })

test('switching charts preserves source rows and chart limits never affect table rows', async () => {
  const rows = Array.from({ length: 25 }, (_, i) => ({ dimensionName: `商品${i}`, amount: 25 - i, qty: i, unitName: 'kg' }))
  const original = JSON.stringify(rows)
  const wrapper = shallowMount(ChartSwitcher, { propsData: { rows, timeSeries: true }, stubs })
  for (const type of ['pie', 'bar', 'line', 'table']) {
    await wrapper.setData({ chartType: type })
    expect(wrapper.vm.chartType).toBe(type)
    expect(wrapper.find('el-table-stub').exists()).toBe(type === 'table')
    expect(wrapper.find('[role="img"]').isVisible()).toBe(type !== 'table')
  }
  expect(wrapper.vm.tableRows).toHaveLength(25)
  expect(JSON.stringify(rows)).toBe(original)
  expect(chartOption(rows, 'bar', 'qty', 10).series[0].data).toHaveLength(10)
  expect(chartOption(rows, 'pie', 'qty', 10).series[0].data[0].name).toBe('商品24')
  wrapper.destroy()
})

test('pie includes every filtered product so Top bars do not change the share denominator', () => {
  const rows = Array.from({ length: 25 }, (_, i) => ({ dimensionName: `商品${i}`, amount: 1, qty: 2, amountRate: 4, qtyRate: 4 }))
  for (const metric of ['amount', 'qty']) {
    const slices = chartOption(rows, 'pie', metric, 10).series[0].data
    expect(slices).toHaveLength(25)
    const total = slices.reduce((sum, item) => sum + item.value, 0)
    expect(slices[0].value / total * 100).toBe(4)
  }
})

test('chart resizes with its viewport and disposes when the scenario is removed', () => {
  const wrapper = shallowMount(ChartSwitcher, { propsData: { rows: [] }, stubs })
  const chart = echarts.init.mock.results[echarts.init.mock.results.length - 1].value
  window.dispatchEvent(new Event('resize'))
  expect(chart.resize).toHaveBeenCalled()
  wrapper.destroy()
  expect(chart.dispose).toHaveBeenCalledTimes(1)
})

test('ranking charts do not offer lines and inventory query drops dates from current snapshot', () => {
  const wrapper = shallowMount(ChartSwitcher, { propsData: { rows: [] }, stubs })
  expect(wrapper.vm.chartTypes.map(item => item.value)).not.toContain('line')
  expect(statisticsParams('inventory-structure', { warehouseId: 1, dateFrom: '2026-10-01', dateTo: '2026-10-18', dimension: 'category', top: 10 })).toEqual({ warehouseId: 1, dimension: 'category' })
  expect(validateStatisticsQuery('trend', { dateFrom: '2026-10-18', dateTo: '2026-10-01' })).toContain('开始')
  wrapper.destroy()
})

test('purchase product detail includes the supplier supplied by the report', () => {
  expect(statisticsColumns('purchase-product').map(column => column.prop)).toContain('supplierName')
})

test('scenario change and overlapping requests discard stale results', async () => {
  let finishFirst
  getStatistics.mockImplementationOnce(() => new Promise(resolve => { finishFirst = resolve }))
  const wrapper = mountPage('sale-product')
  await wrapper.setProps({ scenario: 'customer-rank' }); await flush()
  expect(getStatistics).toHaveBeenLastCalledWith('customer-rank', expect.any(Object))
  finishFirst({ rows: [{ dimensionName: '过期商品' }] }); await flush()
  expect(wrapper.vm.result.rows).toEqual([])
  wrapper.destroy()
})

test('export uses current scenario and all filters without chart Top limit', async () => {
  const wrapper = mountPage('sale-product'); await flush()
  wrapper.vm.query.productId = 7
  await wrapper.vm.exportData()
  expect(exportStatistics).toHaveBeenCalledWith('sale-product', expect.objectContaining({ productId: 7 }))
  expect(exportStatistics.mock.calls[0][1]).not.toHaveProperty('top')
  expect(downloadFile).toHaveBeenCalled()
  wrapper.destroy()
})
