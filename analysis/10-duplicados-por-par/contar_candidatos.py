import csv, sys, collections
LV = {'6814dafa7d73e7209a13d382':'A1','6814dafa7d73e7209a13d218':'A2','6814dafa7d73e7209a13d2b5':'B1','6814dafa7d73e7209a13d3ef':'B2'}
def num(s): return float(s.replace(',', '.'))
rows = list(csv.DictReader(open(sys.argv[1]), delimiter='\t'))
for r in rows:
    r['level'] = LV[r['level']]; r['wildcard']=int(r['wildcard']); r['ws']=num(r['wordSimilarity']); r['cos']=num(r['cosine'])
wild = [r for r in rows if r['wildcard']]
rest = [r for r in rows if not r['wildcard']]
print('same-request pairs', len(rows), 'wildcard pairs', len(wild), 'not settled by wildcard', len(rest))
print('wildcard pairs by level', dict(collections.Counter(r['level'] for r in wild)))
def copies(rule):
    by_copy = collections.defaultdict(list)
    for r in rows: by_copy[(r['knowledgeId'], r['copyIndex'])].append(r)
    out = collections.Counter(); cond = collections.Counter()
    for k, prs in by_copy.items():
        prs.sort(key=lambda r: int(r['keptIndex']))
        for r in prs:
            c = rule(r)
            if c:
                out[r['level']] += 1; cond[c]+=1; break
    return out, cond
o, c = copies(lambda r: 'W' if r['wildcard'] else ('S' if r['cos'] >= 0.95 - 1e-9 else None))
print('current version copies', dict(o), sum(o.values()), dict(c))
levels = ['A1','A2','B1','B2']
print()
print('candidates for the pair judge (not wildcard), by cosine threshold')
print('thr   ' + ' '.join('%6s'%l for l in levels) + '  total | union with ws>=0.6')
for t in [0.95,0.92,0.90,0.88,0.86,0.85,0.84,0.82,0.80,0.78,0.75,0.70]:
    cnt = collections.Counter(r['level'] for r in rest if r['cos'] >= t)
    uni = sum(1 for r in rest if r['cos'] >= t or r['ws'] >= 0.6 - 1e-9)
    print('%.2f ' % t + ' '.join('%6d' % cnt[l] for l in levels) + '  %5d | %5d' % (sum(cnt.values()), uni))
ws = collections.Counter(r['level'] for r in rest if r['ws'] >= 0.6 - 1e-9)
print('ws>=0.6 (route without Ollama):', dict(ws), sum(ws.values()))
band = [r for r in rest if 0.90 <= r['cos'] < 0.95]
print('pairs in [0.90,0.95):', len(band), dict(collections.Counter(r['level'] for r in band)))
# how many exercises / topics touched at 0.85
for t in (0.90, 0.85, 0.80):
    cand = [r for r in rest if r['cos'] >= t]
    print(t, 'topics with a candidate', len({r['knowledgeId'] for r in cand}), 'copies that would be judged', len({(r['knowledgeId'], r['copyIndex']) for r in cand}))
