import base64,os,sys
B=r"e:\\Projects\\pocketmind"
def chunks(data,size=3500):
 [open(os.path.join(B,"chunks_out.txt"),"a").write(f"python {B}\\chunk.py "{f}" "{base64.b64encode(data[i:i+size]).decode()}"\n") for i in range(0,len(data),size)]
def w(rel,data):
 f=os.path.join(B,rel.replace("/",os.sep))
 os.makedirs(os.path.dirname(f),exist_ok=True)
 open(os.path.join(B,"chunks_out.txt"),"a").write(f"# {rel}\n")
 open(f,"wb").write(b"")
 chunks(data)
