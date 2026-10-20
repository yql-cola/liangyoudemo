<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicUnitConvert', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['unit-convert'] }) }
</script>
