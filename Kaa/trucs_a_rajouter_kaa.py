import subprocess
import json
speed = 10
p = subprocess.Popen(["java", "-jar", "plateaux.jar"], stdin = subprocess.PIPE, stdout=subprocess.PIPE)


o, e = p.communicate(json.dumps({"speed": speed}))
