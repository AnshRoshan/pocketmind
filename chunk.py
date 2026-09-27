import sys,base64
p,data=sys.argv[1],sys.argv[2]
open(p,"ab").write(base64.b64decode(data))
print("ok")
