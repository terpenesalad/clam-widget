"""Recolour the rat's paws (below an ankle height) to match her tail/ears."""
import sys, json, base64, io, struct
import numpy as np
from PIL import Image, ImageFilter
sys.path.insert(0, __import__("os").path.dirname(__file__))
from mesh import load

def zmap(g, tris, UV, P, sel, size):
    """Per-texel height (mesh Z) for the selected triangles; NaN elsewhere."""
    Z = np.full((size, size), np.nan)
    for t in tris:
        if not sel[t].all(): continue
        uv = UV[t] * size - 0.5; z = P[t][:, 2]
        x0, y0 = np.floor(uv.min(0) - 1).astype(int); x1, y1 = np.ceil(uv.max(0) + 1).astype(int)
        xs, ys = np.meshgrid(np.arange(max(x0,0), min(x1,size-1)+1), np.arange(max(y0,0), min(y1,size-1)+1))
        (ax, ay), (bx, by), (cx, cy) = uv
        den = (by-cy)*(ax-cx) + (cx-bx)*(ay-cy)
        if abs(den) < 1e-9: continue
        l1 = ((by-cy)*(xs-cx) + (cx-bx)*(ys-cy)) / den
        l2 = ((cy-ay)*(xs-cx) + (ax-cx)*(ys-cy)) / den
        l3 = 1 - l1 - l2
        inside = (l1 > -0.08) & (l2 > -0.08) & (l3 > -0.08)   # slight overdraw closes UV seams
        zz = l1*z[0] + l2*z[1] + l3*z[2]
        cur = Z[ys[inside], xs[inside]]
        Z[ys[inside], xs[inside]] = np.where(np.isnan(cur), zz[inside], np.minimum(cur, zz[inside]))
    return Z

def run(src, dst, ankle, band=0.25, preview=None):
    g, tris, UV, wsum = load(src)
    bufs = [base64.b64decode(b["uri"].split(",",1)[1]) for b in g["buffers"]]
    p = g["meshes"][0]["primitives"][0]; a = g["accessors"][p["attributes"]["POSITION"]]; bv = g["bufferViews"][a["bufferView"]]
    off = bv.get("byteOffset",0)+a.get("byteOffset",0); st = bv.get("byteStride",12)
    P = np.array([struct.unpack_from("<fff", bufs[0], off+k*st) for k in range(a["count"])])
    img_uri = g["images"][0]["uri"]; tex = Image.open(io.BytesIO(base64.b64decode(img_uri.split(",",1)[1]))).convert("RGB")
    size = tex.size[0]; T = np.asarray(tex, dtype=np.float32)
    paw = (wsum(["_Foot","_Toe0","_Hand","_Finger0","_Calf","_Forearm"]) > 0.3)
    Z = zmap(g, tris, UV, P, paw, size)
    # target colour: the tail strip and the ears
    tail = zmap(g, tris, UV, P, wsum(["Tail"]) > 0.5, size)
    ear = zmap(g, tris, UV, P, wsum(["Bone0"]) > 0.5, size)
    tc = T[~np.isnan(tail)].mean(0); ec = T[~np.isnan(ear)].mean(0) if (~np.isnan(ear)).any() else tc
    target = (tc + ec) / 2
    # mask: 1 below the ankle line, fading over `band`
    m = np.where(np.isnan(Z), 0.0, np.clip((ankle + band/2 - Z) / band, 0, 1))
    m = np.asarray(Image.fromarray((m*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2)), dtype=np.float32)/255
    lum = T.mean(2, keepdims=True); sel = m > 0.5
    ref = lum[sel].mean() if sel.any() else 200
    detail = np.clip(0.82 + 0.35*(lum - ref)/ref, 0.55, 1.15)   # keep a little fur/skin texture
    new = np.clip(target * detail, 0, 255)
    out = T*(1-m[...,None]) + new*m[...,None]
    res = Image.fromarray(out.astype(np.uint8))
    if preview: res.save(preview)
    b = io.BytesIO(); res.save(b, "PNG", optimize=True)
    g["images"][0]["uri"] = "data:image/png;base64," + base64.b64encode(b.getvalue()).decode()
    json.dump(g, open(dst, "w"))
    print("tail", tc.round(), "ears", ec.round(), "target", target.round(), "paw texels", int(sel.sum()))

if __name__ == "__main__":
    run(sys.argv[1], sys.argv[2], float(sys.argv[3]), preview=sys.argv[4] if len(sys.argv) > 4 else None)
