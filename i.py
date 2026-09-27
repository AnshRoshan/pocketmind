import base64,os,sys
b=r"e:\\Projects\\pocketmind"
def w(p,e):
 fp=os.path.join(b,p.replace("/",os.sep))
 os.makedirs(os.path.dirname(fp),exist_ok=True)
 open(fp,"w",encoding="utf-8").write(base64.b64decode(e).decode())
 sys.stderr.write("Written: "+p+"\n")
#test
