<template>
  <div v-show="total > 0" class="pagination-container">
    <el-pagination
      :current-page="page"
      :page-size="limit"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  props: {
    total: {
      type: Number,
      required: true
    },
    page: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 10
    },
    pageSizes: {
      type: Array,
      default: () => [10, 20, 50, 100]
    }
  },
  methods: {
    handleSizeChange(val) {
      this.$emit('update:limit', val)
      this.$emit('pagination', { page: this.page, limit: val })
    },
    handleCurrentChange(val) {
      this.$emit('update:page', val)
      this.$emit('pagination', { page: val, limit: this.limit })
    }
  }
}
</script>

<style scoped>
.pagination-container {
  padding: 16px 0 0;
  text-align: right;
}
</style>
