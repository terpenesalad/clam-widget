import sys, base64, json, http.server, threading, functools, os
from playwright.sync_api import sync_playwright
R=sys.argv[1]
H=functools.partial(http.server.SimpleHTTPRequestHandler, directory=R)
class Q(H.func):
    def log_message(self,*a): pass
srv=http.server.ThreadingHTTPServer(("127.0.0.1",8765),functools.partial(Q,directory=R))
threading.Thread(target=srv.serve_forever,daemon=True).start()
def open_page(p,w,h):
    b=p.chromium.launch(args=["--use-angle=swiftshader","--enable-unsafe-swiftshader","--ignore-gpu-blocklist"])
    pg=b.new_page(viewport={"width":w,"height":h})
    pg.on("console",lambda m: print("console:",m.text) if m.type=="error" else None)
    pg.goto(f"http://127.0.0.1:8765/index.html?w={w}&h={h}")
    pg.wait_for_function("window.ready===true",timeout=120000)
    return b,pg
def save(dataurl,path):
    open(path,"wb").write(base64.b64decode(dataurl.split(",",1)[1]))
