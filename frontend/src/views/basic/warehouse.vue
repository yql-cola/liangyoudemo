<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicWarehouse', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['warehouse'] }) }
</script>
