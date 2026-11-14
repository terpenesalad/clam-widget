"""Darken + warm the brown fur (keeping fur detail), protecting tail, ears and white fur."""
import sys, os, json, base64, io, struct
import numpy as np
from PIL import Image, ImageFilter
sys.path.insert(0, os.path.dirname(__file__))
from mesh import load
from recolor import zmap
src, dst, k, sat, detail = sys.argv[1], sys.argv[2], float(sys.argv[3]), float(sys.argv[4]), float(sys.argv[5])
g, tris, UV, wsum = load(src)
bufs = [base64.b64decode(b["uri"].split(",",1)[1]) for b in g["buffers"]]
p = g["meshes"][0]["primitives"][0]; a = g["accessors"][p["attributes"]["POSITION"]]; bv = g["bufferViews"][a["bufferView"]]
off = bv.get("byteOffset",0)+a.get("byteOffset",0); st = bv.get("byteStride",12)
P = np.array([struct.unpack_from("<fff", bufs[bv["buffer"]], off+i*st) for i in range(a["count"])])
uri = g["images"][0]["uri"]; T = np.asarray(Image.open(io.BytesIO(base64.b64decode(uri.split(",",1)[1]))).convert("RGB")).astype(np.float32)
S = T.shape[0]
tail = ~np.isnan(zmap(g, tris, UV, P, wsum(["Tail"]) > 0.3, S))
lum = T.mean(2, keepdims=True); r, gg, b = T[...,0], T[...,1], T[...,2]
warm = np.clip((r - b - 6) / 14, 0, 1); dark = np.clip((150 - lum[...,0]) / 40, 0, 1)
pink = np.clip(((r - gg) - 22) / 12, 0, 1)
m0 = warm * dark
blur = np.asarray(Image.fromarray((m0*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3)), dtype=np.float32)/255
m = np.maximum(m0, np.clip((blur - 0.35) / 0.3, 0, 1)) * (1 - pink) * (~tail)
m = np.asarray(Image.fromarray((m*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8)), dtype=np.float32)[..., None]/255
local = np.asarray(Image.fromarray(T.astype(np.uint8)).filter(ImageFilter.GaussianBlur(4)), dtype=np.float32)
new = local + (T - local) * detail                         # keep fur strand contrast
nl = new.mean(2, keepdims=True); new = (nl + (new - nl) * sat) * k
out = T * (1 - m) + np.clip(new, 0, 255) * m
buf = io.BytesIO(); Image.fromarray(out.astype(np.uint8)).save(buf, "PNG", optimize=True)
g["images"][0]["uri"] = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()
json.dump(g, open(dst, "w")); print("darkened texels:", int((m[...,0] > 0.5).sum()))
