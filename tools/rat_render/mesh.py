import json,base64,struct
import numpy as np
CT={5126:('f',4),5123:('H',2),5121:('B',1),5125:('I',4)}; NC={"SCALAR":1,"VEC2":2,"VEC3":3,"VEC4":4}
def load(path):
    g=json.load(open(path)); bufs=[base64.b64decode(b["uri"].split(",",1)[1]) for b in g["buffers"]]
    def acc(i):
        a=g["accessors"][i]; bv=g["bufferViews"][a["bufferView"]]; t,sz=CT[a["componentType"]]; n=NC[a["type"]]
        off=bv.get("byteOffset",0)+a.get("byteOffset",0); stride=bv.get("byteStride",sz*n); b=bufs[bv["buffer"]]
        out=np.array([struct.unpack_from("<"+t*n,b,off+k*stride) for k in range(a["count"])],dtype=float)
        if a.get("normalized") and t=='B': out/=255
        if a.get("normalized") and t=='H': out/=65535
        return out
    p=g["meshes"][0]["primitives"][0]
    J=acc(p["attributes"]["JOINTS_0"]).astype(int); W=acc(p["attributes"]["WEIGHTS_0"]); UV=acc(p["attributes"]["TEXCOORD_0"])
    s=acc(p["indices"]).astype(int).ravel(); tris=[]
    for i in range(len(s)-2):
        a,b,c=s[i],s[i+1],s[i+2]
        if a==b or b==c or a==c: continue
        tris.append((a,b,c) if i%2==0 else (b,a,c))
    names=[g["nodes"][j]["name"] for j in g["skins"][0]["joints"]]
    def wsum(keys):
        sel=np.array([any(k in n for k in keys) for n in names]); return (W*sel[J]).sum(1)
    return g,np.array(tris),UV,wsum
