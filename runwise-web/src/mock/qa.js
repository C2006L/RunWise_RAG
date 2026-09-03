// mock AI 问答（工程计划 5.1 契约 / 5.2 职责，行为对齐后端 QaController / QaServiceImpl）
// - ask：延迟 800ms 返回 QaAskResult { answer, sources, safetyTip? }
//   · 伤病关键词（对齐后端正则 [疼痛伤膝盖小腿脚踝拉伤]）→ 附加 safetyTip，
//     sources 返回模拟文档名（模拟 RAG 检索形态）
//   · 其余问题 → sources 返回模型标识（对齐后端 LLM 直连形态）
//   · 同时落一条历史记录（模拟后端落库，供 feedback / 分页查询）
// - 历史记录集：预置 8 条，经 localStorage 持久化（镜像后端数据库，
//   刷新后反馈状态经 history 仍保持），createTime 倒序分页；feedback 按 id 更新
// - 分类 / 热门：与后端 QaServiceImpl 静态数据保持一致
import { verifyAccess } from "./user";

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// ===== 伤病关键词与安全提示（文案对齐后端 QaServiceImpl.ask） =====
// 词级匹配而非单字匹配：避免「马拉松」「拉伸」等含「拉」的正常问题被误判为伤病
const INJURY_WORDS = [
  "疼",
  "痛",
  "受伤",
  "伤痛",
  "伤病",
  "膝盖",
  "小腿",
  "脚踝",
  "拉伤",
  "扭伤",
];
const SAFETY_TIP = "以上建议仅供参考，若疼痛持续或加重请及时就医。";

// ===== sources 两种形态：模型标识（直连）/ 模拟文档名（RAG 检索） =====
// 注意：'Qwen3.5-Flash大模型生成' 为 mock 预置文案，真实接入后由后端
// /api/qa/ask 返回的 sources 直接替换（LLM 直连返回模型标识，RAG 返回检索文档名）
const MODEL_SOURCES = ["Qwen3.5-Flash大模型生成"];
const INJURY_DOC_SOURCES = [
  "《跑步伤病预防手册》第2节",
  "《马拉松训练指南》第4章",
];

// ===== 分类（对齐后端静态数据） =====
const CATEGORIES = [
  { name: "训练计划", icon: "📋" },
  { name: "装备选择", icon: "👟" },
  { name: "伤痛预防", icon: "🩹" },
  { name: "跑步技术", icon: "🏃" },
];

// ===== 热门问题（对齐后端静态数据，getHotQuestions 按 limit 截取） =====
const HOT_QUESTIONS = [
  { question: "初学者应该怎么开始跑步？", category: "beginner" },
  { question: "跑步膝盖疼怎么办？", category: "injury" },
  { question: "如何选择合适的跑鞋？", category: "equipment" },
  { question: "5K训练计划是什么？", category: "training" },
  { question: "跑步时心率多少合适？", category: "training" },
  { question: "跑步前吃什么？", category: "nutrition" },
  { question: "如何提高跑步配速？", category: "training" },
  { question: "跑步后怎么拉伸恢复？", category: "recovery" },
];

// ===== 回答模板（200-400 字，按问题关键词路由） =====
const ANSWERS = {
  injury:
    "膝盖疼痛是跑者最常见的困扰之一，多数与训练负荷增长过快、臀部及大腿力量不足有关。建议按以下步骤处理：①立即减量：暂停速度课与长距离，改为游泳或骑行等低冲击运动维持有氧；②冰敷与抬高：急性期每次15-20分钟，每天2-3次；③力量补充：重点加强臀中肌与股四头肌，靠墙静蹲、单腿臀桥各3组；④检查跑鞋里程，超过800公里应及时更换。若休息一周后疼痛未缓解，或出现肿胀、关节弹响与打软腿，请务必就医进行影像检查，明确诊断后再制定恢复计划。恢复跑步后遵循每周增量不超过10%的原则，让膝盖逐步重新适应负荷。",
  training:
    "对初学者而言，科学起步比热情更重要。推荐「跑走结合」入门法：前两周每次运动30分钟，其中慢跑3分钟+快走2分钟交替，每周3次，隔天进行让身体充分恢复；第三至四周逐渐把跑段延长到5-8分钟；第五周起尝试连续慢跑20分钟。配速以能完整说句子为度（约为最大心率的65%-75%），宁慢勿快。每周总跑量增幅控制在10%以内，每训练三周安排一个减量恢复周。跑前动态热身5-10分钟，跑后静态拉伸10分钟。坚持八周后，多数初学者可以舒适地完成连续30分钟慢跑，为5K目标打下扎实基础。",
  equipment:
    "选跑鞋的核心是匹配足型与用途，而不是追逐旗舰款。第一步确认足弓类型：湿脚踩纸观察足印，正常足弓选缓冲均衡型；低足弓或扁平足倾向选择支撑型；高足弓适合缓震型。第二步按用途：初学者与日常慢跑选缓冲训练鞋即可，竞速碳板鞋留到有稳定跑量后再考虑。第三步试穿要点：下午试鞋（脚部略微膨胀），前掌留一指空间，后跟贴合不滑。预算方面，600-900元价位的次旗舰往往性价比最高。切记：没有「最好」的鞋，只有最适合你的鞋，务必试穿后再决定。",
  technique:
    "良好的跑姿能显著降低受伤风险并提升跑步经济性。要点如下：①身体姿态：躯干略微前倾（从脚踝开始而非弯腰），核心收紧，视线望向前方15-20米；②步频优先：理想步频约170-180步/分钟，小步快频比大步幅更省力也更安全；③着地方式：让落点靠近重心正下方，脚掌中前部先触地，避免脚跟重重砸向远前方；④摆臂：手肘约90度前后摆动，避免左右晃动；⑤呼吸：初期采用三步一吸两步一呼的节奏，与步频同步。建议每次训练只专注改进一个要点，并可用慢动作视频自查对照。",
  generic:
    "这是一个很好的问题。跑步能力的提升是训练、营养与恢复三者共同作用的结果。从训练角度看，建议保持每周3-4次的规律运动，将有氧慢跑、力量训练与柔韧性练习合理搭配；从恢复角度看，睡眠是性价比最高的「补剂」，成年人建议保证7-8小时；从营养角度看，日常均衡饮食即可满足多数跑者需求，长距离训练后注意补充碳水与蛋白质。如果你能补充更多背景信息（例如目前的跑量、配速、训练目标或具体的困惑点），我可以给出更有针对性的建议。循序渐进，保持耐心，进步自然会来。",
};

// 非伤病类问题的关键词路由（顺序：装备 → 技术 → 训练 → 兜底）
const KEYWORD_ROUTES = [
  { template: "equipment", words: ["鞋", "装备", "穿戴", "衣服", "手表"] },
  {
    template: "technique",
    words: ["姿势", "步频", "呼吸", "着地", "落地", "摆臂", "跑姿"],
  },
  {
    template: "training",
    words: [
      "计划",
      "训练",
      "入门",
      "初学",
      "开始",
      "配速",
      "心率",
      "跑量",
      "拉伸",
      "恢复",
      "备赛",
    ],
  },
];

function pickTemplate(question) {
  if (INJURY_WORDS.some((w) => question.includes(w))) return "injury";
  for (const route of KEYWORD_ROUTES) {
    if (route.words.some((w) => question.includes(w))) return route.template;
  }
  return "generic";
}

// ===== 时间工具：'YYYY-MM-DD HH:mm:ss' =====
function pad(n) {
  return String(n).padStart(2, "0");
}

function formatDateTime(d) {
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

// 生成 n 天前指定时刻的时间串（供预置历史记录，避免硬编码日期漂移）
function pastTime(daysAgo, hh, mm) {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  d.setHours(hh, mm, 0, 0);
  return formatDateTime(d);
}

// ===== 预置 5 条历史记录（createTime 倒序，sources 混合两种形态；
//       早期测试数据（"111" 等）随存储 key 升级一并废弃） =====
const PRESET_HISTORY = [
  {
    question: "初学者应该怎么开始跑步？",
    answer: ANSWERS.training,
    sources: JSON.stringify(MODEL_SOURCES),
    feedback: 0,
    createTime: pastTime(1, 21, 30),
  },
  {
    question: "如何选择合适的跑鞋？",
    answer: ANSWERS.equipment,
    sources: JSON.stringify([
      "《跑步装备选购指南》第2章",
      "《跑者世界》装备评测",
    ]),
    feedback: 1,
    createTime: pastTime(2, 19, 12),
  },
  {
    question: "跑步时心率多少合适？",
    answer:
      "心率是衡量训练强度的核心指标。最大心率可用「220 − 年龄」粗略估算，训练时建议参考心率区间：轻松跑维持在最大心率的65%-75%，是打有氧基础的黄金区间；节奏跑约80%-88%；间歇训练可短时触及90%以上，但占比不宜过大。对多数健康跑者而言，日常80%的跑量应保持在轻松区间——即能边跑边完整说句子的强度。若没有心率表，用「谈话测试」同样有效：喘到说不出整句话，说明强度偏高。晨起静息心率若连续数日升高5次以上，往往是疲劳累积的信号，应及时减量恢复。",
    sources: JSON.stringify(MODEL_SOURCES),
    feedback: 0,
    createTime: pastTime(3, 7, 45),
  },
  {
    question: "跑步膝盖疼怎么办？",
    answer: ANSWERS.injury,
    sources: JSON.stringify(INJURY_DOC_SOURCES),
    feedback: 0,
    createTime: pastTime(4, 20, 3),
  },
  {
    question: "如何提高跑步配速？",
    answer:
      "提高配速的关键不是每次都拼命跑快，而是科学安排训练强度。建议从三方面入手：①打好有氧基础：80%的跑量保持轻松慢跑，提升毛细血管密度与线粒体数量，这是速度的地基；②加入节奏跑：每周一次「舒适的痛苦」区间训练，例如20分钟乳酸阈值跑，让身体学会高效清除代谢废物；③间歇刺激：隔周进行400-800米重复跑，配速略快于目标，组间慢跑恢复。此外别忘了力量训练——深蹲、弓步、提踵能让跑步经济性提升约3%-5%。记住原则：每次训练只解决一个问题，增量循序渐进。",
    sources: JSON.stringify(MODEL_SOURCES),
    feedback: -1,
    createTime: pastTime(5, 18, 40),
  },
].map((record, idx) => ({ id: idx + 1, userId: "10086", ...record }));

// ===== 历史记录集：localStorage 持久化（镜像后端落库行为，刷新后反馈状态经 history 仍保持） =====
// 首次访问写入预置 5 条；ask / feedback 的变更实时持久化；存储损坏时回退预置数据
// v2.0 去重规则：历史按「日期 + 问题」唯一——ask 重复提问当日不落新记录（覆盖更新
// 反馈与时间），预置数据清洗同口径，杜绝同一条记录重复出现 3 次的问题
// key 带 -v2：旧 key 中的测试数据（"111" 等）直接废弃，不再混入历史列表
const STORAGE_KEY = "runwise-mock-qa-history-v2";

// 去重键：createTime 前 10 位（日期）+ 问题文本
function dedupeKey(record) {
  return `${(record.createTime || "").slice(0, 10)}|${record.question}`;
}

// 清洗：同键保留最新一条（列表已按时间倒序，后者覆盖前者）
function dedupeList(list) {
  const seen = new Set();
  const result = [];
  for (const record of list) {
    const key = dedupeKey(record);
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(record);
  }
  return result;
}

function initStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const list = dedupeList(JSON.parse(raw));
      if (Array.isArray(list) && list.length) {
        return { list, nextId: Math.max(...list.map((r) => r.id)) + 1 };
      }
    }
  } catch {
    // 存储损坏时回退预置数据
  }
  const preset = dedupeList([...PRESET_HISTORY]);
  return { list: preset, nextId: preset.length + 1 };
}

const initialStore = initStore();
let historyStore = initialStore.list;
let nextId = initialStore.nextId;

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(historyStore));
  } catch {
    // 隐私模式等写入失败时静默降级为内存态
  }
}
persist(); // 首次访问时落预置数据

// 提问：延迟 800ms，生成回答并落一条历史记录
export async function ask(question, token) {
  verifyAccess(token);
  await sleep(800);
  const q = (question || "").trim();
  const template = pickTemplate(q);
  const isInjury = template === "injury";

  const result = {
    answer: ANSWERS[template],
    sources: isInjury ? [...INJURY_DOC_SOURCES] : [...MODEL_SOURCES],
    ...(isInjury ? { safetyTip: SAFETY_TIP } : {}),
  };

  // v2.0 去重：同日同问不落新记录——把已存在记录顶到首位并覆盖回答 / 反馈 / 时间
  const today = formatDateTime(new Date()).slice(0, 10);
  const existing = historyStore.find((r) => dedupeKey(r) === `${today}|${q}`);
  if (existing) {
    existing.answer = result.answer;
    existing.sources = JSON.stringify(result.sources);
    existing.createTime = formatDateTime(new Date());
    historyStore = [existing, ...historyStore.filter((r) => r !== existing)];
  } else {
    historyStore.unshift({
      id: nextId++,
      userId: "10086",
      question: q,
      answer: result.answer,
      sources: JSON.stringify(result.sources),
      feedback: 0,
      createTime: formatDateTime(new Date()),
    });
  }
  persist();

  return result;
}

// 历史分页：createTime 倒序，返回 { list, total, pageNum, pageSize }（对齐后端 PageResult）
export async function getHistory(pageNum = 1, pageSize = 10, token) {
  verifyAccess(token);
  await sleep(200);
  const page = Math.max(1, pageNum);
  const size = Math.max(1, pageSize);
  const start = (page - 1) * size;
  return {
    list: historyStore.slice(start, start + size),
    total: historyStore.length,
    pageNum: page,
    pageSize: size,
  };
}

// 分类
export async function getCategories(token) {
  verifyAccess(token);
  await sleep(120);
  return CATEGORIES.map((c) => ({ ...c }));
}

// 热门问题：按 limit 截取
export async function getHotQuestions(limit = 5, token) {
  verifyAccess(token);
  await sleep(120);
  return HOT_QUESTIONS.slice(0, Math.max(1, limit)).map((q) => ({ ...q }));
}

// 反馈：按 id 更新（0 未反馈 / 1 赞 / -1 踩），记录不存在抛错（对齐后端 NOT_FOUND）
export async function feedback(id, value, token) {
  verifyAccess(token);
  await sleep(150);
  const record = historyStore.find((r) => r.id === id);
  if (!record) {
    const err = new Error("问答记录不存在");
    err.response = { status: 404, data: { message: "问答记录不存在" } };
    throw err;
  }
  record.feedback = value;
  persist();
  return null;
}
