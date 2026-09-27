import sys,os
b=r"e:\\Projects\\pocketmind"
open(os.path.join(b,"core","build.gradle.kts"),"w").write("// hello\n")
sys.stderr.write("done\n")
