<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicSupplier', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['supplier'] }) }
</script>
