"""可选本地真实模型冒烟测试。只用明确标注的测试数据，不连接业务数据库。"""
import json
from datetime import date, timedelta
from decimal import Decimal
from time import monotonic
from rag_api import PlanRequest, generate_plan

def main():
    today = date.today()
    start = today + timedelta(days=1)
    context = {"asOf": str(today), "periodStart": str(today - timedelta(days=27)),
               "recordCount": 12, "totalKm": 80, "totalMinutes": 520,
               "weeklyAverageKm": 20, "longestRunKm": 8,
               "dataNote": "LOCAL TEST FIXTURE：仅用于模型冒烟测试，不是实际用户记录"}
    request = {"goal": "maintain", "startDate": str(start), "sessionsPerWeek": 3,
               "noDiscomfortConfirmed": True, "constraints": "只安排轻松跑和恢复日"}
    started = monotonic()
    plan = generate_plan(PlanRequest(request=request, trainingContext=context))
    days = plan["days"]
    errors = []
    if len(days) != 7:
        errors.append("不是完整7天")
    total = Decimal("0")
    runs = 0
    consecutive = 0
    for i, day in enumerate(days):
        if day.get("date") != str(start + timedelta(days=i)):
            errors.append("日期不匹配")
        km = Decimal(str(day.get("targetKm", -1)))
        rest = day.get("type") == "rest"
        if day.get("type") not in ("easy", "rest") or km < 0 or km > 8 or (rest and km != 0) or (not rest and km <= 0):
            errors.append("类型或距离超出测试边界")
        runs += not rest
        consecutive = 0 if rest else consecutive + 1
        if consecutive > 2:
            errors.append("连续训练过多")
        total += km
    if total > 20 or runs > 3 or runs < 1:
        errors.append("周跑量或次数超出测试边界")
    print(json.dumps({"model": plan["model"], "seconds": round(monotonic()-started, 1),
                      "days": len(days), "totalKm": str(total), "runs": runs,
                      "fixtureChecksPassed": not errors, "errors": errors}, ensure_ascii=False))
    if errors:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
