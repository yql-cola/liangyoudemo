<template><BasicDataPage :schema="schema" /></template>
<script>
import BasicDataPage from './BasicDataPage.vue'
import { basicSchemas } from './schemas'
export default { name: 'BasicCategory', components: { BasicDataPage }, data: () => ({ schema: basicSchemas['category'] }) }
</script>
