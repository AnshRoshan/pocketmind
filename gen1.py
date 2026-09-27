import base64,os,sys
B=r"e:\\Projects\\pocketmind"
def w(p,c):
 fp=os.path.join(B,p.replace("/",os.sep))
 os.makedirs(os.path.dirname(fp),exist_ok=True)
 with open(fp,"w",encoding="utf-8") as f: f.write(c)
 print("W:",p)
