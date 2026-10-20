<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicUnit', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['unit'] }) }
</script>
