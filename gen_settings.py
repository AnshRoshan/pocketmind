import os
b=r"e:\\Projects\\pocketmind"
open(os.path.join(b,"settings.gradle.kts"),"w").write(open(os.path.join(b,"settings.gradle.kts"),"r").read() if os.path.exists(os.path.join(b,"settings.gradle.kts")) else "")
print("done")
