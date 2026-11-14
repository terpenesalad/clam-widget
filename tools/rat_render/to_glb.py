"""Pack an embedded .gltf (data: URIs) into a single binary .glb."""
import json, base64, struct, sys
src, dst = sys.argv[1], sys.argv[2]
g = json.load(open(src))
bin_ = bytearray(); base = []
def pad():
    while len(bin_) % 4: bin_.append(0)
for b in g["buffers"]:
    pad(); base.append(len(bin_)); bin_ += base64.b64decode(b["uri"].split(",",1)[1])
for bv in g["bufferViews"]:
    bv["byteOffset"] = bv.get("byteOffset", 0) + base[bv["buffer"]]; bv["buffer"] = 0
for img in g.get("images", []):
    if "uri" not in img: continue
    mime, data = img["uri"][5:].split(";base64,"); raw = base64.b64decode(data)
    pad(); g["bufferViews"].append({"buffer": 0, "byteOffset": len(bin_), "byteLength": len(raw)}); bin_ += raw
    img.pop("uri"); img["bufferView"] = len(g["bufferViews"]) - 1; img["mimeType"] = mime
pad(); g["buffers"] = [{"byteLength": len(bin_)}]
js = json.dumps(g, separators=(",", ":")).encode()
while len(js) % 4: js += b" "
total = 12 + 8 + len(js) + 8 + len(bin_)
with open(dst, "wb") as f:
    f.write(struct.pack("<III", 0x46546C67, 2, total))
    f.write(struct.pack("<II", len(js), 0x4E4F534A)); f.write(js)
    f.write(struct.pack("<II", len(bin_), 0x004E4942)); f.write(bin_)
print("GLB", dst, round(total/1e6, 2), "MB")
