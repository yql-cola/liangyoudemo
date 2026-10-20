<template>
  <div class="sale-detail" v-loading="loading">
    <el-page-header content="销售订单详情" @back="$router.push('/sale/out')" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card v-if="order" shadow="never">
      <div class="actions">
        <el-button v-if="order.orderStatus === 'draft'" v-permission="'sale:order:confirm'" type="primary" @click="act('confirm')">确认订单</el-button>
        <el-button v-if="order.orderStatus === 'pending_handoff'" v-permission="'sale:delivery:complete'" type="primary" @click="act('handoff')">确认交付</el-button>
        <el-button v-if="order.orderStatus === 'pending_delivery'" v-permission="'sale:delivery:start'" type="primary" @click="act('start-delivery')">开始配送</el-button>
        <el-button v-if="order.orderStatus === 'in_delivery'" v-permission="'sale:delivery:complete'" type="primary" @click="act('complete-delivery')">确认送达</el-button>
        <el-button v-if="order.orderStatus === 'in_delivery'" v-permission="'sale:delivery:return'" @click="failed">登记未送达</el-button>
        <el-button v-if="order.orderStatus === 'awaiting_return'" v-permission="'sale:delivery:return'" @click="act('return')">确认货物回店</el-button>
        <el-button v-if="['draft','pending_handoff','pending_delivery'].includes(order.orderStatus)" v-permission="'sale:order:update'" @click="$router.push(`/sale/out/edit/${order.id}`)">编辑</el-button>
        <el-button v-if="['draft','pending_handoff','pending_delivery'].includes(order.orderStatus)" v-permission="'sale:order:update'" @click="act('cancel')">取消订单</el-button>
        <el-button v-if="['cancelled','returned'].includes(order.orderStatus)" v-permission="'sale:order:create'" @click="$router.push({ path: '/sale/new', query: { copy: order.id } })">复制新单</el-button>
        <el-button v-if="order.orderStatus !== 'draft' && order.orderStatus !== 'cancelled'" v-permission="'sale:order:export'" @click="download('pdf')">导出 PDF</el-button>
        <el-button v-if="order.orderStatus !== 'draft' && order.orderStatus !== 'cancelled'" v-permission="'sale:order:export'" @click="download('excel')">导出 Excel</el-button>
        <el-button v-if="order.fulfillmentType === 'delivery' && order.orderStatus !== 'draft'" v-permission="'sale:order:print'" @click="preview">打印预览</el-button>
        <el-button v-if="order.fulfillmentType === 'delivery' && order.orderStatus !== 'draft'" v-permission="'sale:order:print'" @click="deliveryDownload">下载配送 PDF</el-button>
      </div>
      <el-descriptions title="订单信息" border :column="3">
        <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ states[order.orderStatus] }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ order.saleType === 'wholesale' ? '批发' : '零售' }}</el-descriptions-item>
        <el-descriptions-item label="交付方式">{{ order.fulfillmentType === 'delivery' ? '配送' : '自提' }}</el-descriptions-item>
        <el-descriptions-item label="录单时间">{{ order.recordedTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ order.completedTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.contactName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ order.contactPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ order.deliveryAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="经手人">{{ order.handlerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计划送达">{{ order.plannedDeliveryTime || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="order.fulfillmentType === 'delivery'" label="配送备注">{{ order.deliveryRemark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="order.items" border style="margin-top:18px">
        <el-table-column prop="productName" label="商品" /><el-table-column prop="spec" label="规格" /><el-table-column prop="unitName" label="单位" />
        <el-table-column prop="qty" label="数量" /><el-table-column prop="unitPrice" label="单价" /><el-table-column prop="amount" label="金额" /><el-table-column prop="remark" label="备注" />
      </el-table>
      <p class="total">本单应收：¥ {{ order.totalAmount }}</p>
      <p class="hint">本单应收为订单金额，不能作为已收款证明。库存流水以正式完成时间为准。</p>
      <el-button v-if="order.orderStatus === 'completed'" type="text" @click="$router.push('/inventory/flow')">查看库存流水</el-button>
      <h3>操作记录</h3>
      <el-table :data="order.events || []" size="small" border>
        <el-table-column prop="createdTime" label="时间" width="190" /><el-table-column prop="eventType" label="操作" width="170" />
        <el-table-column prop="operatorId" label="操作人 ID" width="110" /><el-table-column prop="detail" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>
<script>
import { getSaleOut, saleAction, exportSale, deliveryPdf } from '@/api/sale/saleOut'
import { downloadFile } from '@/utils/download'
const states = { draft: '草稿', pending_handoff: '待交付', pending_delivery: '待配送', in_delivery: '配送中', awaiting_return: '待退回', completed: '已完成', cancelled: '已取消', returned: '已退回' }
export default {
  name: 'SaleOrderDetail', data: () => ({ order: null, states, loading: false, error: '' }),
  created() { this.load() },
  methods: {
    async load() { this.loading = true; try { this.order = await getSaleOut(this.$route.params.id) } catch (e) { this.error = e.message || '订单加载失败' } finally { this.loading = false } },
    async act(action, data) {
      this.loading = true; this.error = ''
      try { await saleAction(this.$route.params.id, action, data); await this.load() }
      catch (e) { this.error = e.message || '操作失败' } finally { this.loading = false }
    },
    async failed() {
      try { const result = await this.$prompt('请输入未送达原因', '登记未送达', { inputValidator: text => !!text || '原因不能为空' }); await this.act('delivery-failed', { reason: result.value }) }
      catch (e) { if (e !== 'cancel') this.error = e.message || '操作失败' }
    },
    async download(type) { try { await downloadFile(await exportSale(this.order.id, type), `销售清单-${this.order.orderNo}.${type === 'excel' ? 'xlsx' : 'pdf'}`) } catch (e) { this.error = e.message } },
    async deliveryDownload() { try { await downloadFile(await deliveryPdf(this.order.id), `配送清单-${this.order.orderNo}.pdf`) } catch (e) { this.error = e.message } },
    preview() { this.$router.push(`/sale/out/print/${this.order.id}`) }
  }
}
</script>
<style scoped>.sale-detail { padding: 16px; }.el-card { margin-top: 16px; }.actions { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }.total { text-align:right;font-size:18px;font-weight:bold; }.hint { color:#909399; }</style>
