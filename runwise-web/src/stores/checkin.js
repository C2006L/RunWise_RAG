import { defineStore } from 'pinia'
import * as checkinApi from '../api/checkin'
import { formatDate } from '../composables/useFormatDate'
import {useUserStore} from './user'
import {mergeCheckinRange,replaceCheckin} from '../utils/checkinCache'

// 打卡数据 store（工程计划 2.2 / 4.4）：
// - records：按日期索引的记录 Map（'YYYY-MM-DD' → CheckinRecord），翻月加载后合并不覆盖
// - selectedDate：日历当前选中日期（默认今天）
export const useCheckinStore = defineStore('checkin', {
  state: () => ({
    records: {},
    selectedDate: formatDate(new Date()),
    loading: false,
    submitting: false,
  }),
  getters: {
    selectedRecord: (state) => state.records[state.selectedDate] || null,
  },
  actions: {
    select(date) {
      this.selectedDate = date
    },
    // 拉取一段闭区间日期的记录并合并进 records（翻月不丢已加载月份的红点）
    async loadRange(startDate, endDate) {
      const owner=useUserStore().userInfo?.id
      this.loading = true
      try {
        const list = await checkinApi.getCheckinList(startDate, endDate)
        if(owner!==useUserStore().userInfo?.id) return
        this.records = mergeCheckinRange(this.records,list,startDate,endDate)
      } finally {
        this.loading = false
      }
    },
    // 提交打卡（新建 / 补录）：成功后写入 records → 红点与详情即时刷新，无需整页刷新
    async submit(payload) {
      const owner=useUserStore().userInfo?.id
      this.submitting = true
      try {
        const record = await checkinApi.createCheckin(payload)
        if(owner===useUserStore().userInfo?.id) this.records = replaceCheckin(this.records,record)
        return record
      } finally {
        this.submitting = false
      }
    },
    async update(id, payload) {
      const owner=useUserStore().userInfo?.id
      this.submitting = true
      try {
        const record = await checkinApi.updateCheckin(id, payload)
        if(owner===useUserStore().userInfo?.id) this.records = replaceCheckin(this.records,record)
        return record
      } finally {
        this.submitting = false
      }
    },
    uploadImage(file) {
      return checkinApi.uploadCheckinImage(file)
    },
  },
})
