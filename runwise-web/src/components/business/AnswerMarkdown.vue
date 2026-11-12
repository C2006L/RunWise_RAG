<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import { createAnswerRenderer } from '../../../../shared/markdown-policy.mjs'
const props=defineProps({ text:{type:String,default:''} })
const renderer=createAnswerRenderer(MarkdownIt)
const blocks=computed(()=>renderer.blocks(props.text))
</script>
<template><div class="answer-markdown"><div v-for="(block,i) in blocks" :key="i" :class="{'markdown-table':block.table}" v-html="block.html"></div></div></template>
<style scoped>
.answer-markdown{font-family:var(--font-body,inherit);font-size:15px;line-height:1.85;overflow-wrap:anywhere;min-width:0;max-width:100%;white-space:normal}.markdown-table{overflow-x:auto;max-width:100%;margin:12px 0}.answer-markdown :deep(pre){margin:0;white-space:pre;line-height:1.7}.answer-markdown :deep(a){color:#ff7d84;text-decoration:underline}.answer-markdown :deep(code){font-family:monospace;background:#252528;padding:1px 4px}.answer-markdown :deep(h1),.answer-markdown :deep(h2){font-size:20px}.answer-markdown :deep(p:last-child){margin-bottom:0}
</style>
