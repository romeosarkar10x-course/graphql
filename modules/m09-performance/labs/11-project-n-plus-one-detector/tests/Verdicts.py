"""Read surefire XML reports and compare each fixture test with expected.txt. Prints pass/fail lines."""
import glob, re, sys, xml.etree.ElementTree as ET
reports, expected = sys.argv[1], sys.argv[2]
seen = {}
for f in glob.glob(reports + "/TEST-*Fixtures*.xml"):
    for tc in ET.parse(f).getroot().iter("testcase"):
        bad = tc.find("failure") if tc.find("failure") is not None else tc.find("error")
        msg = "" if bad is None else (bad.get("message") or "") + "\n" + (bad.text or "")
        seen[tc.get("name").split("(")[0]] = ("pass" if bad is None else "fail", msg)
for line in open(expected):
    if line.startswith("#") or not line.strip():
        continue
    name, want, pattern = line.rstrip("\n").split("|", 2)
    got, msg = seen.get(name, ("missing", ""))
    ok = got == want and (want == "pass" or re.search(pattern, msg))
    detail = "" if ok else f"expected {want}{' matching /' + pattern + '/' if pattern else ''}, got {got}: {msg.strip().splitlines()[0][:150] if msg.strip() else ''}"
    print(("ok|" if ok else "FAIL|") + name + "|" + detail)
