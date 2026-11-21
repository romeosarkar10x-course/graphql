#!/usr/bin/env python3
"""Builds starter/Sim.java and solution/Sim.java from Sim.java.in and the files beside it. Run after editing them."""
import base64, json, os, shutil
here = os.path.dirname(os.path.abspath(__file__)); lab = os.path.dirname(os.path.dirname(here))
rd = lambda *p: open(os.path.join(here, *p), encoding='utf-8').read()
clients = lambda d: {f: rd(d, f) for f in sorted(os.listdir(os.path.join(here, d)))}
crs = []
for n in range(1, 7):
    cs = {f: rd('cr', f) for f in sorted(os.listdir(os.path.join(here, 'cr'))) if f.startswith('%02d-' % n)}
    crs.append({'text': rd('cr', '%02d.md' % n), 'clients': {'cr' + k: v for k, v in cs.items()}})
blob = {'v0': rd('v0.graphqls'), 'v0Clients': clients('v0'), 'v1Clients': clients('v1'), 'cr': crs}
java = rd('Sim.java.in').replace('@@DATA@@', base64.b64encode(json.dumps(blob).encode()).decode())
for d in ('starter', 'solution'):
    open(os.path.join(lab, d, 'Sim.java'), 'w', encoding='utf-8').write(java)
    shutil.copy(os.path.join(here, 'v0.graphqls'), os.path.join(lab, d, 'v0.graphqls'))
    os.makedirs(os.path.join(lab, d, 'clients', 'v0'), exist_ok=True)
    for f, t in clients('v0').items(): open(os.path.join(lab, d, 'clients', 'v0', f), 'w').write(t)
print('built Sim.java,', len(java), 'chars')
