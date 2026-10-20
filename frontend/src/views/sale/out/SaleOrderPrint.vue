<template>
  <div class="print-page" v-loading="loading">
    <div class="print-actions"><el-button @click="$router.back()">返回</el-button><el-select v-model="paperPreset" @change="setPaper"><el-option label="A5" value="a5" /><el-option label="窄幅自定义" value="narrow" /></el-select><template v-if="paperPreset === 'narrow'"><span>宽</span><el-input-number v-model="narrowWidth" :min="60" :max="140" :step="1" size="small" @change="setPaper" /><span>高</span><el-input-number v-model="narrowHeight" :min="100" :max="300" :step="1" size="small" @change="setPaper" /><span>毫米</span></template><el-button type="primary" :loading="printing" :disabled="!order || loading" @click="print">打印</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <article v-if="order" class="paper" :class="paperPreset" :style="{ width: paperPreset === 'narrow' ? `${narrowWidth}mm` : '148mm' }">
      <h1>粮油配送中心销售清单</h1>
      <div>客户：{{ order.customerName }}</div><div>录单时间：{{ order.recordedTime }}</div><div>订单编号：{{ order.orderNo }}</div>
      <div>状态：{{ states[order.orderStatus] || order.orderStatus }} 打印时间：{{ printedAt }}</div>
      <div v-if="order.completedTime">实际送达时间：{{ order.completedTime }}</div>
      <table><thead><tr><th>商品名称/规格</th><th>单位</th><th>数量</th><th>单价</th><th>金额</th></tr></thead>
        <tbody><tr v-for="item in order.items" :key="item.id"><td>{{ item.productName }} {{ item.spec }}<small v-if="item.remark">{{ item.remark }}</small></td><td>{{ item.unitName }}</td><td>{{ item.qty }}</td><td>{{ item.unitPrice }}</td><td>{{ item.amount }}</td></tr></tbody></table>
      <p class="amount">本单应收：¥ {{ order.totalAmount }}</p>
      <div>经手人：{{ order.handlerName || '—' }}</div><div>联系电话：{{ order.contactPhone || '—' }}</div><div>配送地址：{{ order.deliveryAddress || '—' }}</div>
      <div v-if="order.deliveryRemark">配送备注：{{ order.deliveryRemark }}</div>
      <div class="sign">收货人签字：________________</div><div>实际收货日期：________________</div>
      <small>本清单为配送凭证，不是法定发票或已收款证明。</small>
    </article>
  </div>
</template>
<script>
import { getSaleOut, saleAction } from '@/api/sale/saleOut'
const states = { pending_delivery: '待配送', in_delivery: '配送中', awaiting_return: '待退回', completed: '已完成', returned: '已退回' }
export default {
  name: 'SaleOrderPrint', data: () => ({ order: null, loading: false, printing: false, error: '', states, paperPreset: 'a5', narrowWidth: 80, narrowHeight: 200, printedAt: new Date().toLocaleString() }),
  mounted() { this.setPaper() },
  beforeDestroy() { const style = document.getElementById('sale-print-paper'); if (style) style.remove() },
  async created() { this.loading = true; try { this.order = await getSaleOut(this.$route.params.id) } catch (e) { this.error = e.message } finally { this.loading = false } },
  methods: {
    setPaper() {
      let style = document.getElementById('sale-print-paper')
      if (!style) { style = document.createElement('style'); style.id = 'sale-print-paper'; document.head.appendChild(style) }
      style.textContent = this.paperPreset === 'narrow' ? `@media print { @page { size: ${this.narrowWidth}mm ${this.narrowHeight}mm; margin: 4mm; } }` : '@media print { @page { size: A5; margin: 5mm; } }'
    },
    async print() {
      if (!this.order || this.printing) return
      this.printing = true; this.error = ''
      try { await saleAction(this.order.id, 'print'); this.printedAt = new Date().toLocaleString(); await this.$nextTick(); window.print() }
      catch (e) { this.error = e.message || '打印请求失败' }
      finally { this.printing = false }
    }
  }
}
</script>
<style scoped>
.print-page { padding: 20px; }.print-actions { margin-bottom: 16px; display:flex; align-items:center; gap:8px; }.paper { width: 148mm; margin: auto; padding: 8mm; background: white; color: #111; font: 12px/1.5 sans-serif; box-sizing: border-box; }
h1 { text-align:center;font-size:18px; } table { width:100%;border-collapse:collapse;margin-top:10px;table-layout:fixed; } th,td { border:1px solid #555;padding:4px;word-break:break-all; } th:first-child { width:35%; } td:nth-child(n+3) { text-align:right; } small { display:block; } .amount { text-align:right;font-weight:bold;font-size:14px; }.sign { margin-top:20px; }
.paper.narrow { width: 80mm; }
@media print { .print-actions { display:none; } .paper { width:100% !important;padding:0; } tr { break-inside:avoid; } thead { display:table-header-group; } }
</style>
<style>@media print { .sidebar-container, .navbar { display: none !important; } .main-container { margin-left: 0 !important; } }</style>
