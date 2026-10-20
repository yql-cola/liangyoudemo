// Each schema describes fields, labels and validation; request and dialog behavior lives in BasicDataPage.
const text = (key, label, required = false, max = 128) => ({ key, label, required, max })
const select = (key, label, resource, labelKey) => ({ key, label, resource, labelKey, required: true })
const number = (key, label, min = 0, required = false) => ({ key, label, type: 'number', min, required })
const contact = [text('contactName', '联系人', false, 64), text('contactPhone', '联系电话', false, 32), text('address', '地址', false, 255)]
const account = [text('taxNo', '税号', false, 64), text('bankName', '开户行'), text('bankAccount', '银行账号', false, 64)]
const statusRemark = [{ key: 'status', label: '状态', type: 'status', required: true }, { ...text('remark', '备注', false, 255), type: 'textarea' }]
const business = (key, resource, title, extraFields = contact) => ({
  key, resource, title,
  fields: [text(`${key}Code`, `${title}编码`, true, 64), text(`${key}Name`, `${title}名称`, true), ...extraFields, ...statusRemark],
  columns: [[`${key}Code`, '编码'], [`${key}Name`, '名称'], ['contactName', '联系人'], ['contactPhone', '联系电话'], ['address', '地址']]
})

export const basicSchemas = {
  product: {
    key: 'product', resource: 'products', title: '商品',
    fields: [text('productCode', '商品编码', true, 64), text('productName', '商品名称', true),
      select('categoryId', '商品分类', 'categories', 'categoryName'), text('brandName', '品牌', false, 64), text('spec', '规格'), text('barcode', '条码', false, 64),
      text('imageUrl', '图片地址', false, 255), select('baseUnitId', '基础单位', 'units', 'baseUnitName'),
      number('shelfLifeDays', '保质期（天）'), ...statusRemark],
    filters: [select('categoryId', '分类', 'categories')],
    columns: [['productCode', '商品编码'], ['productName', '商品名称'], ['categoryName', '分类'], ['brandName', '品牌'], ['spec', '规格'], ['baseUnitName', '基础单位']]
  },
  category: {
    key: 'category', resource: 'categories', title: '分类', tree: true,
    fields: [text('categoryName', '分类名称', true), { key: 'parentId', label: '上级分类', type: 'parent', required: true },
      number('sortNo', '排序', 0, true), ...statusRemark],
    columns: [['categoryName', '分类名称'], ['sortNo', '排序'], ['remark', '备注']]
  },
  unit: {
    key: 'unit', resource: 'units', title: '单位',
    fields: [text('unitCode', '单位编码', true, 32), text('unitName', '单位名称', true, 32), ...statusRemark],
    columns: [['unitCode', '单位编码'], ['unitName', '单位名称'], ['remark', '备注']]
  },
  'unit-convert': {
    key: 'unit-convert', resource: 'unit-converts', title: '单位换算',
    fields: [select('productId', '商品', 'products', 'productName'), select('fromUnitId', '源单位', 'units', 'fromUnitName'),
      select('toUnitId', '目标单位', 'units', 'toUnitName'), { ...number('convertRate', '换算比率', 0.000001, true), precision: 6 }, ...statusRemark],
    filters: [select('productId', '商品', 'products'), select('fromUnitId', '源单位', 'units'), select('toUnitId', '目标单位', 'units')],
    columns: [['productName', '商品'], ['fromUnitName', '源单位'], ['toUnitName', '目标单位'], ['convertRate', '换算比率'], ['remark', '备注']]
  },
  warehouse: business('warehouse', 'warehouses', '仓库'),
  supplier: business('supplier', 'suppliers', '供应商', [...contact, ...account]),
  customer: business('customer', 'customers', '客户', [...contact, ...account])
}
