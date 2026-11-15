import sys, math, os; sys.path.insert(0,os.path.dirname(__file__))
from drive import *
R, OUT, NAME, TAG, FPS = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], int(sys.argv[5])
ONESHOT = len(sys.argv) > 6 and sys.argv[6] == "oneshot"  # also render the final frame
os.makedirs(OUT,exist_ok=True)

def blink_shape(t, at, close=0.08, hold=0.04, open_=0.11):
    """0..1 eyelid amount for one blink starting at `at` seconds."""
    x = t - at
    if x < 0 or x > close + hold + open_: return 0.0
    if x < close: u = x / close; return u * u * (3 - 2*u)
    if x < close + hold: return 1.0
    u = (x - close - hold) / open_; return 1 - u * u * (3 - 2*u)

def smooth(u): u = min(1, max(0, u)); return u * u * (3 - 2*u)

BLINKS = {  # seconds within each clip
    "FBXExportClip_0": [1.6, 5.2, 5.5, 9.4, 13.1],   # her default idle (incl. a double blink)
    "SHAKE": [0.35],
    "WAKE": [0.85],                                   # a sleepy blink after opening
}
def blink(name, t, d):
    if name == "SLEEP_LOOP": return 1.0
    if name == "SLEEP_ENTER": return smooth((t / d - 0.35) / 0.55)          # eyes slowly close
    if name == "SNEEZE":
        return max(smooth((t - 0.55) / 0.12), 0) * (1 - smooth((t - 1.0) / 0.15))  # squeeze shut for the sneeze
    a = max([blink_shape(t, b) for b in BLINKS.get(name, [])] + [0.0])
    if name == "WAKE": a = max(a, 1 - smooth(t / (d * 0.4)))               # eyes open as she wakes
    return a
with sync_playwright() as p:
    b,pg=open_page(p,1200,780)
    pg.evaluate(f"setCam({math.radians(225)},{math.radians(12)},0.9,0,0.06,0.07,28)")
    d=pg.evaluate(f'clipDur("{NAME}")'); k=round(d*FPS)
    times=[i*d/k for i in range(k)] + ([d] if ONESHOT else [])
    for i,t in enumerate(times):
        save(pg.evaluate(f'pose("{NAME}",{t},{blink(NAME,t,d):.4f})'),f"{OUT}/{TAG}_{i:03d}.png")
    print(NAME,len(times),"frames")
    b.close()
