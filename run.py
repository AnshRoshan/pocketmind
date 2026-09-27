import sys
sys.stderr.write("running\n")
import os
b=r"e:\Projects\pocketmind"

def w(p,c):
    fp=os.path.join(b,p.replace("/",os.sep))
    os.makedirs(os.path.dirname(fp),exist_ok=True)
    open(fp,"w",encoding="utf-8").write(c)
    sys.stderr.write("Written: "+p+"\n")
