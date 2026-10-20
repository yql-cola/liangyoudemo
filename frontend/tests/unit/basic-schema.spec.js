import { basicSchemas } from '@/views/basic/schemas'

// SaveRequest @Size limits and matching SQL VARCHAR columns are the input contract.
const contact = { contactName: 64, contactPhone: 32, address: 255 }
const account = { taxNo: 64, bankName: 128, bankAccount: 64 }
const contracts = [
  ['product', { productCode: 64, productName: 128, brandName: 64, spec: 128, barcode: 64, imageUrl: 255, remark: 255 }],
  ['category', { categoryName: 128, remark: 255 }],
  ['unit', { unitCode: 32, unitName: 32, remark: 255 }],
  ['unit-convert', { remark: 255 }],
  ['warehouse', { warehouseCode: 64, warehouseName: 128, ...contact, remark: 255 }],
  ['supplier', { supplierCode: 64, supplierName: 128, ...contact, ...account, remark: 255 }],
  ['customer', { customerCode: 64, customerName: 128, ...contact, ...account, remark: 255 }]
]

test.each(contracts)('%s input limits accept the backend field capacity without permitting longer values', (resource, expected) => {
  const limits = basicSchemas[resource].fields.filter(field => field.max).reduce((result, field) => {
    result[field.key] = field.max
    return result
  }, {})
  expect(limits).toEqual(expected)
})
