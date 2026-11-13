import sys, math, os; sys.path.insert(0,os.path.dirname(__file__))
from drive import *
R=sys.argv[1]; OUT=sys.argv[2]; FPS=12
loops=["BREATHE","EAR_TWITCH","EAR_PERK","NOD","SNEEZE","SHAKE","SLEEP_LOOP"]
oneshots=["SLEEP_ENTER","WAKE"]
os.makedirs(OUT,exist_ok=True)
with sync_playwright() as p:
    b,pg=open_page(p,1200,780)
    pg.evaluate(f"setCam({math.radians(225)},{math.radians(12)},0.9,0,0.06,0.07,28)")
    for n in loops+oneshots:
        d=pg.evaluate(f'clipDur("{n}")')
        k=max(1,round(d*FPS)); frames=[i*d/k for i in range(k)]
        if n in oneshots: frames.append(d)
        tag=n.lower()
        for i,t in enumerate(frames):
            save(pg.evaluate(f'pose("{n}",{t})'),f"{OUT}/{tag}_{i:02d}.png")
        print(n, len(frames), "frames")
    b.close()
