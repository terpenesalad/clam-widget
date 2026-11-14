import sys, math, os; sys.path.insert(0,os.path.dirname(__file__))
from drive import *
R, OUT, NAME, TAG, FPS = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], int(sys.argv[5])
ONESHOT = len(sys.argv) > 6 and sys.argv[6] == "oneshot"  # also render the final frame
os.makedirs(OUT,exist_ok=True)
with sync_playwright() as p:
    b,pg=open_page(p,1200,780)
    pg.evaluate(f"setCam({math.radians(225)},{math.radians(12)},0.9,0,0.06,0.07,28)")
    d=pg.evaluate(f'clipDur("{NAME}")'); k=round(d*FPS)
    times=[i*d/k for i in range(k)] + ([d] if ONESHOT else [])
    for i,t in enumerate(times):
        save(pg.evaluate(f'pose("{NAME}",{t})'),f"{OUT}/{TAG}_{i:03d}.png")
    print(NAME,len(times),"frames")
    b.close()
