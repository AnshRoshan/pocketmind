import os
b=r"e:\\Projects\\pocketmind"
def w(p,c):
 fp=os.path.join(b,p.replace("/",os.sep))
 os.makedirs(os.path.dirname(fp),exist_ok=True)
 open(fp,"w",encoding="utf-8").write(c)
 print("W:",p)

w("core/build.gradle.kts","""plugins {\n    id(\"com.android.library\")\n    id(\"org.jetbrains.kotlin.android\")\n    id(\"com.google.devtools.ksp\")\n    id(\"com.google.dagger.hilt.android\")\n    kotlin(\"plugin.serialization\")\n}\n""")
