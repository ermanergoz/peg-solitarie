#!/usr/bin/env python3
"""ui.py <serial> dump | tap "<text>" | shot <name>"""
import sys,subprocess,re,time,os
ADB=os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
S=os.path.dirname(os.path.abspath(__file__))
def adb(ser,*a,**k): return subprocess.run([ADB,"-s",ser,*a],capture_output=True,**k)
def dump(ser):
    # QA fix 2026-09-20: a failed dump used to leave the previous u.xml in place and the tool
    # silently returned a stale screen. Delete first, retry, and fail loudly.
    x=""
    for _ in range(4):
        adb(ser,"shell","rm","-f","/sdcard/u.xml")
        adb(ser,"shell","uiautomator","dump","/sdcard/u.xml")
        x=adb(ser,"exec-out","cat","/sdcard/u.xml").stdout.decode("utf8","ignore")
        if "<hierarchy" in x: break
        time.sleep(1.5)
    else:
        sys.stderr.write("DUMP FAILED (no hierarchy)\n"); sys.exit(3)
    out=[]
    for m in re.finditer(r'<node [^>]*?>',x):
        n=m.group(0)
        g=lambda k:(re.search(k+r'="([^"]*)"',n) or [None,""])[1]
        t,d,b,c,rid=g("text"),g("content-desc"),g("bounds"),g("class"),g("resource-id")
        bb=list(map(int,re.findall(r'\d+',b)))
        out.append((t,d,bb,c.split(".")[-1],rid,g("clickable")))
    return out
def main():
    ser,cmd=sys.argv[1],sys.argv[2]
    if cmd=="dump":
        for t,d,b,c,rid,cl in dump(ser):
            if t or d or rid: print(f"{c:12} {b} text={t!r} desc={d!r} id={rid} click={cl}")
    elif cmd=="tap":
        want=sys.argv[3].lower()
        for t,d,b,c,rid,cl in dump(ser):
            if want in t.lower() or want in d.lower():
                x,y=(b[0]+b[2])//2,(b[1]+b[3])//2; adb(ser,"shell","input","tap",str(x),str(y)); print("tapped",t or d,x,y); return
        print("NOT FOUND",want); sys.exit(1)
    elif cmd=="shot":
        E=os.environ.get("QA_OUT") or os.path.join(os.path.dirname(S),"evidence"); os.makedirs(E,exist_ok=True); p=f"{E}/{sys.argv[3]}.png"; open(p,"wb").write(adb(ser,"exec-out","screencap","-p").stdout)
        subprocess.run(["sips","-Z","900",p,"--out",p.replace(".png","-s.png")],capture_output=True); print(p.replace(".png","-s.png"))
main()
