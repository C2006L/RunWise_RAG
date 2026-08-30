import { defineStore } from 'pinia'
import * as checkinApi from '../api/checkin'
import { formatDate } from '../composables/useFormatDate'

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
      this.loading = true
      try {
        const list = await checkinApi.getCheckinList(startDate, endDate)
        const patch = {}
        list.forEach((record) => {
          patch[record.date] = record
        })
        this.records = { ...this.records, ...patch }
      } finally {
        this.loading = false
      }
    },
    // 提交打卡（新建 / 补录）：成功后写入 records → 红点与详情即时刷新，无需整页刷新
    async submit(payload) {
      this.submitting = true
      try {
        const record = await checkinApi.createCheckin(payload)
        this.records = { ...this.records, [record.date]: record }
        return record
      } finally {
        this.submitting = false
      }
    },
  },
})
