#!/usr/bin/env python3
"""prefs.py <serial|file> : print the app's play_games_* / challenge_* DataStore entries (debug build, run-as).
Added by qa-engineer 2026-09-20. Minimal preferences_pb reader: bool, int, long, string values.
android-game-dev 2026-09-20: ints and longs are signed (a negative Play services status code used to print as
an unsigned 64-bit number), and a path to a pulled preferences_pb is accepted in place of a serial. The cloud
push diagnostic is play_games_cloud_last_push_stage (a stage name) and play_games_cloud_last_push_status (the
status integer; absent when the push carried none)."""
import sys,subprocess,os
ADB=os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
if os.path.isfile(sys.argv[1]): raw=open(sys.argv[1],"rb").read()
else: raw=subprocess.run([ADB,"-s",sys.argv[1],"exec-out","run-as","com.erman.pegsolitarie","cat","files/datastore/app_prefs.preferences_pb"],capture_output=True).stdout
def signed(v): return v-(1<<64) if v>=1<<63 else v
def varint(b,i):
    v=s=0
    while True:
        c=b[i]; i+=1; v|=(c&127)<<s; s+=7
        if not c&128: return v,i
i=0
while i<len(raw):
    tag,i=varint(raw,i); ln,i=varint(raw,i); entry=raw[i:i+ln]; i+=ln
    j=0; key=None; val=None
    while j<len(entry):
        t,j=varint(entry,j); l,j=varint(entry,j); body=entry[j:j+l]; j+=l
        if t>>3==1: key=body.decode()
        else:
            k=0; vt,k=varint(body,k); f=vt>>3
            if vt&7==0: v,k=varint(body,k); val={1:bool(v)}.get(f,signed(v))
            elif vt&7==2: l2,k=varint(body,k); val=body[k:k+l2].decode("utf8","replace")
            else: val="<fixed>"
    if key and (key.startswith("play_games") or key.startswith("challenge")): print(f"{key} = {val}")
