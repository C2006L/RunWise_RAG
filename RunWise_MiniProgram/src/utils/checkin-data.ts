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

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

export function toDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function buildInitialRecords(): CheckinRecord[] {
  const now = new Date();
  const offsets = [1, 3, 5, 8, 11, 14, 17, 20];
  const distances = [5.2, 4.0, 6.3, 3.5, 7.1, 5.0, 4.6, 6.0];
  const durations = [32, 25, 40, 22, 46, 31, 28, 38];
  const moods = ["适中", "轻松", "适中", "轻松", "吃力", "适中", "轻松", "适中"];
  const remarks = ["操场 5K", "晨跑恢复", "节奏跑", "慢跑放松", "长距离", "夜跑", "间歇训练", "轻松跑"];

  return offsets.map((offset, index) => {
    const date = new Date(now);
    date.setDate(date.getDate() - offset);
    return {
      id: index + 1,
      checkinDate: toDateKey(date),
      distance: distances[index],
      duration: durations[index],
      pace: Number((durations[index] / distances[index]).toFixed(2)),
      mood: moods[index],
      remark: remarks[index],
      imageUrl: "",
    };
  });
}

export function loadCheckinRecords(): CheckinRecord[] {
  const saved = uni.getStorageSync(STORAGE_KEY);
  if (Array.isArray(saved)) return saved as CheckinRecord[];

  const initial = buildInitialRecords();
  uni.setStorageSync(STORAGE_KEY, initial);
  return initial;
}

export function saveCheckinRecords(records: CheckinRecord[]): void {
  uni.setStorageSync(STORAGE_KEY, records);
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
