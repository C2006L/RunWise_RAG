import { request, isLoggedIn } from './request';

export interface CheckinRecord {
  id: number;
  checkinDate: string;
  distance: number;
  duration: number;
  pace: number;
  mood: string;
  remark: string;
  imageUrl: string;
}

const STORAGE_KEY = "runwise:checkin-records";
const cacheKey = () => `${STORAGE_KEY}:user:${uni.getStorageSync('userId') || 'anonymous'}`;

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

export function toDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function loadCheckinRecords(): CheckinRecord[] {
  if(!isLoggedIn()) return [];
  const saved = uni.getStorageSync(cacheKey());
  if (Array.isArray(saved)) return saved as CheckinRecord[];

  return [];
}

export function saveCheckinRecords(records: CheckinRecord[]): void {
  uni.setStorageSync(cacheKey(), records);
}

export async function syncCheckinRecords(): Promise<CheckinRecord[]> {
  if(!isLoggedIn()) return [];
  const user=uni.getStorageSync('userId');
  const rows: CheckinRecord[]=[];
  let page=1,total=0;
  do {
    const data=await request<{list:CheckinRecord[];total:number}>(`/api/checkin/list?pageNum=${page}&pageSize=200`);
    rows.push(...data.list);total=data.total;page++;
    if(!data.list.length) break;
  } while(rows.length<total);
  if(user!==uni.getStorageSync('userId')) return [];
  saveCheckinRecords(rows);return rows;
}

export function summarizeCheckins(records: CheckinRecord[]) {
  const dateSet = new Set(records.map((item) => item.checkinDate));
  const totalDistance = Number(records.reduce((sum, item) => sum + item.distance, 0).toFixed(1));

  const cursor = new Date();
  if (!dateSet.has(toDateKey(cursor))) cursor.setDate(cursor.getDate() - 1);
  let currentStreak = 0;
  while (dateSet.has(toDateKey(cursor)) && currentStreak < 3650) {
    currentStreak++;
    cursor.setDate(cursor.getDate() - 1);
  }

  const sortedDates = Array.from(dateSet).sort();
  let longestStreak = 0;
  let runningStreak = 0;
  let previousTime: number | null = null;
  sortedDates.forEach((key) => {
    const time = new Date(`${key}T00:00:00`).getTime();
    runningStreak = previousTime !== null && time - previousTime === 86400000 ? runningStreak + 1 : 1;
    longestStreak = Math.max(longestStreak, runningStreak);
    previousTime = time;
  });

  return {
    totalDistance,
    totalCount: records.length,
    currentStreak,
    longestStreak,
  };
}
