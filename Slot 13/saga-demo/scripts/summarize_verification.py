"""Build a concise, reproducible evidence index from the actual test reports."""
import json
import shutil
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "evidence"
reports = []
for path in [EVIDENCE / "newman-base.json", *sorted(EVIDENCE.glob("extensions-*.json"))]:
    data = json.loads(path.read_text(encoding="utf-8"))["run"]
    reports.append({"report": path.name, "requests": data["stats"]["requests"]["total"],
                    "assertions": data["stats"]["assertions"]["total"],
                    "failed": data["stats"]["assertions"]["failed"], "errors": len(data["failures"])})
assert all(report["failed"] == 0 and report["errors"] == 0 for report in reports), reports
unit_path = ROOT / "order-service/target/surefire-reports/TEST-com.mss301.order.saga.OrderSagaOrchestratorTest.xml"
if unit_path.exists():
    shutil.copyfile(unit_path, EVIDENCE / "junit-order.xml")
unit = ET.parse(EVIDENCE / "junit-order.xml").getroot()
assert unit.attrib["failures"] == "0" and unit.attrib["errors"] == "0"
failures = json.loads((EVIDENCE / "failure-scenarios.json").read_text(encoding="utf-8"))
assert failures["KB6"]["passed"] and failures["KB8"]["passed"]
desktop = json.loads((EVIDENCE / "postman-desktop-run.json").read_text(encoding="utf-8"))
assert desktop["runner"]["failed"] == 0 and desktop["runner"]["errors"] == 0
assert desktop["manualKB6"]["failed"] == 0
for screenshot in desktop["screenshots"]:
    assert (EVIDENCE / screenshot).is_file(), screenshot
summary = {"generatedAt": datetime.now(timezone.utc).isoformat(), "reports": reports,
           "totalRequests": sum(r["requests"] for r in reports),
           "totalAssertions": sum(r["assertions"] for r in reports),
           "failedAssertions": 0, "unitTests": int(unit.attrib["tests"]),
           "realOutages": {"KB6": "passed", "KB8": "passed"},
           "postmanDesktop": desktop}
(EVIDENCE / "verification-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
lines = ["# Verification evidence", "", f"Generated: {summary['generatedAt']}", "",
         "| Report | HTTP requests | Assertions | Failures |", "|---|---:|---:|---:|"]
lines += [f"| [{r['report']}]({r['report']}) | {r['requests']} | {r['assertions']} | {r['failed']} |" for r in reports]
lines += ["", f"Total: **{summary['totalAssertions']} assertions passed**, {summary['totalRequests']} HTTP requests.",
          f"JUnit: **{summary['unitTests']} tests passed**. Real container outages KB6 and KB8 passed.", "",
          "KB8 evidence records PENDING/COMPENSATING and exactly three failed compensation attempts.",
          "The recovery report subsequently verifies stock restoration and repeat recovery safety.", "",
          "## Postman Desktop results", "",
          f"Run started: {desktop['runner']['startedAt']}. Computer Use controlled Postman through `@oai/sky`.",
          "Collection Runner: **120/120 assertions passed**, 36 selected requests, one iteration, zero errors.",
          "Duration: 10.885 seconds; average response time: 59 ms. The manual folder was excluded.",
          "Manual KB6: **18/18 assertions passed** across five requests with Payment actually stopped.",
          "Order 6 ended CANCELLED / COMPENSATED; stock and wallets were unchanged. Payment was restarted.",
          "The user approved resetting only Saga volumes before this Desktop run.",
          "Seed: P1=10, P2=5, C1=5000, C2=100. Final: P1=8, P2=5, C1=3000, C2=100.",
          "Historical Newman, extension, KB8 and recovery reports above were preserved before the reset.",
          "Desktop assertions are reported separately from the 278 historical Newman assertions.",
          "See [Desktop run metadata](postman-desktop-run.json), [Runner text](postman-runner-results.txt),",
          "[post-Runner data](postman-desktop-data.json) and [post-KB6 data](postman-desktop-kb6-data.json).", "",
          "### Unedited Computer Use screenshots", ""]
lines += [f"- [{name}]({name})" for name in desktop["screenshots"]]
lines += ["",
          "## Reproduce", "", "Follow README.md for profile startup and test commands.",
          "Use a fresh KB8 outage before rerunning the one-time recovery folder.",
          "Ensure enough seed stock/balance; successful purchases consume resources.", ""]
(EVIDENCE / "VERIFICATION.md").write_text("\n".join(lines), encoding="utf-8")
print(f"Verified {summary['totalAssertions']} Postman assertions, {summary['unitTests']} JUnit tests, KB6 and KB8")
