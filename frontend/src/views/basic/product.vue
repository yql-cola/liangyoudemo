<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicProduct', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['product'] }) }
</script>
