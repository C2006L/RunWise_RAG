export interface StoredQaMessage {
  role: "user" | "assistant";
  content: string;
  missionTitle?: string;
  sources?: string[];
  safetyTip?: string;
}

export interface QaSession {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: StoredQaMessage[];
}

const SESSIONS_KEY = "runwise:qa-sessions:v1";
const CURRENT_SESSION_KEY = "runwise:qa-current-session:v1";

function isValidSession(value: unknown): value is QaSession {
  if (!value || typeof value !== "object") return false;
  const session = value as QaSession;
  return (
    typeof session.id === "string" &&
    typeof session.title === "string" &&
    typeof session.createdAt === "string" &&
    typeof session.updatedAt === "string" &&
    Array.isArray(session.messages) &&
    session.messages.every(
      (message) =>
        message &&
        (message.role === "user" || message.role === "assistant") &&
        typeof message.content === "string",
    )
  );
}

export function loadQaSessions(): QaSession[] {
  const saved = uni.getStorageSync(SESSIONS_KEY);
  if (!Array.isArray(saved)) return [];
  return saved
    .filter(isValidSession)
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

export function saveQaSession(session: QaSession): void {
  const sessions = loadQaSessions();
  const index = sessions.findIndex((item) => item.id === session.id);
  if (index >= 0) sessions.splice(index, 1, session);
  else sessions.unshift(session);
  sessions.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  uni.setStorageSync(SESSIONS_KEY, sessions.slice(0, 50));
  setCurrentQaSessionId(session.id);
}

export function findQaSession(id: string): QaSession | undefined {
  return loadQaSessions().find((session) => session.id === id);
}

export function createQaSessionId(): string {
  return `qa-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function getCurrentQaSessionId(): string {
  return uni.getStorageSync(CURRENT_SESSION_KEY) || "";
}

export function setCurrentQaSessionId(id: string): void {
  if (id) uni.setStorageSync(CURRENT_SESSION_KEY, id);
  else uni.removeStorageSync(CURRENT_SESSION_KEY);
}
