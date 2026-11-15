<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ src: { type: String, required: true } })
const viewer = ref(null)
const enlarged = ref(false)
const failed = ref(false)

function open() {
  enlarged.value = false
  viewer.value?.showModal()
}
watch(() => props.src, () => {
  failed.value = false
  viewer.value?.close()
})
</script>

<template>
  <div class="training-photo">
    <button class="photo-preview" type="button" aria-label="查看完整训练照片" @click="open">
      <img v-if="!failed" :src="src" alt="训练照片" @error="failed = true" />
      <span v-else class="photo-error">图片加载失败，请更换图片或稍后重试</span>
      <span class="photo-caption">点击查看大图 ↗</span>
    </button>
    <dialog ref="viewer" class="photo-viewer" aria-label="训练照片大图" @click="event => { if (event.target === viewer) viewer.close() }" @close="enlarged = false">
      <header>
        <strong>训练照片</strong>
        <button type="button" :aria-pressed="enlarged" @click="enlarged = !enlarged">{{ enlarged ? '适应窗口' : '原图尺寸' }}</button>
        <button type="button" autofocus @click="viewer.close()">关闭 ×</button>
      </header>
      <div class="photo-viewer-body" :class="{ enlarged }">
        <img :src="src" alt="完整训练照片，可切换原图尺寸滚动查看" />
      </div>
      <p>支持 Esc 关闭 · 原图尺寸下可滚动查看</p>
    </dialog>
  </div>
</template>

<style scoped>
.training-photo { min-width: 0; }
.photo-preview { display: block; position: relative; width: 100%; padding: 0; background: #080808; color: var(--p5-white); cursor: zoom-in; }
.photo-preview img { display: block; width: 100%; height: clamp(200px, 28vw, 340px); object-fit: contain; }
.photo-caption { display: block; padding: 8px; font-size: 12px; color: #ccc; border-top: 1px solid #333; }
.photo-error { display: block; padding: 32px 12px; line-height: 1.6; }
button:focus-visible { outline: 2px solid var(--p5-red); outline-offset: 3px; }
.photo-viewer { width: min(1100px, 94vw); max-width: 94vw; height: 90dvh; max-height: 90dvh; margin: auto; padding: 0; border: 1px solid #ddd; background: #101012; color: #fff; }
.photo-viewer[open] { display: flex; flex-direction: column; }
.photo-viewer::backdrop { background: rgba(0, 0, 0, .85); }
.photo-viewer header { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; padding: 12px 16px; border-bottom: 2px solid var(--p5-red); }
.photo-viewer strong { margin-right: auto; }
.photo-viewer button { padding: 8px 12px; border: 1px solid #777; color: #fff; background: #151518; }
.photo-viewer-body { flex: 1; min-height: 0; overflow: auto; padding: 12px; }
.photo-viewer-body img { display: block; margin: auto; width: auto; height: auto; max-width: 100%; max-height: 70dvh; object-fit: contain; }
.photo-viewer-body.enlarged img { margin: 0; max-width: none; max-height: none; }
.photo-viewer p { padding: 8px 16px; color: #bbb; font-size: 12px; }
</style>
