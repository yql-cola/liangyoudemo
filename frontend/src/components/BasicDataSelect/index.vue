<template>
  <div class="basic-data-select">
    <el-select :value="value" :placeholder="placeholder" :disabled="disabled" :loading="loading" filterable remote clearable
               :remote-method="search" :no-data-text="error || '暂无可选资料'" @change="change" @visible-change="onVisible">
      <el-option v-for="option in displayOptions" :key="option.id" :value="option.id" :label="option.name"
                 :disabled="option.historical || option.status === 0">
        <span>{{ option.name }}</span><span v-if="option.code" class="code">{{ option.code }}</span>
        <span v-if="option.historical" class="code">历史资料</span>
      </el-option>
    </el-select>
    <span v-if="error" class="error" role="alert">{{ error }}</span>
  </div>
</template>

<script>
import { getBasicOptions, getProductUnits } from '@/api/basic'

export default {
  name: 'BasicDataSelect',
  props: {
    value: { type: [Number, String], default: undefined },
    resource: { type: String, required: true },
    productId: { type: [Number, String], default: undefined },
    params: { type: Object, default: () => ({}) },
    selectedLabel: { type: String, default: '' },
    placeholder: { type: String, default: '请选择，可输入关键词搜索' },
    disabled: Boolean
  },
  data: () => ({ options: [], loading: false, error: '', requestId: 0, keyword: '', selectedOption: null }),
  computed: {
    displayOptions() {
      if (this.selectedOption && this.selectedOption.id === this.value && !this.options.some(item => item.id === this.value)) {
        return [this.selectedOption, ...this.options]
      }
      if (this.value !== undefined && this.value !== null && this.value !== '' && this.selectedLabel && !this.options.some(item => item.id === this.value)) {
        return [{ id: this.value, name: this.selectedLabel, historical: true }, ...this.options]
      }
      return this.options
    }
  },
  watch: {
    resource() { this.selectedOption = null; this.search('') },
    productId() { this.selectedOption = null; this.options = []; this.search('') },
    params: { deep: true, handler() { this.search(this.keyword) } }
  },
  created() { this.search('') },
  beforeDestroy() { this.requestId++ },
  methods: {
    async search(keyword = '') {
      const id = ++this.requestId
      this.keyword = keyword
      this.error = ''
      if ((this.resource === 'product-units' || this.resource === 'units' && this.productId !== undefined) && !this.productId) {
        this.options = []; this.loading = false; return
      }
      this.loading = true
      try {
        const params = { ...this.params, keyword }
        const options = this.resource === 'product-units' || this.resource === 'units' && this.productId
          ? await getProductUnits(this.productId, params)
          : await getBasicOptions(this.resource, params)
        if (id === this.requestId) this.options = (options || []).filter(item => item.status !== 0)
      } catch (e) {
        if (id === this.requestId) { this.error = e.message || '资料加载失败'; this.options = [] }
      } finally { if (id === this.requestId) this.loading = false }
    },
    change(value) {
      this.selectedOption = this.displayOptions.find(item => item.id === value) || null
      this.$emit('input', value)
      this.$emit('change', value, this.selectedOption)
    },
    onVisible(visible) { if (visible) this.search('') }
  }
}
</script>

<style scoped>
.basic-data-select, .el-select { width: 100%; }
.code { color: #909399; margin-left: 12px; font-size: 12px; }
.error { display: block; color: #f56c6c; font-size: 12px; line-height: 18px; }
</style>
