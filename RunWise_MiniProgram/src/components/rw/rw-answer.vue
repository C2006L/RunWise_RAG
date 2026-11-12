<script setup lang="ts">
import {computed} from 'vue';
import MarkdownIt from 'markdown-it';
import {createAnswerRenderer} from '../../../../shared/markdown-policy.mjs';
const props=defineProps<{text:string}>();
const renderer=createAnswerRenderer(MarkdownIt);
const blocks=computed(()=>renderer.blocks(props.text));
</script>
<template><view class="rw-answer"><view v-for="(block,i) in blocks" :key="i"><scroll-view v-if="block.table" scroll-x class="rw-answer-table"><rich-text :nodes="block.html" selectable /></scroll-view><rich-text v-else :nodes="block.html" selectable /></view></view></template>
<style scoped>.rw-answer{font-size:28rpx;line-height:1.85;color:#f5f5f5;overflow-wrap:anywhere;max-width:100%;min-width:0}.rw-answer-table{max-width:100%;width:100%;margin:20rpx 0}</style>
