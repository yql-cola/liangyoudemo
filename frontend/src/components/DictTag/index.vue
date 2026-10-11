<template>
  <el-tag :type="tagType" size="mini">{{ label }}</el-tag>
</template>

<script>
export default {
  name: 'DictTag',
  props: {
    value: {
      type: [String, Number],
      default: ''
    }
  },
  computed: {
    label() {
      if (Number(this.value) === 1) return '启用'
      if (Number(this.value) === 0) return '停用'
      return String(this.value)
    },
    tagType() {
      if (Number(this.value) === 1) return 'success'
      if (Number(this.value) === 0) return 'info'
      return 'warning'
    }
  }
}
</script>
