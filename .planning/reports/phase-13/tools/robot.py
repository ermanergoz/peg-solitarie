#!/usr/bin/env python3
"""robot.py <serial> [--read-only] [--leave N]  : read board from screen, solve to 1 peg, tap the moves.
--leave N stops N moves before the end (to leave a game in progress)."""
import sys,subprocess,os,io,time
import numpy as np
from PIL import Image
from scipy import ndimage
ADB=os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
def adb(ser,*a): return subprocess.run([ADB,"-s",ser,*a],capture_output=True).stdout
def shot(ser): return np.asarray(Image.open(io.BytesIO(adb(ser,"exec-out","screencap","-p"))).convert("RGB")).astype(int)
def read(ser):
    a=shot(ser); H,W,_=a.shape
    y0=int(H*0.12); reg=a[y0:int(H*0.95)]
    r,g,b=reg[...,0],reg[...,1],reg[...,2]
    peg=(r>200)&(r-g>60)&(b>g)          # candy pink
    lab,n=ndimage.label(peg); pegs=[]
    for i,sl in enumerate(ndimage.find_objects(lab)):
        h=sl[0].stop-sl[0].start; w=sl[1].stop-sl[1].start
        if h>40 and w>40 and 0.7<h/w<1.4: pegs.append(((sl[1].start+sl[1].stop)/2,(sl[0].start+sl[0].stop)/2+y0,w))
    # panel colour = modal colour around the pegs' bounding area; dimples = small blobs differing from panel
    q=(reg//4).reshape(-1,3); 
    # find panel: most common colour in central band
    cy=reg.shape[0]//2; band=(reg[cy-400:cy+400]//4).reshape(-1,3)
    vals,cnt=np.unique(band,axis=0,return_counts=True); panel=vals[cnt.argmax()]*4
    diff=np.abs(reg-panel).sum(axis=2)
    inpanel=diff<14
    # panel mask filled -> board area
    filled=ndimage.binary_fill_holes(ndimage.binary_opening(inpanel,iterations=3))
    lab2,n2=ndimage.label(filled); sizes=ndimage.sum(filled,lab2,range(1,n2+1)); board=(lab2==(1+int(np.argmax(sizes))))
    cand=board&(~inpanel)&(~peg)
    cand=ndimage.binary_opening(cand,iterations=2)
    lab3,n3=ndimage.label(cand); holes=[]
    for sl in ndimage.find_objects(lab3):
        h=sl[0].stop-sl[0].start; w=sl[1].stop-sl[1].start
        if 15<h<90 and 15<w<90 and 0.6<h/w<1.6: holes.append(((sl[1].start+sl[1].stop)/2,(sl[0].start+sl[0].stop)/2+y0))
    pts=[(x,y,1) for x,y,_ in pegs]+[(x,y,0) for x,y in holes if all(abs(x-p[0])+abs(y-p[1])>50 for p in pegs)]
    xs=sorted(p[0] for p in pts); 
    d=[b-a for a,b in zip(xs,xs[1:]) if b-a>40]; pitch=min(d) if d else 130
    ys=sorted(p[1] for p in pts); d2=[b-a for a,b in zip(ys,ys[1:]) if b-a>40]
    if d2: pitch=min(pitch,min(d2))
    ox=min(p[0] for p in pts); oy=min(p[1] for p in pts)
    cells={}
    for x,y,v in pts:
        c=round((x-ox)/pitch); rr=round((y-oy)/pitch); cells[(rr,c)]=(v,x,y)
    return cells,pitch
def show(cells):
    R=max(k[0] for k in cells)+1; C=max(k[1] for k in cells)+1
    for r in range(R): print("".join({None:" ",0:".",1:"o"}[cells.get((r,c),(None,))[0]] for c in range(C)))
def solve(cells):
    idx={k:i for i,k in enumerate(sorted(cells))}; keys=sorted(cells)
    moves=[]
    for (r,c) in keys:
        for dr,dc in ((0,1),(0,-1),(1,0),(-1,0)):
            m=(r+dr,c+dc); t=(r+2*dr,c+2*dc)
            if m in idx and t in idx: moves.append((1<<idx[(r,c)],1<<idx[m],1<<idx[t],(r,c),t))
    start=sum(1<<idx[k] for k in keys if cells[k][0]==1)
    seen=set(); path=[]
    sys.setrecursionlimit(10000)
    def dfs(s):
        if s&(s-1)==0: return True
        if s in seen: return False
        seen.add(s)
        for a,m,t,src,dst in moves:
            if s&a and s&m and not s&t:
                path.append((src,dst))
                if dfs(s^a^m^t): return True
                path.pop()
        return False
    return path if dfs(start) else None
if __name__=="__main__":
    ser=sys.argv[1]; cells,pitch=read(ser); show(cells); print("pegs",sum(v[0] for v in cells.values()),"cells",len(cells),"pitch",round(pitch))
    if "--read-only" in sys.argv: sys.exit()
    t=time.time(); p=solve(cells); print("solution",None if p is None else len(p),"moves in",round(time.time()-t,1),"s")
    if p is None: sys.exit(2)
    leave=int(sys.argv[sys.argv.index("--leave")+1]) if "--leave" in sys.argv else 0
    for src,dst in p[:len(p)-leave]:
        for k in (src,dst):
            _,x,y=cells[k]; adb(ser,"shell","input","tap",str(int(x)),str(int(y))); time.sleep(0.35)
        time.sleep(0.5)
    print("done")
