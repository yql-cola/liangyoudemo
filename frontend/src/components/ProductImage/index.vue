<template>
  <span class="product-image" :style="{ width: `${size}px`, height: `${size}px` }">
    <img v-if="source && !failed" :src="source" alt="商品图片" :class="{ clickable: preview }" @click="openPreview" @error="failed = true">
    <span v-else class="placeholder"><i class="el-icon-picture-outline" /></span>
    <el-dialog v-if="preview" title="商品图片" :visible.sync="dialogVisible" append-to-body width="520px" @close="closePreview">
      <img v-if="fullSource && !fullFailed" :src="fullSource" class="full-image" alt="商品原图" @error="fullFailed = true">
      <p v-else>{{ fullFailed ? '图片加载失败' : '图片加载中…' }}</p>
    </el-dialog>
  </span>
</template>

<script>
import { getProductImage } from '@/api/basic'

let active = 0
const pending = []
function limited(task) {
  return new Promise((resolve, reject) => {
    pending.push({ task, resolve, reject })
    pump()
  })
}
function pump() {
  while (active < 4 && pending.length) {
    const next = pending.shift(); active++
    Promise.resolve().then(next.task).then(next.resolve, next.reject).finally(() => { active--; pump() })
  }
}

export default {
  name: 'ProductImage',
  props: { productId: { type: [Number, String], default: null }, hasImage: Boolean,
    legacyUrl: { type: String, default: '' }, imageVersion: { type: String, default: '' },
    size: { type: Number, default: 40 }, preview: Boolean },
  data: () => ({ source: '', fullSource: '', failed: false, fullFailed: false, dialogVisible: false,
    requestId: 0, previewId: 0, observer: null, objectUrl: '', fullObjectUrl: '' }),
  computed: { legacySource() { return /^https?:\/\//i.test(this.legacyUrl || '') ? this.legacyUrl : '' } },
  watch: {
    productId() { this.reset() }, hasImage() { this.reset() }, imageVersion() { this.reset() }, legacyUrl() { this.reset() }
  },
  mounted() { this.observe() },
  beforeDestroy() { this.requestId++; this.previewId++; if (this.observer) this.observer.disconnect(); this.release() },
  methods: {
    observe() {
      if (!this.hasImage) { this.source = this.legacySource; return }
      if (typeof IntersectionObserver === 'undefined') { this.loadThumbnail(); return }
      const id = this.requestId
      // Capture this observer so a stale callback cannot disconnect a newer one.
      const observer = new IntersectionObserver(entries => {
        if (entries.some(entry => entry.isIntersecting)) {
          observer.disconnect()
          if (this.observer === observer) this.observer = null
          if (id === this.requestId) this.loadThumbnail()
        }
      })
      this.observer = observer
      observer.observe(this.$el)
    },
    reset() {
      const id = ++this.requestId
      this.closePreview(); this.release(); this.source = ''; this.failed = false; this.fullFailed = false
      if (this.observer) this.observer.disconnect()
      this.observer = null
      // Product ID, image flag and version may change in the same Vue update.
      this.$nextTick(() => { if (id === this.requestId) this.observe() })
    },
    release() {
      if (this.objectUrl) URL.revokeObjectURL(this.objectUrl)
      if (this.fullObjectUrl) URL.revokeObjectURL(this.fullObjectUrl)
      this.objectUrl = ''; this.fullObjectUrl = ''
    },
    async loadThumbnail() {
      if (!this.productId || !this.hasImage) return
      const id = ++this.requestId
      const productId = this.productId
      const version = this.imageVersion
      try {
        const blob = await limited(() => getProductImage(productId, true, version))
        if (id !== this.requestId) return
        this.objectUrl = URL.createObjectURL(blob); this.source = this.objectUrl
      } catch (e) { if (id === this.requestId) this.failed = true }
    },
    async openPreview(event) {
      if (!this.preview || !this.source) return
      event.stopPropagation()
      const previewId = ++this.previewId
      this.dialogVisible = true; this.fullFailed = false
      if (!this.hasImage) { this.fullSource = this.legacySource; return }
      const id = this.requestId
      const productId = this.productId
      const version = this.imageVersion
      try {
        const blob = await limited(() => getProductImage(productId, false, version))
        if (!this.dialogVisible || id !== this.requestId || previewId !== this.previewId) return
        if (this.fullObjectUrl) URL.revokeObjectURL(this.fullObjectUrl)
        this.fullObjectUrl = URL.createObjectURL(blob); this.fullSource = this.fullObjectUrl
      } catch (e) { if (id === this.requestId && previewId === this.previewId) this.fullFailed = true }
    },
    closePreview() {
      this.previewId++
      this.dialogVisible = false; this.fullSource = ''
      if (this.fullObjectUrl) URL.revokeObjectURL(this.fullObjectUrl)
      this.fullObjectUrl = ''
    }
  }
}
</script>

<style scoped>
.product-image { display: inline-flex; flex: none; vertical-align: middle; overflow: hidden; border-radius: 4px; background: #f3f5f7; }
.product-image img { width: 100%; height: 100%; object-fit: contain; }
.product-image .clickable { cursor: zoom-in; }
.placeholder { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; color: #b8c0cc; }
.full-image { display: block; max-width: 100%; max-height: 70vh; margin: auto; object-fit: contain; }
</style>
